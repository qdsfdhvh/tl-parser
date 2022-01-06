package com.seiko.tl.parser

import okio.BufferedSource
import okio.use

object GramTLParser {

    private enum class ParseState {
        START,
        MAYBE_IGNORE,
        IGNORE,
        CLASS,
        CONSTRUCTOR,
        VALUE_NAME,
        VALUE_TYPE,
        FLAGS_VALUE_TYPE,
        RETURN,
    }

    fun parseTokens(source: BufferedSource): TokenResult {
        return source.use {
            parseTokens(OkioSourceReader(it))
        }
    }

    fun parseTokens(content: String): TokenResult {
        return parseTokens(StringReader(content))
    }

    fun parseTokens(reader: Reader): TokenResult {
        val tokens = mutableListOf<Token>()
        val childClassListMap = mutableMapOf<String, MutableSet<String>>()

        var state = ParseState.START
        var currentToken = StringBuilder()

        var currentName = ""
        var currentIsFlagsValue = false
        var currentFlagsIndex = 0

        var currentClassName = ""

        fun reset() {
            state = ParseState.START
            currentToken = StringBuilder()

            currentName = ""
            currentFlagsIndex = 0

            currentClassName = ""
        }

        while (reader.hasNext()) {
            val char = reader.next()
            when (state) {
                ParseState.START -> {
                    if (char.isLetterOrDigit()) {
                        currentToken.append(char)
                        state = ParseState.CLASS
                    } else if (char == ' ' || char == '\n' || char == '\r') {
                        // ignore char
                    } else if (char == '/') {
                        currentToken.append(char)
                        state = ParseState.MAYBE_IGNORE
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.MAYBE_IGNORE -> {
                    if (char == '/') {
                        currentToken = StringBuilder()
                        state = ParseState.IGNORE
                    } else {
                        currentToken.append(char)
                        state = ParseState.CLASS
                    }
                }
                ParseState.IGNORE -> {
                    if (char == '\n') {
                        state = ParseState.START
                    }
                }
                ParseState.CLASS -> {
                    if (char == '#') {
                        currentName = currentToken.toString()
                        tokens.add(ClassToken(currentName))

                        currentClassName = currentName
                        currentToken = StringBuilder()
                        state = ParseState.CONSTRUCTOR
                    } else if (char.isLetterOrDigit() || char == '.') {
                        currentToken.append(char)
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.CONSTRUCTOR -> {
                    if (char == ' ') {
                        tokens.add(ConstructorToken(currentToken.toString()))

                        currentToken = StringBuilder()
                        state = ParseState.VALUE_NAME
                    } else if (char.isLetterOrDigit()) {
                        currentToken.append(char)
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.VALUE_NAME -> {
                    if (char.isLetterOrDigit() || char == '_') {
                        currentToken.append(char)
                    } else if (char == ':') {
                        currentName = currentToken.toString()

                        currentToken = StringBuilder()
                        state = ParseState.VALUE_TYPE
                    } else if (char == '=') {
                        currentToken = StringBuilder()
                        state = ParseState.RETURN
                    } else if (char == ' ') {
                        // ignore char
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.VALUE_TYPE -> {
                    if (char.isLetterOrDigit() ||
                        char == '<' ||
                        char == '>' ||
                        char == '_'
                    ) {
                        currentToken.append(char)
                    } else if (char == ' ') {
                        if (currentIsFlagsValue) {
                            currentIsFlagsValue = false
                            tokens.add(FlagsValueToken(currentName, ValueType.parse(currentToken.toString()), currentFlagsIndex))
                        } else {
                            tokens.add(ValueToken(currentName, ValueType.parse(currentToken.toString())))
                        }
                        currentToken = StringBuilder()
                        state = ParseState.VALUE_NAME
                    } else if (char == '#') {
                        if (currentName == FlagsToken.NAME) {
                            tokens.add(FlagsToken(currentToken.toString()))

                            currentToken = StringBuilder()
                            state = ParseState.VALUE_NAME
                        } else {
                            throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                        }
                    } else if (char == '.') {
                        if (currentToken.toString() == FlagsToken.NAME) {
                            currentIsFlagsValue = true
                            currentToken = StringBuilder()
                            state = ParseState.FLAGS_VALUE_TYPE
                        } else {
                            currentToken.append(char)
                        }
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.FLAGS_VALUE_TYPE -> {
                    if (char.isDigit()) {
                        currentToken.append(char)
                    } else if (char == '?') {
                        currentFlagsIndex = currentToken.toString().toInt()

                        currentToken = StringBuilder()
                        state = ParseState.VALUE_TYPE
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
                ParseState.RETURN -> {
                    if (char.isLetterOrDigit() || char == '.') {
                        currentToken.append(char)
                    } else if (char == ';') {
                        val returnClassName = currentToken.toString()
                        val returnType = ValueType.parse(returnClassName)
                        tokens.add(ReturnToken(returnType))
                        if (returnType is ValueType.CLASS) {
                            childClassListMap.getOrPut(returnType.name) {
                                mutableSetOf()
                            }.add(currentClassName)
                        }
                        reset()
                    } else if (char == ' ') {
                        // ignore char
                    } else {
                        throw IllegalArgumentException("Invalid input format, state: $state, char: $char")
                    }
                }
            }
        }
        return TokenResult(
            tokens = tokens,
            childClassListMap = childClassListMap,
        )
    }
}
