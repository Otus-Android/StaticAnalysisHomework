package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import io.gitlab.arturbosch.detekt.rules.fqNameOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.resolve.BindingContext

@RequiresTypeResolution
class TopLevelCoroutineInSuspendFunRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid running top level coroutines inside suspend functions",
        debt = Debt.FIVE_MINS
    )

    private val RE_LAUNCH = "\\s*\\.\\s*launch\\W"
    private val RE_ASYNC = "\\s*\\.\\s*async\\W"
    private val RE_PARENT = ":\\s*CoroutineScope\\W"
    private val COROUTINESCOPE = "CoroutineScope"
    private val FULLSCOPENAME = "kotlinx.coroutines.CoroutineScope"
    private val RE_NAME = "(\\w+)"

    companion object {
        private val childNameList : MutableList<String> = mutableListOf()
    }

    private fun ExtractName(text : String) : String {
        val end = text.indexOf("(")
        val result = Regex(RE_NAME).find(text.substring(0, if (end < 0) text.length else end))
        if (result != null) {
            if (result.groupValues.size > 1)
                return result.groupValues[1]
        }
        return ""
    }

    private fun CoroutineScopeChild(name : String) : Boolean {
        return childNameList.contains(name)
    }

    private fun CoroutineScopeParameter(name : String, list : KtParameterList?) : Boolean {
        if (list == null)
            return false

        if (bindingContext == BindingContext.EMPTY)
            return false

        for (param in list.parameters) {
            if (!name.equals(param.name))
                continue

            val descriptor = bindingContext[BindingContext.VALUE_PARAMETER, param] ?: continue
            val paramType = descriptor.type.fqNameOrNull()
            if (paramType == FqName(FULLSCOPENAME))
                return true

            if (CoroutineScopeChild(descriptor.type.toString()))
                return true
        }
        return false
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        if (klass.getClassOrInterfaceKeyword() != null) {
            if (klass.text.contains(Regex(RE_PARENT))) {
                if (klass.name != null) {
                    childNameList.add(klass.name!!)
                }
            }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (!function.text.startsWith("suspend"))
            return

        val parameterList = function.valueParameterList
        var offset = 0
        val lines = function.text.lines()
        for (line in lines) {
            offset += line.length
            var pos = 0
            if (line.contains(Regex(RE_LAUNCH))) {
                pos = line.indexOf("launch")
            }
            else if (line.contains(Regex(RE_ASYNC))) {
                pos = line.indexOf("async")
            }
            if (pos > 0) {
                val name = ExtractName(line.substring(0, pos))
                if (name == COROUTINESCOPE) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(function, offset),
                            message = "Detect CoroutineScope().launch in suspend function"
                        )
                    )
                }
                else if (CoroutineScopeChild(name)) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(function, offset),
                            message = "Detect [child of CoroutineScope].launch in suspend function"
                        )
                    )
                }
                else if (CoroutineScopeParameter(name, parameterList)) {
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(function, offset),
                            message = "Detect [CoroutineScope parameter].launch in suspend function"
                        )
                    )
                }
            }
            ++offset // skip '\n'
        }
    }
}
