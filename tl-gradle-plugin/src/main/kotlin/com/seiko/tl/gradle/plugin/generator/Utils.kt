package com.seiko.tl.gradle.plugin.generator

import org.gradle.configurationcache.extensions.capitalized

// TL_, help.asd -> TL_help_Asd
internal fun generateClassName(prefix: String, name: String): String {
    return buildString {
        if (prefix.isNotEmpty()) {
            append(prefix)
        }
        val array = name.split('.')
        if (array.size < 2) {
            append(name.capitalized())
        } else {
            append(array[0])
            append('_')
            array.drop(1)
                .joinTo(this, "_") {
                    it.capitalized()
                }
        }
    }
}
