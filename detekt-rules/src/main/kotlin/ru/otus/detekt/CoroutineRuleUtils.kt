package ru.otus.detekt

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal const val COROUTINE_SCOPE_FQ_NAME = "kotlinx.coroutines.CoroutineScope"
internal const val GLOBAL_SCOPE_FQ_NAME = "kotlinx.coroutines.GlobalScope"
internal const val COROUTINES_PACKAGE = "kotlinx.coroutines"

private val COROUTINE_LAUNCH_FQ_NAME = "$COROUTINES_PACKAGE.launch"
private val COROUTINE_ASYNC_FQ_NAME = "$COROUTINES_PACKAGE.async"

internal fun KtCallExpression.isCoroutineLaunchOrAsync(bindingContext: BindingContext): Boolean {
    val resolvedCall = getResolvedCall(bindingContext) ?: return false
    val fqName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString()
    return fqName == COROUTINE_LAUNCH_FQ_NAME || fqName == COROUTINE_ASYNC_FQ_NAME
}

internal fun KtCallExpression.coroutineReceiverType(bindingContext: BindingContext): KotlinType? =
    getResolvedCall(bindingContext)?.extensionReceiver?.type

internal fun KotlinType.isCoroutineScopeOrSubtype(): Boolean =
    (listOf(this) + supertypes()).any {
        it.constructor.declarationDescriptor?.fqNameOrNull()?.asString() == COROUTINE_SCOPE_FQ_NAME
    }

internal fun KotlinType.isGlobalScope(): Boolean =
    constructor.declarationDescriptor?.fqNameOrNull()?.asString() == GLOBAL_SCOPE_FQ_NAME

internal fun KtCallExpression.isTopLevelLaunchInSuspendFunction(bindingContext: BindingContext): Boolean {
    val receiver = receiverExpressionOrNull()
    val hasExplicitReceiver = receiver != null &&
        (receiver !is KtThisExpression ||
            (receiver.getLabelName() != null && receiver.getLabelName() !in SCOPE_LAMBDA_LABELS))
    var ancestor = parent
    while (ancestor != null) {
        when (ancestor) {
            is KtFunctionLiteral -> {
                val receiverType = bindingContext[BindingContext.FUNCTION, ancestor]
                    ?.extensionReceiverParameter?.type
                if (!hasExplicitReceiver && receiverType != null && receiverType.isCoroutineScopeOrSubtype()) {
                    return false
                }
            }
            is KtFunction -> return ancestor.hasModifier(KtTokens.SUSPEND_KEYWORD)
            else -> Unit
        }
        ancestor = ancestor.parent
    }
    return false
}

internal fun KtCallExpression.isGlobalScopeLaunchOrAsyncBySyntax(): Boolean {
    val calleeName = calleeExpression?.text ?: return false
    if (calleeName != "launch" && calleeName != "async") return false
    val receiver = receiverExpressionOrNull() ?: return false
    if (receiver.text == GLOBAL_SCOPE_FQ_NAME) return true
    if ((receiver as? KtNameReferenceExpression)?.getReferencedName() != "GlobalScope") return false

    return receiver.containingKtFile.importDirectives.any { directive ->
        directive.importedFqName?.asString() == GLOBAL_SCOPE_FQ_NAME ||
            (directive.isAllUnder && directive.importedFqName?.asString() == COROUTINES_PACKAGE)
    }
}

internal fun KtCallExpression.receiverExpressionOrNull() =
    (parent as? KtQualifiedExpression)
        ?.takeIf { it.selectorExpression == this }
        ?.receiverExpression

private val SCOPE_LAMBDA_LABELS = setOf(
    "coroutineScope",
    "supervisorScope",
    "withContext",
    "launch",
    "async",
)
