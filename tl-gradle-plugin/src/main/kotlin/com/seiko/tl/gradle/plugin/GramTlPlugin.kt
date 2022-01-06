package com.seiko.tl.gradle.plugin

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.register


class GramTlPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create("gramTl", GramTlExtension::class.java)

        val generatedKtDir = layout.buildDirectory.dir(extension.generatedCodesDir)

        // set generated sources root
        (extensions.getByName("android") as? CommonExtension<*, *, *, *, *>)?.sourceSets {
            getByName(SourceSet.MAIN_SOURCE_SET_NAME).kotlin.srcDir(generatedKtDir)
        }

        val genTask by tasks.register<GenerateGramTlTask>("generateGramTl") {
            group = "gram tl"
            gramTlDir.set(layout.projectDirectory.dir(extension.tlSourceDir))
            codeDir.set(generatedKtDir)
            packageName.set(extension.packageName)
            prefix.set(extension.prefix)
            listMagicNumber.set(extension.listMagicNumber)
        }
        tasks.configureEach { task ->
            if (
                task.name != "compileKotlin" &&
                task.name.startsWith("compile") &&
                task.name.endsWith("Kotlin")
            ) {
                task.dependsOn(genTask)
            }
        }
    }
}
