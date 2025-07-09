package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.containingFunction()?.modifierList?.hasSuspendModifier() != true) return

        val coroutineBuilders = setOf("launch", "async")
        val callName = expression.getCallNameExpression()?.text
        if (callName !in coroutineBuilders) return

        var parent = expression.parent
        while (parent != null) {
            when (parent) {
                is KtDotQualifiedExpression -> {
                    val receiver = parent.receiverExpression
                    val receiverType = receiver.getType(bindingContext)?.fqNameOrNull()
                    if (
                        receiverType == FqName("kotlinx.coroutines.CoroutineScope") ||
                        receiver.text == "viewModelScope"
                    ) {
                        if (isInsideSuspendFunction(parent)) {
                            reportCodeSmell(expression)
                            return
                        }
                    }
                }
                is KtCallExpression -> {
                    if (
                        parent.getType(bindingContext)
                            ?.supertypes()
                            ?.any { it.fqNameOrNull() == FqName("kotlinx.coroutines.CoroutineScope") } == true
                    ) {
                        if (isInsideSuspendFunction(parent)) {
                            reportCodeSmell(expression)
                            return
                        }
                    }
                }
            }
            parent = parent.parent
        }
    }

    private fun KtCallExpression.containingFunction(): KtFunction? =
        this.parents.filterIsInstance<KtFunction>().firstOrNull()

    private fun isInsideSuspendFunction(element: PsiElement): Boolean {
        return element.parents.filterIsInstance<KtNamedFunction>().firstOrNull()?.modifierList?.hasSuspendModifier() == true
    }

    private fun reportCodeSmell(expression: KtCallExpression) {
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Avoid running top level coroutines inside suspend functions"
            )
        )
    }

}
