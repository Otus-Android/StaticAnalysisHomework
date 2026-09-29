package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text !in COROUTINE_BUILDERS) return

        val callee = expression.calleeExpression as? KtReferenceExpression ?: return
        val target = bindingContext[BindingContext.REFERENCE_TARGET, callee]
            as? FunctionDescriptor ?: return
        val receiverType = target.extensionReceiverParameter?.type ?: return

        if (!receiverType.isCoroutineScopeOrSubtype()) return
        if (isInsideScopeBuilder(expression)) return

        val enclosingFunction = expression.parentsWithSelf
            .filterIsInstance<KtNamedFunction>()
            .firstOrNull() ?: return

        if (!enclosingFunction.isSuspend()) return

        report(
            CodeSmell(
                issue = issue,
                entity = expression.findReportingEntity(),
                message = "${expression.calleeExpression?.text} in a suspend function breaks structured concurrency. " +
                    "Use coroutineScope or supervisorScope instead."
            )
        )
    }

    private fun KotlinType.isCoroutineScopeOrSubtype(
        visited: MutableSet<TypeConstructor> = mutableSetOf()
    ): Boolean {
        if (!visited.add(constructor)) return false
        if (constructor.declarationDescriptor?.fqNameSafe == COROUTINE_SCOPE_FQ_NAME) return true
        return constructor.supertypes.any { it.isCoroutineScopeOrSubtype(visited) }
    }

    private fun isInsideScopeBuilder(expression: KtCallExpression): Boolean =
        expression.parentsWithSelf
            .takeWhile { it !is KtNamedFunction }
            .filterIsInstance<KtCallExpression>()
            .any { it.calleeExpression?.text in SCOPE_BUILDERS }

    private fun KtNamedFunction.isSuspend(): Boolean =
        modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true

    private fun KtCallExpression.findReportingEntity(): Entity =
        (parent as? KtDotQualifiedExpression)?.let { Entity.from(it) } ?: Entity.from(this)

    private companion object {
        private val COROUTINE_SCOPE_FQ_NAME = FqName("kotlinx.coroutines.CoroutineScope")
        private val COROUTINE_BUILDERS = setOf("launch", "async")
        private val SCOPE_BUILDERS = setOf("coroutineScope", "supervisorScope")
    }
}
