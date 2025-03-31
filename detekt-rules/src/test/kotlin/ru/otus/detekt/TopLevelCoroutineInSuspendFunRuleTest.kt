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
    fun `reports launch in suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch and async in suspend fun with apply`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).apply {
                launch {
            
                }
                async {
                    
                }
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `reports launch in suspend fun with val scope`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch in suspend fun with viewModelScope`() {
        val code = """
        import androidx.lifecycle.viewModelScope
        import kotlinx.coroutines.launch

        suspend fun loadInfo() {
            viewModelScope.launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports launch in suspend fun with scope from parameter`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo(scope: CoroutineScope) {
            scope.launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports async in suspend fun`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).async {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `no report launch in coroutineScope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.coroutineScope

        suspend fun loadInfo() {
            coroutineScope {
                launch {}
                async {}
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }


    @Test
    fun `no report launch in supervisor scope`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.supervisorScope

        suspend fun loadInfo() {
            supervisorScope {
                launch {}
                async {}
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `no reports launch in not suspend fun`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        fun loadInfo() {
            CoroutineScope(Dispatchers.Default).launch {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `no reports async in not suspend fun`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers

        fun loadInfo() {
            CoroutineScope(Dispatchers.Default).async {
        
            }
        }
        """
        val findings = rule.compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
