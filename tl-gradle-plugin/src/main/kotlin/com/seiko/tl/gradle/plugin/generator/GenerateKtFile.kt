package com.seiko.tl.gradle.plugin.generator

import com.seiko.tl.parser.ClassToken
import com.seiko.tl.parser.ConstructorToken
import com.seiko.tl.parser.FlagsToken
import com.seiko.tl.parser.FlagsValueToken
import com.seiko.tl.parser.GramTLParser
import com.seiko.tl.parser.ReturnToken
import com.seiko.tl.parser.ValueToken
import com.seiko.tl.parser.ValueType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.withIndent
import okio.buffer
import okio.source
import java.io.File

internal fun generateKtFile(
    context: GenerateContext,
    codeDir: File,
    tlFile: File,
) {
    val builder = FileSpec.builder(
        context.packageName,
        tlFile.name.substringBeforeLast('.'),
    )

    val tokenResult = GramTLParser.parseTokens(tlFile.source().buffer())

    var currentClassName = ""
    var currentClassKey = ""
    var currentClassConstructor = ""
    var currentValues: MutableList<ValueToken> = mutableListOf()

    fun reset() {
        currentClassName = ""
        currentClassKey = ""
        currentClassConstructor = ""
        currentValues = mutableListOf()
    }

    tokenResult.tokens.forEach { token ->
        when (token) {
            is ClassToken -> {
                reset()
                currentClassName = generateClassName(context.prefix, token.name)
                currentClassKey = token.name
            }
            is ConstructorToken -> {
                currentClassConstructor = token.constructor
            }
            is FlagsToken -> {
                currentValues.add(ValueToken(FlagsToken.NAME, ValueType.INT))
            }
            is ValueToken -> {
                currentValues.add(token)
            }
            is ReturnToken -> {
                val returnClassName = when (val type = token.type) {
                    is ValueType.BOOLEAN -> context.prefix + "Bool"
                    is ValueType.CLASS -> generateClassName(context.prefix, type.name)
                    else -> ""
                }

                val childClassNameList = if (currentClassKey.isNotEmpty()) {
                    // ignoreCase for CurrentClassKey
                    val realCurrentClassKey = tokenResult.childClassListMap.keys.find { it.contains(currentClassKey, ignoreCase = true) }
                    tokenResult.childClassListMap[realCurrentClassKey]
                        ?.mapTo(mutableSetOf()) {
                            generateClassName(context.prefix, it)
                        }.orEmpty()
                } else {
                    emptySet()
                }

                val classBuilder = TypeSpec.classBuilder(currentClassName)
                    .superclass(TLObjectClassName)

                if (currentValues.isNotEmpty()) {
                    // data class (...)
                    classBuilder
                        .addModifiers(KModifier.DATA)
                        .primaryConstructor(
                            FunSpec.constructorBuilder()
                                .addModifiers(KModifier.INTERNAL)
                                .addParameters(
                                    currentValues
                                        .map { value ->
                                            ParameterSpec.builder(value.name, value.type.asKtType(context))
                                                .defaultValue("%L", value.defaultValue(context))
                                                .build()
                                        },
                                )
                                .build(),
                        )
                        .addProperties(
                            currentValues
                                .map { value ->
                                    PropertySpec.builder(value.name, value.type.asKtType(context))
                                        .initializer(value.name)
                                        .mutable()
                                        .addAnnotation(JvmField::class)
                                        .build()
                                },
                        )
                }

                classBuilder
                    .addFunction(
                        FunSpec.builder("deserializeResponse")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(StreamName, AbstractSerializedDataClassName)
                            .addParameter(ConstructorName, INT)
                            .addParameter(ExceptionName, BOOLEAN)
                            .returns(TLObjectClassName.copy(nullable = true))
                            .addStatement("return %L.$TLdeserializeName($StreamName, $ConstructorName, $ExceptionName)", returnClassName)
                            .build(),
                    )

                if (currentValues.isNotEmpty()) {
                    classBuilder
                        .addFunction(
                            FunSpec.builder("readParams")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter(StreamName, AbstractSerializedDataClassName)
                                .addParameter(ExceptionName, BOOLEAN)
                                .readStreamValues(
                                    context = context.copy(currentClassName = currentClassName),
                                    tokens = currentValues,
                                )
                                .build(),
                        )
                }

                classBuilder
                    .addFunction(
                        FunSpec.builder("serializeToStream")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(StreamName, AbstractSerializedDataClassName)
                            .writeStreamValues(context.copy(currentClassName = currentClassName), currentValues)
                            .build(),
                    )
                    // companion object
                    .addType(
                        TypeSpec.companionObjectBuilder()
                            .addProperty(
                                PropertySpec.builder(ConstructorName, INT)
                                    .getter(
                                        FunSpec.getterBuilder()
                                            .addStatement("return 0x%L.toInt()", currentClassConstructor)
                                            .build(),
                                    )
                                    .build(),
                            )
                            .addFunction(
                                FunSpec.builder(TLdeserializeName)
                                    .addParameter(StreamName, AbstractSerializedDataClassName)
                                    .addParameter(ConstructorName, INT)
                                    .addParameter(ExceptionName, BOOLEAN)
                                    .apply {
                                        if (childClassNameList.isEmpty() || childClassNameList.first() == currentClassName) {
                                            returns(ClassName(context.packageName, currentClassName).copy(nullable = true))
                                        } else {
                                            returns(TLObjectClassName.copy(nullable = true))
                                        }
                                    }
                                    .objectTLDeserialize(
                                        context = context.copy(currentClassName = currentClassName),
                                        childClassNameList = childClassNameList,
                                    )
                                    .build(),
                            )
                            .build(),
                    )

                builder.addType(classBuilder.build())
            }
            else -> {
            }
        }
    }

    builder.build().writeTo(codeDir)
}

