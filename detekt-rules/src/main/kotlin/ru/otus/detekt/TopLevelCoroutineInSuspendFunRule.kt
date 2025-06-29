package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS,
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val isSuspend = function.firstChild.text == "suspend"
        if (isSuspend) {
            function.bodyBlockExpression?.forEachChildOfType(
                types = setOf(KtNodeTypes.DOT_QUALIFIED_EXPRESSION)
            ) { dotQualifiedExpression ->
                var reference = dotQualifiedExpression.firstChild
                if (reference is KtCallExpression) {
                    reference = reference.firstChild
                }
                val callingFun = dotQualifiedExpression.lastChild.firstChild
                if (bindingContext != BindingContext.EMPTY) {
                    val descriptor = bindingContext[
                        BindingContext.REFERENCE_TARGET,
                        reference as KtReferenceExpression
                    ]

                    // e.g. GlobalScope.launch {
                    val isCoroutineScopeClass = (descriptor as? DeserializedClassDescriptor)
                        ?.isCoroutineScope()
                    // e.g. scopeVal.launch {
                    val isCoroutineScopeValue = (descriptor as? LocalVariableDescriptor)
                        ?.isCoroutineScope()
                    // e.g. CoroutineScope(Dispatchers.IO).launch {
                    val isCoroutineScopeSimpleFunction =
                        (descriptor as? DeserializedSimpleFunctionDescriptor)
                            ?.isCoroutineScope()

                    if (isCoroutineScopeClass == true || isCoroutineScopeValue == true ||
                        isCoroutineScopeSimpleFunction == true
                    ) {
                        if (callingFun != null) {
                            val funText = callingFun.text
                            if (funText == "launch" || funText == "async") {
                                report(
                                    CodeSmell(
                                        issue = issue,
                                        entity = Entity.from(dotQualifiedExpression),
                                        message = "Avoid running top level coroutines inside suspend functions" +
                                                "More: " +
                                                "https://elizarov.medium.com/structured-concurrency-722d765aa952"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val coroutineScopeFqName = FqName("kotlinx.coroutines.CoroutineScope")

    private fun DeserializedSimpleFunctionDescriptor.isCoroutineScope() =
        this.returnType?.fqNameOrNull() == FqName("kotlinx.coroutines.CoroutineScope")

    private fun DeserializedClassDescriptor.isCoroutineScope() =
        this.fqNameOrNull() == coroutineScopeFqName ||
                this.typeConstructor.supertypes.any { type ->
                    type.fqNameOrNull() == coroutineScopeFqName
                }

    private fun LocalVariableDescriptor.isCoroutineScope() =
        this.fqNameOrNull() == coroutineScopeFqName ||
                this.type.constructor
                    .supertypes.any { type ->
                        type.fqNameOrNull() == coroutineScopeFqName
                    }
}
