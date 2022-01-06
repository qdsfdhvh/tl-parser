package com.seiko.tl.gradle.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class GramTlExtension(objects: ObjectFactory) {
    val packageName: Property<String> = objects.property<String>()
    val prefix: Property<String> = objects.property<String>()

    val tlSourceDir: Property<String> = objects.property<String>().convention(GRAM_TL_SOURCE_DIR)
    val generatedCodesDir: Property<String> = objects.property<String>().convention(GRAM_TL_GEN_DIR)

    val listMagicNumber: Property<String> = objects.property<String>().convention(LIST_MAGIC_NUMBER)

    companion object {
        private const val GRAM_TL_SOURCE_DIR = "src/main/tl"
        private const val GRAM_TL_GEN_DIR = "generated/source/gramtl/kotlin"
        private const val LIST_MAGIC_NUMBER = "1cb5c415"
    }
}
