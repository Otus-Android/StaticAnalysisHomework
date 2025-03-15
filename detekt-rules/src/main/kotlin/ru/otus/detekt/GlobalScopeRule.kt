package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtScript

class GlobalScopeRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Avoid using GlobalScope",
        debt = Debt.FIVE_MINS
    )

    private val RE_GLOBALSCOPE_LAUNCH = "\\WGlobalScope\\s*\\.\\s*launch\\W"
    private val RE_GLOBALSCOPE_ASYNC = "\\WGlobalScope\\s*\\.\\s*async\\W"
    private val RE_VIEWMODEL = ":\\s*ViewModel\\W"
    private val RE_FRAGMENT = ":\\s*Fragment\\W"

    companion object {
        private var viewModelExt: Boolean = false
        private var lintHint = ""
    }

    override fun visitScript(script: KtScript) {
      super.visitScript(script)
      if (script.name.equals("build.gradle") || script.name.equals("build.gradle.kts")) {
        val lines = script.text.lines()
        for (line in lines) {
          if (line.contains("androidx.lifecycle:lifecycle-viewmodel-ktx")) {
            viewModelExt = true
            break
          }
        }
      }
    }

    override fun visitClass(className: KtClass) {
        val nameLen = className.name?.length ?: 0
        if (nameLen > 0) {
            lintHint = ""
            val nameEnd = className.text.indexOf(className.name!!) + nameLen
            val bodyPos = className.text.indexOf("{", nameEnd)
            if (bodyPos >= nameEnd) {
                val text = className.text.substring(nameEnd, bodyPos)
                if (text.contains(Regex(RE_FRAGMENT))) {
                    lintHint = "; Change it to lifecycleScope"
                } else if (text.contains(Regex(RE_VIEWMODEL)) && viewModelExt) {
                    lintHint = "; Change it to ViewModelScope"
                }
            }
            super.visitClass(className)
            lintHint = ""
        }
        else {
            super.visitClass(className)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
      super.visitNamedFunction(function)
      var offset = 0
      val lines = function.text.lines()
      for (line in lines) {
        offset += line.length
        if (line.contains(Regex(RE_GLOBALSCOPE_LAUNCH))) {
          report(
            CodeSmell(
              issue = issue,
              entity = Entity.from(function, offset),
              message = "Detect GlobalScope.launch usage" + lintHint
            )
          )
        }
        else if (line.contains(Regex(RE_GLOBALSCOPE_ASYNC))) {
          report(
            CodeSmell(
              issue = issue,
              entity = Entity.from(function, offset),
              message = "Detect GlobalScope.async usage" + lintHint
            )
          )
        }
        ++offset // skip '\n'
      }
    }

}
