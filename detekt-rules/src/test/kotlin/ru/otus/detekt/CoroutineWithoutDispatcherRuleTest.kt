package ru.otus.detekt


import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class CoroutineWithoutDispatcherRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = CoroutineWithoutDispatcherRule(Config.empty)

    @Test
    fun `reports launch without dispatcher`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            fun test(scope: CoroutineScope) {
           
                scope.launch {
                    println("No dispatcher here")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports async without dispatcher`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async

            fun test(scope: CoroutineScope) {
                scope.async {
                    println("No dispatcher here either")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report launch with Dispatchers IO`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            fun test(scope: CoroutineScope) {
                scope.launch(Dispatchers.IO) {
                    println("Dispatcher is present")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings.shouldBeEmpty()
    }

    @Test
    fun `does not report async with Dispatchers Default`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.async

            fun test(scope: CoroutineScope) {
                scope.async(Dispatchers.Default) {
                    println("Safe async")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings.shouldBeEmpty()
    }

    @Test
    fun `does not report launch with custom dispatcher variable`() {
        val code = """
            import kotlinx.coroutines.*

            fun test(scope: CoroutineScope, dispatcher: CoroutineDispatcher) {
                scope.launch(dispatcher) {
                    println("Using custom dispatcher")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings.shouldBeEmpty()
    }

    @Test
    fun `does not report launch with multiple context elements including dispatcher`() {
        val code = """
            import kotlinx.coroutines.*
            
            fun test(scope: CoroutineScope) {
                scope.launch(Dispatchers.IO + CoroutineName("loader")) {
                    println("Dispatcher is included in the context")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings.shouldBeEmpty()
    }

    @Test
    fun `reports launch with non-dispatcher argument`() {
        val code = """
            import kotlinx.coroutines.*

            fun test(scope: CoroutineScope, name: CoroutineName) {
                scope.launch(name) {
                    println("No dispatcher, only name")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report nested launch with dispatcher`() {
        val code = """
            import kotlinx.coroutines.*

            fun test(scope: CoroutineScope) {
                scope.launch(Dispatchers.IO) {
                    launch {
                        println("Nested launch without dispatcher, but inside IO")
                    }
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1 // nested launch should be flagged
    }

    @Test
    fun `reports top-level launch in suspend fun without dispatcher`() {
        val code = """
            import kotlinx.coroutines.*

            suspend fun loadData() {
                launch {
                    println("Launched without dispatcher")
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }
}
