import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact
import net.minecraftforge.gradle.common.tasks.JarExec

plugins {
	id("java")
	id("idea")
	id("eclipse")
	id("maven-publish")
	id("net.minecraftforge.gradle")
	id("me.modmuss50.mod-publish-plugin")
}

publishMods {
	file.set(tasks.jar.flatMap { it.archiveFile })
}

// gradle.properties
val forgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val configModId: String by extra
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
val jeiApiDependency: String by rootProject.extra
val forgeServerSmokeTestModId = "mezz_config_gui_test_forge_smoke"
group = configModGroup

val baseArchivesName = "${configGuiModId}-${minecraftVersion}-forge"
base {
	archivesName.set(baseArchivesName)
}

val commonProject: Project = project(":Common")
val dependencyProjects: List<Project> = listOf(commonProject)
dependencyProjects.forEach {
	project.evaluationDependsOn(it.path)
}
val commonApiSourceSet = commonProject.sourceSets["api"]
val dependencySourceSets = listOf(commonProject.sourceSets.main.get(), commonApiSourceSet)
val mezzConfigRun by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	isTransitive = false
}
val mezzConfigRunSourceSet = sourceSets.create("mezzConfigRun") {
	java.setSrcDirs(emptyList<String>())
	resources.setSrcDirs(emptyList<String>())
}
val configGuiRunSourceSet = sourceSets.create("configGuiRun") {
	java.setSrcDirs(emptyList<String>())
	resources.setSrcDirs(emptyList<String>())
}
val serverSmokeTestModSourceSet = sourceSets.create("serverSmokeTestMod") {
	compileClasspath += sourceSets.main.get().output
	compileClasspath += sourceSets.main.get().compileClasspath
}
configurations.named(serverSmokeTestModSourceSet.runtimeClasspathConfigurationName) {
	extendsFrom(configurations.runtimeClasspath.get())
}
val serverSmokeTestRunDir = layout.buildDirectory.dir("run/server-smoke")
val serverSmokeTestSuccessFile = serverSmokeTestRunDir.map { it.file("smoke-test-passed") }

extra["configLanguageDependencyProjects"] = dependencyProjects
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))

val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")

val prepareMezzConfigRun = tasks.register<Sync>("prepareMezzConfigRun") {
	from(mezzConfigRun.map { zipTree(it) })
	into(mezzConfigRunSourceSet.java.destinationDirectory)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val mezzConfigRunClassesTask = tasks.named(mezzConfigRunSourceSet.classesTaskName) {
	dependsOn(prepareMezzConfigRun)
}

val prepareConfigGuiRun = tasks.register<Sync>("prepareConfigGuiRun") {
	from(sourceSets.main.get().output)
	for (dependencySourceSet in dependencySourceSets) {
		from(dependencySourceSet.output)
	}
	into(configGuiRunSourceSet.java.destinationDirectory)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val configGuiRunClassesTask = tasks.named(configGuiRunSourceSet.classesTaskName) {
	dependsOn(prepareConfigGuiRun)
}

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

// Hack fix: FG can't resolve deps like lwjgl-freetype-3.3.3-natives-macos-patch.jar without this
repositories {
	val deployDir = rootProject.findProperty("DEPLOY_DIR")
	if (deployDir != null) {
        maven(deployDir) {
            content {
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
	mezzConfigRun(mezzConfigForgeDependency)
	dependencyProjects.forEach {
		compileOnly(it)
	}
	compileOnly(commonApiSourceSet.output)
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
				create(configModId) {
					source(mezzConfigRunSourceSet)
				}
				create(configGuiModId) {
					source(configGuiRunSourceSet)
				}
				create(forgeServerSmokeTestModId) {
					source(serverSmokeTestModSourceSet)
				}
			}
		}
		val server = create("server") {
			taskName("Server")
			property("forge.logging.console.level", "debug")
			workingDirectory(file("run/server"))
			mods {
				create(configModId) {
					source(mezzConfigRunSourceSet)
				}
				create(configGuiModId) {
					source(configGuiRunSourceSet)
				}
				create(forgeServerSmokeTestModId) {
					source(serverSmokeTestModSourceSet)
				}
			}
		}
		create("serverSmokeTest") {
			parent(server)
			taskName("runServerSmokeTest")
			property("forge.logging.console.level", "info")
			property("com.mojang.eula.agree", "true")
			property("mezzConfigGui.loaderSmokeTest.successFile", serverSmokeTestSuccessFile.get().asFile.absolutePath)
			args("--nogui")
			workingDirectory(serverSmokeTestRunDir.get().asFile)
		}
	}
}

val serverSmokeTestModClassesTask = tasks.named(serverSmokeTestModSourceSet.classesTaskName)
val serverSmokeTestRunTasks = setOf("runClientDev", "Server", "runServerSmokeTest")
tasks.matching { it.name in serverSmokeTestRunTasks }.configureEach {
	dependsOn(mezzConfigRunClassesTask, configGuiRunClassesTask, serverSmokeTestModClassesTask)
}

val runServerSmokeTestTasks = tasks.matching { it.name == "runServerSmokeTest" }
runServerSmokeTestTasks.configureEach {
	notCompatibleWithConfigurationCache("ForgeGradle run tasks cannot be serialized by the configuration cache")
	doNotTrackState("ForgeGradle run configurations are not serializable")
	outputs.file(serverSmokeTestSuccessFile)
	outputs.upToDateWhen { false }
	doFirst {
		val successFile = outputs.files.singleFile
		successFile.parentFile.mkdirs()
		successFile.resolveSibling("eula.txt").writeText("eula=true\n")
		successFile.resolveSibling("server.properties").writeText("online-mode=false\nserver-port=0\n")
		successFile.delete()
	}
	doLast {
		if (!outputs.files.singleFile.isFile) {
			throw GradleException("The Forge loader smoke test did not report success.")
		}
	}
}

tasks.check {
	dependsOn(runServerSmokeTestTasks)
}

tasks.jar {
	dependsOn(mergedConfigLanguageResources)
	from(sourceSets.main.get().output)
	for (dependencySourceSet in dependencySourceSets) {
		from(dependencySourceSet.output) {
			exclude("assets/mezz_config/lang/*.json")
		}
	}
	from(mergedConfigLanguageResources)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
	from(sourceSets.main.get().allJava)
	for (dependencySourceSet in dependencySourceSets) {
		from(dependencySourceSet.allJava)
	}
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveClassifier.set("sources")
}

val mavenJarTask = tasks.register<Jar>("mavenJar") {
	from(sourceSets.main.get().output)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	destinationDirectory.set(layout.buildDirectory.dir("maven-libs"))
}

val mavenSourcesJarTask = tasks.register<Jar>("mavenSourcesJar") {
	from(sourceSets.main.get().allJava)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveClassifier.set("sources")
	destinationDirectory.set(layout.buildDirectory.dir("maven-libs"))
}

tasks.assemble {
	dependsOn(sourcesJarTask)
}

publishing {
	publications {
		register<MavenPublication>("configGuiForgeJar") {
			artifactId = baseArchivesName
			artifact(mavenJarTask.get())
			artifact(mavenSourcesJarTask.get())

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
