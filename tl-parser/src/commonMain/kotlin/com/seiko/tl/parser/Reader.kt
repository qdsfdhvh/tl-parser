package com.seiko.tl.parser

import okio.BufferedSource

interface Reader {
    fun next(): Char
    fun hasNext(): Boolean
}

internal class StringReader(private val content: String) : Reader {

    private var index: Int = 0

    override fun next(): Char {
        return content[index++]
    }

    override fun hasNext(): Boolean {
        return index < content.length
    }
}

internal class OkioSourceReader(private val source: BufferedSource) : Reader {
    override fun next(): Char {
        return source.readByte().toInt().toChar()
    }

    override fun hasNext(): Boolean {
        return source.exhausted().not()
    }
}
