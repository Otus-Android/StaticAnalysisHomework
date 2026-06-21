package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
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
        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val descriptor = resolvedCall.resultingDescriptor
        val name = descriptor.name.asString()
        if (name != LAUNCH && name != ASYNC) return
        val receiverType = resolvedCall.dispatchReceiver?.type ?: resolvedCall.extensionReceiver?.type ?: return
        if (!isCoroutineScope(receiverType)) return
        if (!isInsideSuspendFunction(expression)) return
        if (isInsideCoroutineScopeBuilder(expression)) {
            val parent = expression.parent
            if (parent is KtDotQualifiedExpression) {
                val receiver = parent.receiverExpression
                if (receiver.text != THIS_MODIFIER) {
                    report(CodeSmell(issue, Entity.from(expression), issue.description))
                }
            }
            return
        }
        report(CodeSmell(issue, Entity.from(expression), issue.description))
    }

    private fun isCoroutineScope(type: KotlinType): Boolean {
        if (type.constructor.declarationDescriptor?.fqNameSafe?.asString() == COROUTINE_SCOPE_FQ_NAME) return true
        return type.supertypes().any { it.constructor.declarationDescriptor?.fqNameSafe?.asString() == COROUTINE_SCOPE_FQ_NAME }
    }

    private fun isInsideSuspendFunction(element: KtElement): Boolean {
        var current: KtElement? = element
        while (current != null) {
            if (current is KtNamedFunction) {
                if (current.hasModifier(KtTokens.SUSPEND_KEYWORD)) return true
            }
            current = current.parent as? KtElement
        }
        return false
    }

    private fun isInsideCoroutineScopeBuilder(element: KtElement): Boolean {
        var current: KtElement? = element
        while (current != null) {
            if (current is KtLambdaArgument) {
                val call = current.parent as? KtCallExpression
                if (call != null) {
                    val resolved = call.getResolvedCall(bindingContext)
                    val fqName = resolved?.resultingDescriptor?.fqNameSafe?.asString()
                    if (fqName == COROUTINE_SCOPE_BUILDER || fqName == SUPERVISOR_SCOPE_BUILDER) {
                        return true
                    }
                }
            }
            current = current.parent as? KtElement
        }
        return false
    }

    companion object {
        private const val LAUNCH = "launch"
        private const val ASYNC = "async"
        private const val THIS_MODIFIER = "this"
        private const val COROUTINE_SCOPE_FQ_NAME = "kotlinx.coroutines.CoroutineScope"
        private const val COROUTINE_SCOPE_BUILDER = "kotlinx.coroutines.coroutineScope"
        private const val SUPERVISOR_SCOPE_BUILDER = "kotlinx.coroutines.supervisorScope"
    }
}
