package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class TopLevelCoroutineInSuspendFunRuleTest(private val env: KotlinCoreEnvironment) {

    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `should report when start new coroutine in suspend function not in coroutine scope`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            suspend fun <T> coroutineLaunchInSuspendViolation() {
                CoroutineScope(Dispatchers.Main).launch {  }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report when coroutine scope is presence but coroutine isn't build in`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.supervisorScope

            suspend fun coroutineLaunchInSuspendViolation() {
                CoroutineScope(Dispatchers.Main).launch {
        
                }
                coroutineScope { }   
                supervisorScope { }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report when start new coroutine for collecting flow in suspend function not in coroutine scope`() {
        val code = """
            import kotlinx.coroutines.flow.flow
            import kotlinx.coroutines.launch            

            suspend fun <T> coroutineLaunchInSuspendViolation() {
                flow<T> {  }.launchIn(CoroutineScope(Dispatchers.Main))
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `shouldn't report when start new coroutine in not suspend fun`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch            

            fun coroutineLaunchInSuspendViolation() {
                CoroutineScope(Dispatchers.Main).launch {
        
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `shouldn't report when start new coroutine in coroutine scope`() {
        val code = """
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            suspend fun coroutineLaunchInSuspendViolation() {
                coroutineScope {
                    CoroutineScope(Dispatchers.Main).launch {
        
                    }
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `shouldn't report when start new coroutine in supervisor scope`() {
        val code = """
            import kotlinx.coroutines.supervisorScope
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch            

            suspend fun coroutineLaunchInSuspendViolation() {
                supervisorScope {
                    CoroutineScope(Dispatchers.Main).launch {
        
                    }
                }
            }
        """.trimIndent()

        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
