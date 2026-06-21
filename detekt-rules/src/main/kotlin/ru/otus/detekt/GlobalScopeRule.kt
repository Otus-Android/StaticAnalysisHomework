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
        val receiverText = expression.receiverExpression.text
        val selectorText = expression.selectorExpression?.text
        if (receiverText == GLOBAL_SCOPE && (selectorText?.startsWith(LAUNCH) == true || selectorText?.startsWith(ASYNC) == true)) {
            report(CodeSmell(issue, Entity.from(expression), issue.description))
        }
    }

    companion object {
        private const val GLOBAL_SCOPE = "GlobalScope"
        private const val LAUNCH = "launch"
        private const val ASYNC = "async"
    }
}
