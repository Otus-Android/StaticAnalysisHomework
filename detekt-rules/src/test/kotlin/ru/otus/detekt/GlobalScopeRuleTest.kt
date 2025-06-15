package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GlobalScopeRuleTest {

    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports GlobalScope launch`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch

            fun start() {
                GlobalScope.launch {
                    println("Running")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        findings shouldHaveSize 1
        findings[0].message shouldBe "Do not use GlobalScope. Use a structured coroutine scope instead (e.g., viewModelScope, lifecycleScope, etc.)."
    }

    @Test
    fun `reports GlobalScope async`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async

            fun start() {
                GlobalScope.async {
                    println("Running")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report launch in custom scope`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            fun start(scope: CoroutineScope) {
                scope.launch {
                    println("Safe")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report async in custom scope`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async

            fun start(scope: CoroutineScope) {
                scope.async {
                    println("Safe")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        findings shouldHaveSize 0
    }
}