private fun FunSpec.Builder.objectTLDeserialize(
    context: GenerateContext,
    childClassNameList: Set<String>,
) = apply {
    addCode(
        buildCodeBlock {
            if (childClassNameList.isEmpty()) {
                addStatement("if (this.$ConstructorName != $ConstructorName) {")
                withIndent {
                    addStatement("if ($ExceptionName) {")
                    withIndent {
                        addStatement("error(\"can't parse magic \${$ConstructorName} in \" + \"${context.currentClassName}\")")
                    }
                    addStatement("}")
                    addStatement("return null")
                }
                addStatement("}")
                objectTLDeserializeSelf(context)
            } else {
                addStatement("when ($ConstructorName) {")
                withIndent {
                    childClassNameList.forEach { childClassName ->
                        addStatement("%L.$ConstructorName -> {", childClassName)
                        withIndent {
                            if (childClassName == context.currentClassName) {
                                objectTLDeserializeSelf(context)
                            } else {
                                addStatement("return %L.$TLdeserializeName($StreamName, $ConstructorName, $ExceptionName)", childClassName)
                            }
                        }
                        addStatement("}")
                    }
                    addStatement("else -> {")
                    withIndent {
                        addStatement("if ($ExceptionName) {")
                        withIndent {
                            addStatement("error(\"can't parse magic \${$ConstructorName} in \" + \"${context.currentClassName}\")")
                        }
                        addStatement("}")
                        addStatement("return null")
                    }
                    addStatement("}")
                }
                addStatement("}")
            }
        },
    )
}

private fun CodeBlock.Builder.objectTLDeserializeSelf(
    context: GenerateContext,
) = apply {
    addStatement("val result = %L()", context.currentClassName)
    addStatement("result.readParams($StreamName, $ExceptionName)")
    addStatement("return result")
}

private fun FunSpec.Builder.readStreamValues(
    context: GenerateContext,
    tokens: List<ValueToken>,
) = apply {
    addCode(
        buildCodeBlock {
            if (tokens.any { it.type is ValueType.VECTOR }) {
                addStatement("var magic: Int")
                addStatement("var count: Int")
            }
            tokens.forEach { token ->
                if (token is FlagsValueToken) {
                    addStatement("if (${FlagsToken.NAME} and (1 shl ${token.index}) != 0) {")
                    withIndent {
                        readType(context, token.type, token.name)
                    }
                    addStatement("}")
                } else {
                    readType(context, token.type, token.name)
                }
            }
        },
    )
}

