package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class TopLevelCoroutineRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = TopLevelCoroutineInSuspendFunRule(Config.empty)

    private fun assertFindings(
        code: String,
        expectedSize: Int
    ) {
        val findings = rule.compileAndLintWithContext(env, code.trimIndent())
        findings shouldHaveSize expectedSize
    }

    @Test
    fun `reports launch in suspend fun`() = assertFindings(
        """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).launch { }
        }
        """,
        1
    )

    @Test
    fun `reports launch and async in suspend fun with apply`() = assertFindings(
        """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).apply {
                launch { }
                async { }
            }
        }
        """,
        2
    )

    @Test
    fun `reports launch in suspend fun with val scope`() = assertFindings(
        """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        suspend fun loadInfo() {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch { }
        }
        """,
        1
    )

    @Test
    fun `reports launch in suspend fun with viewModelScope`() = assertFindings(
        """
        import androidx.lifecycle.viewModelScope
        import kotlinx.coroutines.launch
        suspend fun loadInfo() {
            viewModelScope.launch { }
        }
        """,
        1
    )

    @Test
    fun `reports launch in suspend fun with scope from parameter`() = assertFindings(
        """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        suspend fun loadInfo(scope: CoroutineScope) {
            scope.launch { }
        }
        """,
        1
    )

    @Test
    fun `reports async in suspend fun`() = assertFindings(
        """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        suspend fun loadInfo() {
            CoroutineScope(Dispatchers.Default).async { }
        }
        """,
        1
    )

    @Test
    fun `no report launch in coroutineScope`() = assertFindings(
        """
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
        """,
        0
    )

    @Test
    fun `no report launch in supervisor scope`() = assertFindings(
        """
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
        """,
        0
    )

    @Test
    fun `no reports launch in not suspend fun`() = assertFindings(
        """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        fun loadInfo() {
            CoroutineScope(Dispatchers.Default).launch { }
        }
        """,
        0
    )

    @Test
    fun `no reports async in not suspend fun`() = assertFindings(
        """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        fun loadInfo() {
            CoroutineScope(Dispatchers.Default).async { }
        }
        """,
        0
    )

}
