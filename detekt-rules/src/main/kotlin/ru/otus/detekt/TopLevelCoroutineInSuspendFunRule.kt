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

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (bindingContext == BindingContext.EMPTY) return
        if (!expression.isCoroutineLaunchOrAsync(bindingContext)) return
        if (!expression.isTopLevelLaunchInSuspendFunction(bindingContext)) return

        val receiverType = expression.coroutineReceiverType(bindingContext) ?: return
        if (!receiverType.isCoroutineScopeOrSubtype()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Avoid launching top level coroutines inside suspend functions",
            ),
        )
    }
}
