package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class GlobalScopeRuleTest() {
    val rule = GlobalScopeRule(config = Config.empty)

    @Test
    fun `should report when use async with Global scope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope

        fun globalScopeViolation() {
            GlobalScope.async {

            }
        }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report when use launch with Global scope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope

        fun globalScopeViolation() {
            GlobalScope.launch {

            }
        }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `shouldn't report when pass Global scope in function`() {
        val code = """
        import kotlinx.coroutines.GlobalScope

        fun check() {
            test(GlobalScope)
        }
        
        fun test(coroutineScope: CoroutineScope) {
            
        }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 0
    }

    @Test
    fun `shouldn't report when assign Global scope to val`() {
        val code = """
        import kotlinx.coroutines.GlobalScope

        fun check() {
            val scope = GlobalScope
        }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 0
    }
}
