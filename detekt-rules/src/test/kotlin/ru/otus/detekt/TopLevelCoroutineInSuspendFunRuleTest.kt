package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.lintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class TopLevelCoroutineInSuspendFunRuleTest(
    private val env: KotlinCoreEnvironment
) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `reports launch and async on CoroutineScope in suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            suspend fun load(scope: CoroutineScope) {
                scope.launch { }
                scope.async { 42 }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 2
    }

    @Test
    fun `reports coroutine started on CoroutineScope subtype`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            class ScreenScope : CoroutineScope

            suspend fun load(scope: ScreenScope) {
                scope.launch { }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 1
    }

    @Test
    fun `does not report coroutine builders outside suspend function`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            fun load(scope: CoroutineScope) {
                scope.launch { }
                scope.async { 42 }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 0
    }

    @Test
    fun `does not report structured coroutines in coroutineScope`() {
        val code = """
            import kotlinx.coroutines.async
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch

            suspend fun load() = coroutineScope {
                launch { }
                async { 42 }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 0
    }

    @Test
    fun `does not report structured coroutines in supervisorScope`() {
        val code = """
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch
            import kotlinx.coroutines.supervisorScope

            suspend fun load() = supervisorScope {
                launch { }
                async { 42 }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 0
    }

    @Test
    fun `reports explicit external scope inside scope builder`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.coroutineScope
            import kotlinx.coroutines.launch

            suspend fun load(externalScope: CoroutineScope) = coroutineScope {
                externalScope.launch { }
            }
        """.trimIndent()

        rule.lintWithContext(env, code, COROUTINES_STUBS) shouldHaveSize 1
    }

    private companion object {
        val COROUTINES_STUBS = """
            package kotlinx.coroutines

            interface CoroutineScope
            class Job
            class Deferred<T>

            fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job = Job()
            fun <T> CoroutineScope.async(block: suspend CoroutineScope.() -> T): Deferred<T> = Deferred()

            suspend fun <T> coroutineScope(block: suspend CoroutineScope.() -> T): T = TODO()
            suspend fun <T> supervisorScope(block: suspend CoroutineScope.() -> T): T = TODO()
        """.trimIndent()
    }
}
