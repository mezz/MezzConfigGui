import org.slf4j.event.Level
import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ValidateGameTestResult : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val logFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val file = logFile.get().asFile
        if (!Regex("All [1-9][0-9]* required tests passed").containsMatchIn(file.readText())) {
            throw GradleException("GameTest server did not report that all required tests passed; see ${file.path}")
        }
    }
}

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

publishMods {
    file.set(tasks.jar.flatMap { it.archiveFile })
}

repositories {
    mavenCentral()
    val deployDir = rootProject.findProperty("DEPLOY_DIR")
    if (deployDir != null) {
        maven(deployDir) {
            content {
                includeGroup("net.mezzdev.config")
            }
        }
    }
    maven("https://maven.blamejared.com") {
        content {
            includeGroup("net.mezzdev.config")
        }
    }
    mavenLocal {
        content {
            includeGroup("net.mezzdev.config")
        }
    }
}

// gradle.properties
val neoforgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val configModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val jsr305Version: String by extra
val jUnitVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigNeoForgeDependency: String by rootProject.extra
val neoForgeNativeDefaultsTestModId = "mezz_config_gui_test_neoforge_defaults"
val neoForgeNativeCustomTestModId = "mezz_config_gui_test_neoforge_custom"

group = configModGroup

val baseArchivesName = "${configGuiModId}-${minecraftVersion}-neoforge"
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
val mezzConfigRunOutput = layout.buildDirectory.dir("sourceSets/mezzConfigRun")
val mezzConfigRunSourceSet = sourceSets.create("mezzConfigRun") {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
    java.destinationDirectory.set(mezzConfigRunOutput)
    output.setResourcesDir(mezzConfigRunOutput)
}
val prepareMezzConfigRun = tasks.register<Sync>("prepareMezzConfigRun") {
    from(mezzConfigRun.map { zipTree(it) })
    into(mezzConfigRunOutput)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named(mezzConfigRunSourceSet.compileJavaTaskName) {
    enabled = false
}
tasks.named(mezzConfigRunSourceSet.processResourcesTaskName) {
    enabled = false
}
val mezzConfigRunClassesTask = tasks.named(mezzConfigRunSourceSet.classesTaskName) {
    dependsOn(prepareMezzConfigRun)
}
val defaultsTestModSourceSet = sourceSets.create("defaultsTestMod") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
val customTestModSourceSet = sourceSets.create("customTestMod") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
val testModSourceSets = listOf(defaultsTestModSourceSet, customTestModSourceSet)

for (testModSourceSet in testModSourceSets) {
    configurations.named(testModSourceSet.implementationConfigurationName) {
        extendsFrom(configurations.implementation.get())
    }
    configurations.named(testModSourceSet.compileOnlyConfigurationName) {
        extendsFrom(configurations.compileOnly.get())
    }
}

extra["configLanguageDependencyProjects"] = dependencyProjects
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))

val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")
val configGuiAccessTransformer = commonProject.layout.projectDirectory.file("src/main/accesstransformer.cfg")

neoForge {
    version = neoforgeVersion
    addModdingDependenciesTo(sourceSets.test.get())
    accessTransformers {
        from(configGuiAccessTransformer)
    }

    for (testModSourceSet in testModSourceSets) {
        addModdingDependenciesTo(testModSourceSet)
    }

    mods {
        create(configModId) {
            sourceSet(mezzConfigRunSourceSet)
        }
        create(configGuiModId) {
            sourceSet(sourceSets.main.get())
            for (dependencySourceSet in dependencySourceSets) {
                sourceSet(dependencySourceSet)
            }
        }
        create(neoForgeNativeDefaultsTestModId) {
            sourceSet(defaultsTestModSourceSet)
        }
        create(neoForgeNativeCustomTestModId) {
            sourceSet(customTestModSourceSet)
        }
    }

    runs {
        val configMod = mods.named(configModId)
        val configGuiMod = mods.named(configGuiModId)
        val neoForgeNativeDefaultsTestMod = mods.named(neoForgeNativeDefaultsTestModId)
        val neoForgeNativeCustomTestMod = mods.named(neoForgeNativeCustomTestModId)

        configureEach {
            getLoadedMods().set(setOf(
                configMod.get(),
                configGuiMod.get(),
                neoForgeNativeDefaultsTestMod.get(),
                neoForgeNativeCustomTestMod.get()
            ))
        }
        create("client") {
            client()
            gameDirectory = file("run/client")
            logLevel = Level.DEBUG
        }
        create("server") {
            server()
            gameDirectory = file("run/server")
            programArguments.addAll("nogui")
            logLevel = Level.INFO
        }
        create("gameTestServer") {
            getType().set("gameTestServer")
            gameDirectory = file("run/gameTestServer")
            logLevel = Level.INFO
        }
    }
}

val testModClassesTasks = testModSourceSets.map {
    tasks.named(it.classesTaskName)
}
val validateGameTestResult = tasks.register<ValidateGameTestResult>("validateGameTestResult") {
    group = "verification"
    description = "Checks that the NeoForge GameTest server ran and passed at least one required test."
    logFile.set(layout.projectDirectory.file("run/gameTestServer/logs/latest.log"))
}
tasks.matching { it.name == "runClient" }.configureEach {
    dependsOn(mezzConfigRunClassesTask, testModClassesTasks)
}
val runGameTestServerTasks = tasks.matching { it.name == "runGameTestServer" }
runGameTestServerTasks.configureEach {
    dependsOn(mezzConfigRunClassesTask, testModClassesTasks)
    finalizedBy(validateGameTestResult)
}

tasks.check {
    dependsOn(testModClassesTasks, runGameTestServerTasks)
}

sourceSets {
    named("test") {
        //The test module has no resources
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {

    compileOnly(mezzConfigApiDependency)
    mezzConfigRun(mezzConfigNeoForgeDependency)
    dependencyProjects.forEach {
        implementation(it)
    }
    compileOnly(commonApiSourceSet.output)
    testImplementation(commonApiSourceSet.output)
    testImplementation(mezzConfigApiDependency)
    testImplementation("org.junit.jupiter:junit-jupiter:$jUnitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    for (testModSourceSet in testModSourceSets) {
        add(testModSourceSet.compileOnlyConfigurationName, "com.google.code.findbugs:jsr305:$jsr305Version")
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    javaToolchains {
        compilerFor {
            languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
        }
    }
}

tasks.named<ProcessResources>(sourceSets.main.get().processResourcesTaskName) {
    from(configGuiAccessTransformer) {
        into("META-INF")
        rename { "accesstransformer.cfg" }
    }
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
    from(configGuiAccessTransformer) {
        into("META-INF")
        rename { "accesstransformer.cfg" }
    }
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

tasks.assemble {
    dependsOn(sourcesJarTask)
}

publishing {
    publications {
        register<MavenPublication>("configGuiNeoForgeJar") {
            artifactId = baseArchivesName
            // NeoForge must discover common/API classes in the mod's game-layer jar.
            // A transitive plain common jar is not a loadable NeoForge mod or game library.
            artifact(tasks.jar)
            artifact(sourcesJarTask)

            val dependencyInfos = listOf(dependencyInfo(mezzConfigNeoForgeDependency))

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
