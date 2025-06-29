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
    fun `should detect coroutine launch inside suspend function`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.supervisorScope

        suspend fun test() {
            CoroutineScope(Dispatchers.Main).launch { }
            GlobalScope.launch { }
            GlobalScope.async { }
            CoroutineScope.launch { }

            coroutineScope { }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 4
    }

    @Test
    fun `should detect coroutine launch using scope value`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        suspend fun test() {
            println("suspend")
            val g = GlobalScope
            g.launch { }

            var g1 = GlobalScope
            g1.launch { }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `should not detect coroutine launch in ordinary function`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch

        fun test3() {
            GlobalScope.launch { }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
