package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes

private const val SUPER_TYPE_NAME = "kotlinx.coroutines.CoroutineScope"

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val isSuspend = function.modifierList?.hasSuspendModifier() ?: false

        if (bindingContext == BindingContext.EMPTY || !isSuspend) return

        val dotCallExpression = function.collectDescendantsOfType<KtDotQualifiedExpression>()

        val dotCallExpressions = dotCallExpression.filter {

            it.receiverExpression.isSubclassOfCoroutineScope()
        }

        val sendReport = dotCallExpressions.any { it.isExecuteAsyncOrLaunch() }

        if (sendReport) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(function),
                    message = "Avoid running top level coroutines inside suspend functions"
                )
            )
        }
    }

    private fun KtQualifiedExpression.isExecuteAsyncOrLaunch(): Boolean {

        val callExpression = this.selectorExpression as? KtCallExpression ?: return false

        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false

        val fullFunctionName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString()

        return when (fullFunctionName) {
            "kotlinx.coroutines.launch", "kotlinx.coroutines.async" -> true
            else -> false
        }
    }

    private fun KtExpression.isSubclassOfCoroutineScope(): Boolean {

        val type = bindingContext.getType(this) ?: return false

        val cleanType = type.makeNotNullable()

        val className = cleanType.fqNameOrNull()?.asString()

        if (className == SUPER_TYPE_NAME) return true

        return cleanType.supertypes().any { superType ->
            val superTypeName = superType.fqNameOrNull()?.asString()
            superTypeName == SUPER_TYPE_NAME
        }
    }

}
