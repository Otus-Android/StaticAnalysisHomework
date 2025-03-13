package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Warning,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )


    @OptIn(IDEAPluginsCompatibilityAPI::class)
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val containingFunction = expression.containingFunction()
        if (containingFunction?.isSuspend() == true) {
            val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
            val functionName = resolvedCall.resultingDescriptor.name.asString()

            if (functionName in listOf("launch", "async")) {
                val receiverType = resolvedCall.extensionReceiver?.type
                if (receiverType != null && isCoroutineScope(receiverType)) {
                    val parent = expression.parent
                    if (parent is KtDotQualifiedExpression &&
                        parent.receiverExpression.text in listOf(
                            "coroutineScope",
                            "supervisorScope"
                        )
                    ) return


                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            message = "Avoid running top level coroutines inside suspend functions"
                        )
                    )
                }
            }
        }
    }

    private fun isCoroutineScope(type: KotlinType): Boolean =
        type.supertypes()
            .any { it.fqNameOrNull()?.asString() == "kotlinx.coroutines.CoroutineScope" }

    private fun KtCallExpression.containingFunction(): KtFunction? =
        this.parents.filterIsInstance<KtFunction>().firstOrNull()

    private fun KtFunction.isSuspend(): Boolean =
        this.modifierList?.text == "suspend"
}
