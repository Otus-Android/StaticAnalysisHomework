package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class GlobalScopeRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports launch and async in GlobalScope`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            fun runTasks() {
                GlobalScope.launch { }
                GlobalScope.async { 1 }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 2
    }

    @Test
    fun `does not report another CoroutineScope`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            fun runTasks(scope: CoroutineScope) {
                scope.launch { }
            }
        """

        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `reports launch and async without type resolution`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            fun runTasks() {
                GlobalScope.launch { }
                GlobalScope.async { 1 }
            }
        """

        rule.compileAndLint(code) shouldHaveSize 2
    }

    @Test
    fun `does not report a local object named GlobalScope`() {
        val code = """
            object GlobalScope {
                fun launch(block: () -> Unit) = block()
            }

            fun runTasks() {
                GlobalScope.launch { }
            }
        """

        rule.compileAndLint(code) shouldHaveSize 0
    }
}
