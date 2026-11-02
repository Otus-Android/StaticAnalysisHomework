package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class TopLevelCoroutineInSuspendFunRuleTest(
    private val env: KotlinCoreEnvironment
) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `reports launch on CoroutineScope parameter in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load(scope: CoroutineScope) {
            scope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports async on CoroutineScope parameter in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.async
 
        suspend fun load(scope: CoroutineScope) {
            scope.async { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch on newly created CoroutineScope in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch
 
        suspend fun load() {
            CoroutineScope(Dispatchers.Default).launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch on GlobalScope in suspend fun`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch
 
        suspend fun load() {
            GlobalScope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch on custom CoroutineScope inheritor in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
        import kotlin.coroutines.CoroutineContext
 
        class MyScope(override val coroutineContext: CoroutineContext) : CoroutineScope
 
        suspend fun load(scope: MyScope) {
            scope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports implicit launch in suspend extension fun on CoroutineScope`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun CoroutineScope.load() {
            launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports implicit async in suspend member fun of CoroutineScope inheritor`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.async
        import kotlin.coroutines.CoroutineContext
 
        class Worker(override val coroutineContext: CoroutineContext) : CoroutineScope {
            suspend fun load() {
                async { println("Hello!") }
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch inside regular lambda in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load(scope: CoroutineScope, items: List<Int>) {
            items.forEach { scope.launch { println(it) } }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports each launch and async in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch
 
        suspend fun load(scope: CoroutineScope) {
            scope.launch { println("Hello!") }
            scope.async { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `reports launch on external scope inside coroutineScope builder`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load(scope: CoroutineScope) = coroutineScope {
            scope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `doesn't report launch and async inside coroutineScope`() {
        val code = """
        import kotlinx.coroutines.async
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load() = coroutineScope {
            launch { println("Hello!") }
            async { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report launch and async inside supervisorScope`() {
        val code = """
        import kotlinx.coroutines.async
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.supervisorScope
 
        suspend fun load() = supervisorScope {
            launch { println("Hello!") }
            async { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report nested launch inside coroutineScope`() {
        val code = """
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load() {
            coroutineScope {
                launch {
                    launch { println("Hello!") }
                }
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report this launch inside coroutineScope`() {
        val code = """
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun load() = coroutineScope {
            this.launch { println("Hello!") }
            this@coroutineScope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report launch inside coroutineScope in extension suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.coroutineScope
        import kotlinx.coroutines.launch
 
        suspend fun CoroutineScope.load() = coroutineScope {
            launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report launch in regular fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        fun load(scope: CoroutineScope) {
            scope.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report implicit launch in regular fun of CoroutineScope extension`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        fun CoroutineScope.load() {
            launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report launch on non CoroutineScope receiver in suspend fun`() {
        val code = """
        class Player {
            fun launch(block: () -> Unit) = block()
        }
 
        suspend fun load(player: Player) {
            player.launch { println("Hello!") }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report suspend fun without launch and async`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.delay
 
        suspend fun load(scope: CoroutineScope) {
            delay(100)
            println(scope.hashCode())
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
