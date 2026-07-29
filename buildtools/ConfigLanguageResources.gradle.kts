import groovy.json.JsonOutput
import groovy.json.JsonSlurper

@Suppress("UNCHECKED_CAST")
val configLanguageDependencyProjects = extensions.extraProperties.get("configLanguageDependencyProjects") as List<Project>

val configLanguageResourceProjects = configLanguageDependencyProjects.filter {
	it.layout.projectDirectory.file("src/main/resources/assets/mezz_config/lang/en_us.json").asFile.isFile
}
extensions.extraProperties.set("configLanguageResourceProjects", configLanguageResourceProjects)

tasks.register("mergeConfigLanguageResources") {
	val configLanguageFiles = configLanguageResourceProjects.map {
		it.layout.projectDirectory.file("src/main/resources/assets/mezz_config/lang/en_us.json")
	}
	val outputDirectory = layout.buildDirectory.dir("generated/resources/mergedConfigLang")
	inputs.files(configLanguageFiles)
	outputs.dir(outputDirectory)
	doLast {
		val mergedTranslations = linkedMapOf<String, String>()
		val jsonSlurper = JsonSlurper()
		for (languageFileProvider in configLanguageFiles) {
			val languageFile = languageFileProvider.asFile
			if (!languageFile.isFile) {
				continue
			}
			val translations = jsonSlurper.parse(languageFile) as Map<*, *>
			for ((key, value) in translations) {
				if (key !is String || value !is String) {
					throw GradleException("Invalid config language entry in ${languageFile}: $key=$value")
				}
				val previousValue = mergedTranslations.put(key, value)
				if (previousValue != null && previousValue != value) {
					throw GradleException("Conflicting config language entry for key '$key'")
				}
			}
		}

		val outputFile = outputDirectory.get().file("assets/mezz_config/lang/en_us.json").asFile
		outputFile.parentFile.mkdirs()
		outputFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mergedTranslations)) + "\n")
	}
}
