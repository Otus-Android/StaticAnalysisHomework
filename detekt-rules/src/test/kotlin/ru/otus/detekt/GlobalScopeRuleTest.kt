package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class GlobalScopeRuleTest {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports GlobalScope launch`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch

            fun test() {
                GlobalScope.launch { }
            }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports GlobalScope async`() {
        val code = """
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.async

            fun test() {
                GlobalScope.async { }
            }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report regular scope launch`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch

            val scope = CoroutineScope(Dispatchers.Default)
            fun test() {
                scope.launch { }
            }
        """
        val findings = rule.lint(code)
        findings shouldHaveSize 0
    }
}
