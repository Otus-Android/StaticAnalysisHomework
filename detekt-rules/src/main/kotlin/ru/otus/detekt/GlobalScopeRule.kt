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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val functionName = expression.calleeExpression?.text ?: return
        if (functionName !in setOf("launch", "async")) return

        val qualifiedExpression =
            expression.getQualifiedExpressionForSelector() as? KtDotQualifiedExpression
                ?: return

        val receiver = qualifiedExpression.receiverExpression

        if (!receiver.isGlobalScope()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(qualifiedExpression),
                message = "`GlobalScope.$functionName` should be avoided"
            )
        )
    }

    private fun KtExpression.isGlobalScope(): Boolean {
        val text = this.text
        if (text == "kotlinx.coroutines.GlobalScope") return true
        if (this !is KtNameReferenceExpression || text != "GlobalScope") return false

        return containingKtFile.importDirectives.any { import ->
            val importedName = import.importedFqName?.asString()

            importedName == "kotlinx.coroutines.GlobalScope" ||
                    (import.isAllUnder && importedName == "kotlinx.coroutines")
        }
    }
}
