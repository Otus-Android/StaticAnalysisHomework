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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
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

        if (bindingContext == BindingContext.EMPTY) return

        val functionName = expression.calleeExpression?.text ?: return
        if (functionName !in setOf("launch", "async")) return

        val parentFunction = expression.getStrictParentOfType<KtNamedFunction>() ?: return
        if (!parentFunction.hasModifier(KtTokens.SUSPEND_KEYWORD)) return

        val receiver = expression.getQualifiedExpressionForSelector()?.receiverExpression

        val isOnCoroutineScope = receiver?.isCoroutineScopeReceiver(expression, parentFunction)
            ?: expression.isImplicitReceiverFunctionScope(parentFunction)
        if (!isOnCoroutineScope) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "`$functionName` on a CoroutineScope inside a suspend function"
            )
        )
    }

    private fun KtExpression.isCoroutineScopeReceiver(
        call: KtCallExpression,
        function: KtNamedFunction,
    ): Boolean {
        if (this is KtThisExpression) {
            val label = getLabelName()
            val refersToScopeBuilder = label == null || label in setOf("coroutineScope", "supervisorScope")
            if (refersToScopeBuilder && call.isInsideScopeBuilderLambda(function)) return false
        }
        return bindingContext.getType(this)?.isCoroutineScope() == true
    }

    private fun KtCallExpression.isImplicitReceiverFunctionScope(
        function: KtNamedFunction
    ): Boolean {
        if (lambdasUpTo(function).any()) return false

        val descriptor = bindingContext[BindingContext.FUNCTION, function] ?: return false
        return listOfNotNull(
            descriptor.extensionReceiverParameter?.type,
            descriptor.dispatchReceiverParameter?.type,
        ).any { it.isCoroutineScope() }
    }

    private fun KtCallExpression.isInsideScopeBuilderLambda(
        function: KtNamedFunction
    ): Boolean =
        lambdasUpTo(function).any { it.owningCallName() in setOf("coroutineScope", "supervisorScope") }

    private fun KtExpression.lambdasUpTo(function: KtNamedFunction): Sequence<KtLambdaExpression> =
        generateSequence(parent) { it.parent }
            .takeWhile { it != function }
            .filterIsInstance<KtLambdaExpression>()

    private fun KtLambdaExpression.owningCallName(): String? =
        (parent as? KtValueArgument)
            ?.getStrictParentOfType<KtCallExpression>()
            ?.calleeExpression
            ?.text

    private fun KotlinType.isCoroutineScope(): Boolean =
        (listOf(this) + supertypes()).any {
            it.fqNameOrNull() ==
                    FqName("kotlinx.coroutines.CoroutineScope")
        }
}
