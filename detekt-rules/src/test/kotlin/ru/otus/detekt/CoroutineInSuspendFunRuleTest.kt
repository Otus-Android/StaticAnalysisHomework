package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class CoroutineInSuspendFunRuleTest(private val env: KotlinCoreEnvironment) {
        private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

        @Test
        fun `reports launch in suspend fun`() {
            val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).launch {
        
            }
        }
        """
            val findings = rule.compileAndLintWithContext(env, code)
            findings shouldHaveSize 1
        }

        @Test
        fun `reports async in suspend fun`() {
            val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).async {
        
            }
        }
        """
            val findings = rule.compileAndLintWithContext(env, code)
            findings shouldHaveSize 1
        }

    @Test
    fun `reports async in suspend fun with scope child`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        interface CoScope : CoroutineScope {}

        suspend fun loadInfo() {
            CoScope(Dispatchers.Default).async {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch in suspend fun with scope variable`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo(scope : CoroutineScope) {
            scope.launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports async in suspend fun with scope child variable`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        interface CoScope : CoroutineScope {}

        suspend fun loadInfo(scope : CoScope) {
            scope.async {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
        fun `no report launch in suspend fun`() {
            val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            coroutineScope {
                launch {}
            }
        }
        """
            val findings = rule.compileAndLintWithContext(env, code)
            findings shouldHaveSize 0
        }

        @Test
        fun `no report launch in supervisor scope`() {
            val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            supervisorScope {
                launch {}
                async {}
            }
        }
        """
            val findings = rule.compileAndLintWithContext(env, code)
            findings shouldHaveSize 0
        }
}
