package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext

class ComposeModifierMissingRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Always specify modifier for UI Composable functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.isComposableFun().not()) return
        if (function.isComposablePreview()) return
        if (function.typeReference != null && function.typeReference?.text != "Unit") return
        val valueParameterList = function.valueParameterList ?: return

        val isModifierMissing = valueParameterList
            .parameters
            .none { isModifierType(it) }

        if (isModifierMissing) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(function),
                    message = "Always specify modifier for UI Composable functions"
                )
            )
        }
    }

    private fun isModifierType(parameter: KtParameter): Boolean {
        if (bindingContext == BindingContext.EMPTY) {
            if (parameter
                    .containingKtFile
                    .importDirectives.none { it.text == "import androidx.compose.ui.Modifier" }
            ) return false

            return parameter.typeReference?.text == "Modifier"
        }
        // Use type resolution to determine the type of the parameter
        val parameterDescriptor =
            bindingContext[BindingContext.VALUE_PARAMETER, parameter] ?: return false
        val parameterType = parameterDescriptor.type.fqNameOrNull()

        return parameterType == FqName("androidx.compose.ui.Modifier")
    }

    private fun KtFunction.isComposableFun(): Boolean {
        return annotationEntries.any { it.text == "@Composable" }
    }

    private fun KtFunction.isComposablePreview(): Boolean {
        return annotationEntries.any { it.typeReference?.text == "Preview" }
    }
}