private fun CodeBlock.Builder.readType(
    context: GenerateContext,
    type: ValueType,
    name: String,
) {
    when (type) {
        ValueType.STRING -> {
            addStatement("%L = $StreamName.readString($ExceptionName)", name)
        }
        is ValueType.BOOLEAN -> {
            addStatement("%L = $StreamName.readBool($ExceptionName)", name)
        }
        ValueType.INT -> {
            addStatement("%L = $StreamName.readInt32($ExceptionName)", name)
        }
        ValueType.LONG -> {
            addStatement("%L = $StreamName.readInt64($ExceptionName)", name)
        }
        is ValueType.VECTOR -> {
            addStatement("magic = stream.readInt32($ExceptionName)")
            addStatement("if (magic != 0x%L) {", context.listMagicNumber)
            withIndent {
                addStatement("if ($ExceptionName) {")
                withIndent {
                    addStatement("error(\"wrong Vector magic: \${magic} in \" + \"${context.currentClassName}\")")
                }
                addStatement("}")
                addStatement("return")
            }
            addStatement("}")
            addStatement("count = stream.readInt32($ExceptionName)")
            addStatement("%L = List(count) {", name)
            withIndent {
                addStatement("val value: %L", type.type.asKtType(context))
                readType(context, type.type, "value")
                addStatement("value")
            }
            addStatement("}")
        }
        is ValueType.CLASS -> {
            addStatement(
                "%L = %L.$TLdeserializeName($StreamName, $StreamName.readInt32($ExceptionName), $ExceptionName) ?: %L()",
                name,
                generateClassName(context.prefix, type.name),
                generateClassName(context.prefix, type.name),
            )
        }
    }
}

private fun FunSpec.Builder.writeStreamValues(
    context: GenerateContext,
    tokens: List<ValueToken>,
) = apply {
    addCode(
        buildCodeBlock {
            addStatement("$StreamName.writeInt32($ConstructorName)")
            tokens.forEach { token ->
                // (bool) ? (flags | 64) : (flags &~ 64)
                if (token is FlagsValueToken) {
                    addStatement("if (${token.name} != ${token.defaultValue(context)}) {")
                    withIndent {
                        addStatement("${FlagsToken.NAME} or 1 shl ${token.index}")
                    }
                    addStatement("} else {")
                    withIndent {
                        addStatement("${FlagsToken.NAME} and ${FlagsToken.NAME}.inv().shl(${token.index}).inv()")
                    }
                    addStatement("}")
                }
            }
            tokens.forEach { token ->
                if (token is FlagsValueToken) {
                    addStatement("if (${FlagsToken.NAME} and (1 shl ${token.index}) != 0) {")
                    withIndent {
                        writeType(context, token.type, token.name)
                    }
                    addStatement("}")
                } else {
                    writeType(context, token.type, token.name)
                }
            }
        },
    )
}

private fun CodeBlock.Builder.writeType(
    context: GenerateContext,
    type: ValueType,
    name: String,
) {
    when (type) {
        ValueType.STRING -> {
            addStatement("$StreamName.writeString(%L)", name)
        }
        is ValueType.BOOLEAN -> {
            addStatement("$StreamName.writeBool(%L)", name)
        }
        ValueType.INT -> {
            addStatement("$StreamName.writeInt32(%L)", name)
        }
        ValueType.LONG -> {
            addStatement("$StreamName.writeInt64(%L)", name)
        }
        is ValueType.VECTOR -> {
            addStatement("$StreamName.writeInt32(0x%L)", context.listMagicNumber)
            addStatement("$StreamName.writeInt32(%L.size)", name)
            addStatement("%L.forEach { value ->", name)
            withIndent {
                writeType(context, type.type, "value")
            }
            addStatement("}")
        }
        is ValueType.CLASS -> {
            addStatement("%L.serializeToStream($StreamName)", name)
        }
    }
}

private fun ValueType.asKtType(context: GenerateContext): TypeName = when (this) {
    ValueType.STRING -> STRING
    is ValueType.BOOLEAN -> BOOLEAN
    ValueType.INT -> INT
    ValueType.LONG -> LONG
    is ValueType.VECTOR -> LIST.parameterizedBy(type.asKtType(context))
    is ValueType.CLASS -> ClassName(context.packageName, generateClassName(context.prefix, name))
}

private fun ValueToken.defaultValue(context: GenerateContext): String {
    return when (val type = type) {
        ValueType.STRING -> "\"\""
        ValueType.INT -> "0"
        ValueType.LONG -> "0"
        is ValueType.BOOLEAN -> type.value.toString()
        is ValueType.VECTOR -> "emptyList()"
        is ValueType.CLASS -> "${generateClassName(context.prefix, type.name)}()"
    }
}
