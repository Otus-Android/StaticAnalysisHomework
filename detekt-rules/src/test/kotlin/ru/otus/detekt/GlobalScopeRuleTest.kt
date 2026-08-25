package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class GlobalScopeRuleTest {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports launch in GlobalScope`() {
        val code = """
            fun startWork() {
                GlobalScope.launch { doWork() }
            }
        """.trimIndent()

        rule.lint(code) shouldHaveSize 1
    }

    @Test
    fun `reports async in GlobalScope`() {
        val code = """
            fun loadData() {
                GlobalScope.async { getData() }
            }
        """.trimIndent()

        rule.lint(code) shouldHaveSize 1
    }

    @Test
    fun `reports every GlobalScope coroutine builder call`() {
        val code = """
            fun runTasks() {
                GlobalScope.launch { firstTask() }
                GlobalScope.async { secondTask() }
            }
        """.trimIndent()

        rule.lint(code) shouldHaveSize 2
    }

    @Test
    fun `does not report coroutine builders in another scope`() {
        val code = """
            fun startWork(scope: CoroutineScope) {
                scope.launch { doWork() }
                scope.async { getData() }
            }
        """.trimIndent()

        rule.lint(code) shouldHaveSize 0
    }
}
