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
            import kotlinx.coroutines.launch
            
            fun example() {
                GlobalScope.launch { 
                    println("This is a coroutine")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report GlobalScope async`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async
            
            fun example() {
                GlobalScope.async { 
                    println("This is an async task")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should not report CoroutineScope launch`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch
            
            fun example(scope: CoroutineScope) {
                scope.launch { 
                    println("This is a coroutine")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `should not report non-coroutine code`() {
        val code = """
            fun example() {
                println("This is not a coroutine")
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

}
