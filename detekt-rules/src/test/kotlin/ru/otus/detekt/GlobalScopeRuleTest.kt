package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class GlobalScopeRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = GlobalScopeRule(Config.empty)

    private fun assertFindings(code: String, expectedSize: Int) {
        val findings = rule.compileAndLintWithContext(env, code.trimIndent())
        findings shouldHaveSize expectedSize
    }

    @Test
    fun `should report GlobalScope launch`() = assertFindings("""
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch
        fun loadInfo() {
            GlobalScope.launch { }
        }
    """, 1)

    @Test
    fun `should report GlobalScope async`() = assertFindings("""
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        fun loadInfo() {
            GlobalScope.async { }
        }
    """, 1)

    @Test
    fun `should not report 'val scope = GlobalScope'`() = assertFindings("""
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.GlobalScope
        fun loadInfo(scope: CoroutineScope) = Unit
        fun fetchData() {
            val scope = GlobalScope
            loadInfo(scope)
        }
    """, 0)

    @Test
    fun `should not report CoroutineScope launch`() = assertFindings("""
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
        fun example(scope: CoroutineScope) {
            scope.launch { println("This is a coroutine") }
        }
    """, 0)

    @Test
    fun `should not report non-coroutine code`() = assertFindings("""
        fun example() {
            println("This is not a coroutine")
        }
    """, 0)
}
