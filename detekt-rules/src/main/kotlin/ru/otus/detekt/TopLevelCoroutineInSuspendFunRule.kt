package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (bindingContext == BindingContext.EMPTY) return

        val suspendFunction = expression.getStrictParentOfType<KtNamedFunction>() ?: return
        if (!suspendFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)) return

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val functionFqName = DescriptorUtils.getFqNameSafe(resolvedCall.resultingDescriptor).asString()
        if (functionFqName !in setOf("kotlinx.coroutines.launch", "kotlinx.coroutines.async")) return

        val receiverType = resolvedCall.extensionReceiver?.type ?: return
        if (!receiverType.isCoroutineScope()) return

        val hasImplicitReceiver = resolvedCall.explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        if (hasImplicitReceiver && expression.isInsideScopeBuilder(suspendFunction)) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Use in ${expression.calleeExpression?.text}"
            )
        )
    }

    private fun KotlinType.isCoroutineScope(): Boolean {
        return (TypeUtils.getAllSupertypes(this) + this)
            .any { it.fqNameOrNull()?.asString() == "kotlinx.coroutines.CoroutineScope" }
    }

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    private fun KtCallExpression.isInsideScopeBuilder(suspendFunction: KtNamedFunction): Boolean {
        var current = parent
        while (current != null && current != suspendFunction) {
            if (current is KtLambdaExpression) {
                val builderCall = current.getStrictParentOfType<KtCallExpression>()
                val builderFqName = builderCall
                    ?.getResolvedCall(bindingContext)
                    ?.resultingDescriptor
                    ?.let(DescriptorUtils::getFqNameSafe)
                    ?.asString()

                if (builderFqName in setOf(
                        "kotlinx.coroutines.coroutineScope",
                        "kotlinx.coroutines.supervisorScope"
                    )) return true
            }
            current = current.parent
        }
        return false
    }
}
