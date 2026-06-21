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
    fun `reports GlobalScope launch in suspend fun`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch

            suspend fun test() {
                GlobalScope.launch { }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports custom scope launch in suspend fun`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)

            suspend fun test() {
                scope.launch { }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports explicit scope launch even inside builder`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch

            suspend fun test() {
                coroutineScope {
                    GlobalScope.launch { }
                }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report launch in coroutineScope builder`() {
        val code = """
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch

            suspend fun test() {
                coroutineScope {
                    launch { }
                }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report launch in supervisorScope builder`() {
        val code = """
            import kotlinx.coroutines.supervisorScope
            import kotlinx.coroutines.launch

            suspend fun test() {
                supervisorScope {
                    launch { }
                }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
