import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact
import net.minecraftforge.gradle.common.tasks.JarExec

plugins {
	id("java")
	id("idea")
	id("eclipse")
	id("maven-publish")
	id("net.minecraftforge.gradle")
}

// gradle.properties
val forgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val mixinVersion: String by extra
val guavaVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val log4jVersion: String by extra
val fastutilVersion: String by extra
val jsr305Version: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigForgeDependency: String by rootProject.extra
val jeiApiDependency: Any by rootProject.extra
val configGuiApiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui-api")
val configGuiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui")

group = configModGroup

val baseArchivesName = "${configGuiModId}-${minecraftVersion}-forge"
base {
	archivesName.set(baseArchivesName)
}

val dependencyProjects: List<Project> = listOf(
	configGuiApiProject,
	configGuiProject,
)

(dependencyProjects).forEach {
	project.evaluationDependsOn(it.path)
}

extra["configLanguageDependencyProjects"] = dependencyProjects
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val configLanguageResourceProjects = extra["configLanguageResourceProjects"] as List<Project>
val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")

sourceSets {
	named("test") {
		//The test module has no resources
		resources.setSrcDirs(emptyList<String>())
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
	}
	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	javaToolchains {
		compilerFor {
			languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
		}
	}
}

tasks.named<JavaCompile>(sourceSets.main.get().compileJavaTaskName) {
	dependencyProjects.forEach {
		source(it.sourceSets.main.get().allSource)
	}
}

// Hack fix: FG can't resolve deps like lwjgl-freetype-3.3.3-natives-macos-patch.jar without this
repositories {
	val deployDir = rootProject.findProperty("DEPLOY_DIR")
	if (deployDir != null) {
		maven(deployDir) {
			content {
				includeGroup("mezz.jei")
				includeGroup("net.mezzdev.config")
			}
		}
	}
	maven("https://libraries.minecraft.net")
	mavenCentral()
	maven("https://maven.blamejared.com") {
		content {
			includeGroup("mezz.jei")
			includeGroup("net.mezzdev.config")
		}
	}
	mavenLocal {
		content {
			includeGroup("mezz.jei")
			includeGroup("net.mezzdev.config")
		}
	}
}

dependencies {
	"minecraft"(
		group = "net.minecraftforge",
		name = "forge",
		version = "${minecraftVersion}-${forgeVersion}"
	)
	compileOnly("org.spongepowered:mixin:$mixinVersion")
	compileOnly("com.google.guava:guava:$guavaVersion")
	compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
	compileOnly("org.apache.logging.log4j:log4j-api:$log4jVersion")
	compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")
	compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
	compileOnly(jeiApiDependency)
	compileOnly(mezzConfigApiDependency)
	runtimeOnly(mezzConfigForgeDependency)
	dependencyProjects.forEach {
		compileOnly(it)
	}
}

minecraft {
	mappings("official", minecraftVersion)

	// use Official mappings at runtime
	reobf = false

	copyIdeResources.set(true)

	accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

	runs {
		create("client") {
			taskName("runClientDev")
			property("forge.logging.console.level", "debug")
			workingDirectory(file("run/client/Dev"))
			mods {
				create(configGuiModId) {
					source(sourceSets.main.get())
				}
			}
		}
		create("server") {
			taskName("Server")
			property("forge.logging.console.level", "debug")
			workingDirectory(file("run/server"))
			mods {
				create(configGuiModId) {
					source(sourceSets.main.get())
				}
			}
		}
	}
}

tasks.named<ProcessResources>(sourceSets.main.get().processResourcesTaskName) {
	dependsOn(mergedConfigLanguageResources)
	for (p in configLanguageResourceProjects) {
		from(p.sourceSets.main.get().resources) {
			exclude("assets/mezz_config/lang/*.json")
		}
	}
	from(mergedConfigLanguageResources)
}

tasks.jar {
	dependsOn(mergedConfigLanguageResources)
	from(sourceSets.main.get().output)
	from(mergedConfigLanguageResources)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
	from(sourceSets.main.get().allJava)
	for (p in dependencyProjects) {
		from(p.sourceSets.main.get().allJava)
	}
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveClassifier.set("sources")
}

tasks.assemble {
	dependsOn(sourcesJarTask)
}

publishing {
	publications {
		register<MavenPublication>("configGuiForgeJar") {
			artifactId = baseArchivesName
			artifact(tasks.jar.get())
			artifact(sourcesJarTask.get())

			val dependencyInfos = listOf(dependencyInfo(mezzConfigForgeDependency)) + dependencyProjects.map {
				mapOf(
					"groupId" to it.group,
					"artifactId" to it.base.archivesName.get(),
					"version" to it.version
				)
			}

			pom.withXml {
				val dependenciesNode = asNode().appendNode("dependencies")
				dependencyInfos.forEach {
					val dependencyNode = dependenciesNode.appendNode("dependency")
					it.forEach { (key, value) ->
						dependencyNode.appendNode(key, value)
					}
				}
			}
		}
	}
	repositories {
		val deployDir = project.findProperty("DEPLOY_DIR")
		if (deployDir != null) {
			maven(deployDir)
		}
	}
}

fun dependencyInfo(notation: String): Map<String, String> {
	val (groupId, artifactId, version) = notation.split(":")
	return mapOf(
		"groupId" to groupId,
		"artifactId" to artifactId,
		"version" to version
	)
}

idea {
	module {
		for (fileName in listOf("build", "run", "out", "logs")) {
			excludeDirs.add(file(fileName))
		}
	}
}

// Required because FG, copied from the MDK
sourceSets.forEach {
	val outputDir = layout.buildDirectory.file("sourcesSets/${it.name}").get().asFile
	it.output.setResourcesDir(outputDir)
	it.java.destinationDirectory.set(outputDir)
}

tasks.withType<DownloadMavenArtifact> {
	notCompatibleWithConfigurationCache("uses Task.project at execution time")
}

tasks.withType<JarExec> {
	notCompatibleWithConfigurationCache("uses external process at execution time")
}
