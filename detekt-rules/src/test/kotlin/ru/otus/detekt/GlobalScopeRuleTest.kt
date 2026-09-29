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
    fun `reports launch in GlobalScope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch

        fun load() {
            GlobalScope.launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports async in GlobalScope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async

        fun load() {
            GlobalScope.async { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports launch in GlobalScope with arguments`() {
        val code = """
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch

        fun load() {
            GlobalScope.launch(Dispatchers.Default) { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports aliased GlobalScope with type resolution`() {
        val code = """
        import kotlinx.coroutines.GlobalScope as AppScope
        import kotlinx.coroutines.launch

        fun load() {
            AppScope.launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `doesn't report launch on a local scope`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch

        fun load() {
            CoroutineScope(Dispatchers.Default).launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `doesn't report other calls on GlobalScope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope

        fun isActive(): Boolean = GlobalScope.isActive
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }
}
