package com.seiko.tl.gradle.plugin.generator

data class GenerateContext(
    val packageName: String,
    val prefix: String,
    val listMagicNumber: String,
    val currentClassName: String,
)
