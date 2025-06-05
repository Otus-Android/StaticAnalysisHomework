package ru.otus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class OtusRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "OtusRuleSet"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                ComposeModifierMissingRule(config),
                GlobalScopeRule(config),
                TopLevelCoroutineInSuspendFunRule(config),
            ),
        )
    }
}
