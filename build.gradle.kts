import com.vanniktech.maven.publish.SonatypeHost

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/")
        ktlint(libs.versions.ktlint.get())
    }
}

allprojects {
    group = "io.github.qdsfdhvh"
    version = "0.0.4"

    plugins.withId("com.vanniktech.maven.publish.base") {
        mavenPublishing {
            publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
            signAllPublications()
            @Suppress("UnstableApiUsage")
            pom {
                name.set("ti-parser")
                description.set("gram-js tl parser for kotlin multiplatform.")
                url.set("https://github.com/cybernhl/tl-parser")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("Seiko")
                        name.set("SeikoDes")
                        email.set("seiko_des@outlook.com")
                    }
                }
                scm {
                    url.set("https://github.com/cybernhl/tl-parser")
                    connection.set("scm:git:git://github.com/cybernhl/tl-parser.git")
                    developerConnection.set("scm:git:git://github.com/cybernhl/tl-parser.git")
                }
            }
        }
    }
}
