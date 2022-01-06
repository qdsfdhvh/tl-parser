package com.seiko.tl.parser

sealed interface Token
data class ClassToken(val name: String) : Token
data class ConstructorToken(val constructor: String) : Token
data class FlagsToken(val value: String) : Token {
    companion object {
        const val NAME = "flags"
    }
}

sealed interface ValueToken : Token {
    val name: String
    val type: ValueType
}

fun ValueToken(name: String, type: ValueType): ValueToken = SimpleValueToken(
    name = name,
    type = type,
)

data class SimpleValueToken(
    override val name: String,
    override val type: ValueType,
) : ValueToken

data class FlagsValueToken(
    override val name: String,
    override val type: ValueType,
    val index: Int,
) : ValueToken

data class ReturnToken(val type: ValueType) : Token
