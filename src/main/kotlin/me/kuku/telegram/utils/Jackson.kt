package me.kuku.telegram.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

object Jackson {

    var objectMapper: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .registerModules(JavaTimeModule(), kotlinModule())


    fun readTree(json: String): JsonNode {
        return objectMapper.readTree(json)
    }

    fun createObjectNode(): ObjectNode {
        return objectMapper.createObjectNode()
    }

    fun createArrayNode(): ArrayNode {
        return objectMapper.createArrayNode()
    }

    inline fun <reified T: Any> convertValue(jsonNode: JsonNode): T {
        return objectMapper.convertValue(jsonNode, object: TypeReference<T>() {})
    }

    fun writeValueAsString(any: Any): String {
        return objectMapper.writeValueAsString(any)
    }

}

fun String.toJsonNode(): JsonNode {
    return Jackson.readTree(this)
}

fun String.jsonpToJsonNode(): JsonNode {
    return """\{(?:[^{}]|\{[^{}]*})*}""".toRegex().find(this)?.value?.toJsonNode() ?: error("json not found")
}

inline fun <reified T: Any> JsonNode.convertValue(): T {
    return Jackson.convertValue(this)
}

