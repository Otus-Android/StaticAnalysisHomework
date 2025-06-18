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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (!isSuspendFunction(function)) return
        if (!function.hasBody()) return

        if (function.isBuildNewCoroutineNotInParentContext()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(function),
                    message = "Link parent job with new child!"
                )
            )
        }
        super.visitNamedFunction(function)
    }

    private fun isSuspendFunction(function: KtNamedFunction): Boolean {
        return function.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) != null
    }

    private fun KtNamedFunction.isBuildNewCoroutineNotInParentContext(): Boolean =
        anyDescendantOfType<KtDotQualifiedExpression> { ktDotExpression ->
            (buildUsingLaunchOrAsync(ktDotExpression) || buildUsingLaunchIn(ktDotExpression)) &&
                    buildInCoroutineOrSupervisorScope(ktDotExpression).not()
        }

    private fun buildUsingLaunchOrAsync(ktDotExpression: KtDotQualifiedExpression): Boolean =
        ktDotExpression.receiverExpression.getType(bindingContext)?.isCoroutineScope() == true &&
                ktDotExpression.getCalleeExpressionIfAny()?.text in listOf("launch", "async")

    private fun buildUsingLaunchIn(ktDotExpression: KtDotQualifiedExpression): Boolean =
        ktDotExpression.receiverExpression.getType(bindingContext)?.isCoroutinesFlow() == true &&
                ktDotExpression.getCalleeExpressionIfAny()?.text == "launchIn"

    private fun buildInCoroutineOrSupervisorScope(ktDotExpression: KtDotQualifiedExpression): Boolean =
        ktDotExpression.getCalleeExpressionIfAny()?.isInsideCoroutineScope() == true

    private fun KtExpression.isInsideCoroutineScope(): Boolean {
        var current: PsiElement? = this.parent

        while (current != null) {
            if (current is KtCallExpression) {
                val calleeText = current.calleeExpression?.text
                if (calleeText == "coroutineScope" || calleeText == "supervisorScope") {
                    return true
                }
            }
            current = current.parent
        }

        return false
    }

    private fun KotlinType.isCoroutineScope(): Boolean =
        sequence {
            yield(this@isCoroutineScope)
            yieldAll(this@isCoroutineScope.supertypes())
        }
            .mapNotNull {
                it.fqNameOrNull()?.asString()
            }
            .contains("kotlinx.coroutines.CoroutineScope")

    private fun KotlinType.isCoroutinesFlow(): Boolean =
        sequence {
            yield(this@isCoroutinesFlow)
            yieldAll(this@isCoroutinesFlow.supertypes())
        }
            .mapNotNull { it.fqNameOrNull()?.asString() }
            .contains("kotlinx.coroutines.flow.Flow")
}
