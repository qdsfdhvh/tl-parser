plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.gradle.plugin.android.api)
    implementation(libs.kotlin.poet)
    implementation(project(":tl-parser"))
}

gradlePlugin {
    plugins {
        create("tl-generator") {
            id = "io.github.qdsfdhvh.tl-generator"
            implementationClass = "com.seiko.tl.gradle.plugin.GramTlPlugin"
        }
    }
}
