plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("configLanguageResources") {
            id = "net.mezzdev.config-language-resources"
            implementationClass = "net.mezzdev.config.gradle.ConfigLanguageResourcesPlugin"
        }
    }
}
