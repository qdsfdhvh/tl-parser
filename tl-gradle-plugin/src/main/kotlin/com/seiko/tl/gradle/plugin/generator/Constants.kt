package com.seiko.tl.gradle.plugin.generator

import com.squareup.kotlinpoet.ClassName

internal const val ConstructorName = "`constructor`"
internal const val StreamName = "stream"
internal const val ExceptionName = "exception"
internal const val TLdeserializeName = "TLdeserialize"

internal val AbstractSerializedDataClassName = ClassName("org.telegram.tgnet", "AbstractSerializedData")
internal val TLObjectClassName = ClassName("org.telegram.tgnet", "TLObject")
