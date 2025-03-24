package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class TopLevelCoroutineInSuspendFunRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `should report launch on CoroutineScope inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch
            
            suspend fun example(scope: CoroutineScope) {
                scope.launch { 
                    println("This is a coroutine")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report async on CoroutineScope inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            
            suspend fun example(scope: CoroutineScope) {
                scope.async { 
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
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            fun example() {
                CoroutineScope(Dispatchers.Default).launch {
                    println("This is a coroutine")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `should not report coroutineScope builder inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch
            
            suspend fun example() {
                coroutineScope { 
                    launch { 
                        println("This is a coroutine")
                    }
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `should not report supervisorScope builder inside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            
            suspend fun example() {
                supervisorScope { 
                    async { 
                        println("This is an async task")
                    }
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `should not report launch outside suspend function`() {
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

}
