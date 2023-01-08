package me.kuku.telegram.utils

import me.kuku.telegram.config.TelegramAbilityExceptionEvent
import me.kuku.utils.JobManager
import org.telegram.abilitybots.api.objects.*

class AbilitySubscriber {

    private val abilityMap = mutableMapOf<String, Ability>()

    operator fun String.invoke(block: suspend AbilityContext.() -> Unit) {
        val ability = ability(this, this, block = block)
        abilityMap[this] = ability
    }

    fun sub(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend AbilityContext.() -> Unit) {
        val ability = ability(name, info, input, reply, locality, privacy, block)
        abilityMap[name] = ability
    }

    private fun ability(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
                        privacy: Privacy = Privacy.PUBLIC, block: suspend AbilityContext.() -> Unit): Ability {
        return Ability.builder().locality(locality).privacy(privacy).name(name).info(info).input(input).action {
            JobManager.now {
                invokeAbility(AbilityContext(it), block)
            }
        }.also { reply?.let { r -> it.reply(r) } }.build()
    }

    private suspend fun invokeAbility(abilityContext: AbilityContext, block: suspend AbilityContext.() -> Unit) {
        runCatching {
            block.invoke(abilityContext)
        }.onFailure {
            context.publishEvent(TelegramAbilityExceptionEvent(abilityContext.messageContext, it))
        }
    }

}


typealias CallbackSubscriber = CallBackQ
