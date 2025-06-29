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

    @Test
    fun `should report GlobalScope launch`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        fun test() {
            GlobalScope.launch { }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report GlobalScope async`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        fun test() {
            GlobalScope.async { }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should not report GlobalScope toString`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        fun test() {
            GlobalScope.toString()
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `should not report GlobalScope string`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        fun test() {
            printLn("GlobalScope.launch")
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
