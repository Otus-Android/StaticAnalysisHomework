package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (expression.receiverExpression.text.contains("GlobalScope") &&
                expression.getCalleeExpressionIfAny()?.text in listOf("async", "launch")
        ) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Don't use GlobalScope for coroutines"
                )
            )
        }
        super.visitDotQualifiedExpression(expression)
    }

}
