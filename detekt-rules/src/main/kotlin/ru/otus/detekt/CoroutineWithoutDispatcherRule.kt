package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe


class CoroutineWithoutDispatcherRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "Coroutine launch/async should explicitly specify a dispatcher",
        debt = Debt.TWENTY_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return
        if (callee != "launch" && callee != "async") return

        val context = bindingContext ?: return


        val valueArgs = expression.valueArguments
        val hasDispatcher = valueArgs.any { arg ->
            val expr = arg.getArgumentExpression() ?: return@any false
            val type = context.getType(expr) ?: return@any false

            val fqName =
                type.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return@any false
            fqName == "kotlin.coroutines.CoroutineContext" || fqName.startsWith("kotlinx.coroutines.CoroutineDispatcher")
        }

        if (!hasDispatcher && expression.lambdaArguments.isNotEmpty()) {

            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Call to '$callee' should specify a dispatcher (e.g. Dispatchers.IO)"
                )
            )
        }
    }
}
