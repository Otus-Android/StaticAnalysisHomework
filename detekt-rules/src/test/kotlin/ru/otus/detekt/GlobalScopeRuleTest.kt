package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class GlobalScopeRuleTest {
    private val rule = GlobalScopeRule(Config.empty)

    @Test
    fun `reports call launch or async in GlobalScope`() {
        val code = """
            import androidx.compose.runtime.Composable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SameEntity {

    suspend fun globalScope(){
        GlobalScope.async {
         GlobalScope.launch{
             
         }
        }
    }
}

        """
        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 2
    }

    @Test
    fun `reports call launch in GlobalScope`() {
        val code = """
            import androidx.compose.runtime.Composable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SameClass {

    suspend fun globalScope(){
         kotlinx.coroutines.GlobalScope.launch { }
    }
}

        """
        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports call async in GlobalScope`() {
        val code = """
            import androidx.compose.runtime.Composable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val  PROPERTY = ""

class SameClass{

    suspend fun globalScope(){
         GlobalScope.async{
             
         }
    }
}

        """
        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `no reports`() {
        val code = """
            import androidx.compose.runtime.Composable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val  PROPERTY = ""

class SameClass {

    suspend fun noGlobalScope(){
         GlobalScope.customMethod(launch = true)
    }
}

        """
        val findings = rule.compileAndLint(code)
        findings shouldHaveSize 0
    }

}

