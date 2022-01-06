package com.seiko.tl.parser

data class TokenResult(
    val tokens: List<Token>,
    val childClassListMap: Map<String, Set<String>>,
)
