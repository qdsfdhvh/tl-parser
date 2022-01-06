package com.seiko.tl.gradle.plugin

import com.seiko.tl.gradle.plugin.generator.GenerateContext
import com.seiko.tl.gradle.plugin.generator.generateKtFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateGramTlTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gramTlDir: DirectoryProperty

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val prefix: Property<String>

    @get:Input
    abstract val listMagicNumber: Property<String>

    @TaskAction
    fun generate() {
        println("gramTlDir=${gramTlDir.get()}")
        println("codeDir=${codeDir.get()}")

        try {
            val rootProtoDir = gramTlDir.get().asFile
            logger.info("Generate Gram TL for $rootProtoDir")

            // remove old code files
            codeDir.asFile.get().deleteRecursively()

            val context = GenerateContext(
                packageName = packageName.get(),
                prefix = prefix.get(),
                listMagicNumber = listMagicNumber.get(),
                currentClassName = "",
            )
            execProtoMetaInfo(context, rootProtoDir)
        } catch (e: Exception) {
            logger.error("e: GenerateGramTlTask was failed:", e)
        }
    }

    private fun execProtoMetaInfo(context: GenerateContext, file: File) {
        if (file.isFile && file.name.endsWith(".tl")) {
            generateKtFile(
                context = context,
                codeDir = codeDir.asFile.get(),
                tlFile = file,
            )
        } else if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                execProtoMetaInfo(context, child)
            }
        }
    }
}
