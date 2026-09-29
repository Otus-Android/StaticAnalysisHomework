package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (!expression.receiverExpression.isGlobalScope()) return
        if (!expression.selectorExpression.isCoroutineBuilderCall()) return

        val builderName = (expression.selectorExpression as? KtCallExpression)
            ?.calleeExpression?.text ?: return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "$builderName in GlobalScope. " +
                    "Such coroutines are not cancelled automatically and can leak resources."
            )
        )
    }

    private fun KtExpression?.isGlobalScope(): Boolean {
        if (this == null) return false
        if (bindingContext != BindingContext.EMPTY) {
            return bindingContext.getType(this)?.fqNameOrNull() == GLOBAL_SCOPE_FQ_NAME
        }
        return text == GLOBAL_SCOPE
    }

    private fun KtExpression?.isCoroutineBuilderCall(): Boolean {
        val call = this as? KtCallExpression ?: return false
        return call.calleeExpression?.text in COROUTINE_BUILDERS
    }

    private companion object {
        private const val GLOBAL_SCOPE = "GlobalScope"
        private val GLOBAL_SCOPE_FQ_NAME = FqName("kotlinx.coroutines.GlobalScope")
        private val COROUTINE_BUILDERS = setOf("launch", "async")
    }
}
