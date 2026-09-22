package net.mezzdev.config.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

private const val CONFIG_LANGUAGE_FILE = "src/main/resources/assets/mezz_config/lang/en_us.json"

class ConfigLanguageResourcesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val mergeTask = project.tasks.register(
            "mergeConfigLanguageResources",
            MergeConfigLanguageResources::class.java
        ) {
            outputDirectory.set(project.layout.buildDirectory.dir("generated/resources/mergedConfigLang"))
        }
        project.extensions.add(
            "configLanguageResources",
            ConfigLanguageResourcesExtension(mergeTask)
        )
    }
}

class ConfigLanguageResourcesExtension(
    private val mergeTask: TaskProvider<MergeConfigLanguageResources>
) {
    fun from(vararg sourceProjects: Project) {
        mergeTask.configure {
            languageFiles.from(sourceProjects.map {
                it.layout.projectDirectory.file(CONFIG_LANGUAGE_FILE)
            })
        }
    }
}

abstract class MergeConfigLanguageResources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val languageFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun merge() {
        val mergedTranslations = linkedMapOf<String, String>()
        val jsonSlurper = JsonSlurper()
        for (languageFile in languageFiles.files) {
            if (!languageFile.isFile) {
                continue
            }
            val translations = jsonSlurper.parse(languageFile) as Map<*, *>
            for ((key, value) in translations) {
                if (key !is String || value !is String) {
                    throw GradleException("Invalid config language entry in $languageFile: $key=$value")
                }
                val previousValue = mergedTranslations.put(key, value)
                if (previousValue != null && previousValue != value) {
                    throw GradleException("Conflicting config language entry for key '$key'")
                }
            }
        }

        val outputFile = outputDirectory.file("assets/mezz_config/lang/en_us.json").get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mergedTranslations)) + "\n")
    }
}
