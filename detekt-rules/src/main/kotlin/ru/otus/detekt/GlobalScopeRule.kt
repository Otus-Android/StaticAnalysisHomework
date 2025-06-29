package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val isGlobalScopeUsing = expression.firstChild.text == "GlobalScope"
        if (isGlobalScopeUsing) {
            val callingFun = expression.lastChild.firstChild
            if (callingFun != null) {
                val funText = callingFun.text
                if (funText == "launch" || funText == "async") {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(expression),
                            message = "Avoid using GlobalScope " +
                                    "More: https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc"
                        )
                    )
                }
            }
        }
    }
}
