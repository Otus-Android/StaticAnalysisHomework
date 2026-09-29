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
    fun `reports launch on CoroutineScope inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch

        suspend fun load() {
            CoroutineScope(Dispatchers.Default).launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports async on CoroutineScope inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.async

        suspend fun load() {
            CoroutineScope(Dispatchers.Default).async { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports launch on CoroutineScope subclass inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Job
        import kotlinx.coroutines.launch

        class CustomScope : CoroutineScope {
            override val coroutineContext get() = Job()
        }

        suspend fun load(scope: CustomScope) {
            scope.launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `reports launch on implicit receiver inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Job
        import kotlinx.coroutines.launch

        class Service : CoroutineScope {
            override val coroutineContext get() = Job()

            suspend fun work() {
                launch { }
            }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 1
    }

    @Test
    fun `doesn't report launch inside ordinary fun`() {
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
    fun `doesn't report launch inside nested non suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch

        suspend fun outer() {
            fun inner() {
                CoroutineScope(Dispatchers.Default).launch { }
            }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `doesn't report coroutineScope builder inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch

        suspend fun load() {
            coroutineScope {
                launch { }
            }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `doesn't report supervisorScope builder inside suspend fun`() {
        val code = """
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.supervisorScope

        suspend fun load() {
            supervisorScope {
                launch { }
            }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }

    @Test
    fun `doesn't report launch on non scope receiver`() {
        val code = """
        class Engine {
            fun launch(block: () -> Unit) = block()
        }

        suspend fun start(engine: Engine) {
            engine.launch { }
        }
        """
        rule.compileAndLintWithContext(env, code) shouldHaveSize 0
    }
}
