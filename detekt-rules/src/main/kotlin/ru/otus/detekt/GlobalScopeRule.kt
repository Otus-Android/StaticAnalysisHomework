package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
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

        val callExpression = expression.selectorExpression as? KtCallExpression ?: return

        val functionName = callExpression.calleeExpression?.text

        val isLaunchOrAsync = functionName == "launch" || functionName == "async"

        if (!isLaunchOrAsync) return

        val receiverText = expression.receiverExpression.text

        val isGlobalScope = receiverText == "GlobalScope" || receiverText.endsWith(".GlobalScope")

        if (isGlobalScope) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Avoid using GlobalScope"
                )
            )
        }
    }
}
