package me.kuku.telegram.utils

import org.telegram.abilitybots.api.objects.*

class AbilitySubscriber {

    private val abilityMap = mutableMapOf<String, Ability>()

    operator fun String.invoke(block: suspend MessageContext.() -> Unit) {
        val ability = ability(this, block = block)
        abilityMap[this] = ability
    }

    fun sub(name: String, info: String = "这个命令没有描述", input: Int = 0, reply: Reply? = null, locality: Locality = Locality.ALL,
            privacy: Privacy = Privacy.PUBLIC, block: suspend MessageContext.() -> Unit) {
        val ability = ability(name, info, input, reply, locality, privacy, block)
        abilityMap[name] = ability
    }

}


typealias CallbackSubscriber = CallBackQ
