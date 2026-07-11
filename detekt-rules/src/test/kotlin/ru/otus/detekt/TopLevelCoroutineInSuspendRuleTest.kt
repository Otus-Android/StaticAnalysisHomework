package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class TopLevelCoroutineInSuspendRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    @Test
    fun `reports call coroutine scoped launch or async in suspend`() {
        val content = """
           import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val  PROPERTY = ""

class SameClass{

    suspend fun executeSuspend(){
        CoroutineScope(SupervisorJob()).launch{

        }
    }
}
        """
        val findings = rule.compileAndLintWithContext(env, content)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports call coroutine scoped launch in suspend with Global Scope`() {
        val content = """
           import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

private const val  PROPERTY = ""

class SameClass {

    suspend fun executeSuspend(){
        GlobalScope.launch{

        }
    }
}
        """
        val findings = rule.compileAndLintWithContext(env, content)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports call coroutine scoped async in 2 suspend func `() {
        val content = """
           import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.GlobalScope

private const val  PROPERTY = ""

class SameClass{

    suspend fun executeSuspend(){
        GlobalScope.async{

        }
    }
suspend fun executeAnotherSuspend(){
         CoroutineScope(SupervisorJob()).async{

        }
    }
}
        """
        val findings = rule.compileAndLintWithContext(env, content)
        findings shouldHaveSize 2
    }

    @Test
    fun `no reports`() {
        val content = """
package ru.otus.example

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class SameClass {

    suspend fun invoke(){
        supervisorScope{(SupervisorJob())
            launch { 
                println("hello")
            }
        }
    }
}

        """
        val findings = rule.compileAndLintWithContext(env, content)
        findings shouldHaveSize 0
    }
}
