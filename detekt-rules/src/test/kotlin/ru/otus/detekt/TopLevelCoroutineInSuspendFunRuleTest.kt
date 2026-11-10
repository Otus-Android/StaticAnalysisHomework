package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class TopLevelCoroutineInSuspendFunRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `reports launch and async on CoroutineScope subtype in suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            class AppScope : CoroutineScope by CoroutineScope(kotlin.coroutines.EmptyCoroutineContext)

            suspend fun load(scope: AppScope) {
                scope.launch { }
                scope.async { 1 }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 2
    }

    @Test
    fun `reports launch on CoroutineScope in suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            suspend fun load(scope: CoroutineScope) {
                scope.launch { }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `does not report launch outside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            fun load(scope: CoroutineScope) {
                scope.launch { }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `does not report launch in a non-suspend local function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            suspend fun load(scope: CoroutineScope) {
                fun inner() {
                    scope.launch { }
                }
                inner()
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `does not report launch scoped by coroutine builders`() {
        val code = """
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch
            import kotlinx.coroutines.supervisorScope

            suspend fun load() {
                coroutineScope { launch { } }
                supervisorScope { launch { } }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `reports an outer scope launch inside a lambda`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            suspend fun load(scope: CoroutineScope, ids: List<Int>) {
                ids.forEach { scope.launch { } }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `does not report a custom launch function`() {
        val code = """
            class Foo {
                fun launch(block: () -> Unit) = block()
            }

            suspend fun load(foo: Foo) {
                foo.launch { }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }
}
