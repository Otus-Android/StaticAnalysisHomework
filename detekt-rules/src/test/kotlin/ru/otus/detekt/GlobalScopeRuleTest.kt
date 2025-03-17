package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class GlobalScopeRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `should report GlobalScope launch`() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            fun loadInfo() {
                GlobalScope.launch { 
                    
                 }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should report GlobalScope async`() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            fun loadInfo() {
                GlobalScope.async { 
                    
                 }
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `should not report 'val scope = GlobalScope'`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.GlobalScope
            
            fun loadInfo(scope: CoroutineScope) = Unit
            
            fun fetchData() {
                val scope = GlobalScope
                loadInfo(scope)
            }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
