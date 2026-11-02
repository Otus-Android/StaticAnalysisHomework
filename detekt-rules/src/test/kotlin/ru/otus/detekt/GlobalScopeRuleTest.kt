package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class GlobalScopeRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports GlobalScope launch`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch
 
        fun load() {
            GlobalScope.launch {
                println("Hello!")
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports GlobalScope async`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch
 
        fun load() {
            GlobalScope.async {
                println("Hello!")
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports GlobalScope launch with imports check`() {
        val code = """
        import kotlinx.coroutines.*
 
        fun load() {
            GlobalScope.launch {
                println("Hello!")
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports GlobalScope async with imports check`() {
        val code = """
        import kotlinx.coroutines.*
 
        fun load() {
            GlobalScope.async {
                println("Hello!")
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }


    @Test
    fun `doesn't report GlobalScope without imports`() {
        val code = """
        object GlobalScope {
            fun launch(block: () -> Unit) = block()
        }
 
        fun load() {
            GlobalScope.launch {
                println("Hello!")
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 0
    }

    @Test
    fun `doesn't report custom scope`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch
 
        class Repository(private val scope: CoroutineScope) {
            fun load() {
                scope.launch { println("Hello!") }
            }
        }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 0
    }
}
