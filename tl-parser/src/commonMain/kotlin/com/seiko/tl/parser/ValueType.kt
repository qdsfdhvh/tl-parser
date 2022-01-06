package com.seiko.tl.parser

sealed interface ValueType {
    data object STRING : ValueType
    data class BOOLEAN(val value: Boolean) : ValueType
    data object INT : ValueType
    data object LONG : ValueType
    data class VECTOR(val type: ValueType) : ValueType
    data class CLASS(val name: String) : ValueType

    companion object {
        fun parse(valueString: String): ValueType {
            return when {
                valueString.equals("string", true) -> STRING
                valueString.equals("bool", true) -> BOOLEAN(false)
                valueString.equals("true", true) -> BOOLEAN(true)
                valueString.equals("false", true) -> BOOLEAN(false)
                valueString.equals("int", true) -> INT
                valueString.equals("long", true) -> LONG
                valueString.startsWith("vector", true) -> VECTOR(
                    parse(valueString.substring(7, valueString.length - 1)),
                )
                else -> CLASS(valueString)
            }
        }
    }
}
