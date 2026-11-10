package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.isGlobalScopeCoroutineLaunch()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Avoid launching coroutines in GlobalScope",
            ),
        )
    }

    private fun KtCallExpression.isGlobalScopeCoroutineLaunch(): Boolean =
        if (bindingContext == BindingContext.EMPTY) {
            isGlobalScopeLaunchOrAsyncBySyntax()
        } else {
            isCoroutineLaunchOrAsync(bindingContext) &&
                coroutineReceiverType(bindingContext)?.isGlobalScope() == true
        }
}
