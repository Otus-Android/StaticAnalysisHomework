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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val qualifiedExpression = expression.parent as? KtDotQualifiedExpression

        if (qualifiedExpression != null) {
            val receiver = qualifiedExpression.receiverExpression as? KtNameReferenceExpression
            val methodName = qualifiedExpression.selectorExpression?.referenceExpression()?.text

            if (receiver?.text == "GlobalScope" && methodName in listOf("launch", "async")) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        message = "Avoid using GlobalScope"
                    )
                )
            }
        }
    }

}
