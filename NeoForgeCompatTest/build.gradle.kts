import org.slf4j.event.Level
import java.time.Duration

plugins {
    java
    idea
    id("net.neoforged.moddev.legacyforge")
}

val minecraftVersion: String by extra
val modJavaVersion: String by extra
val configGuiModId: String by extra
val configModId: String by extra
val neoForgeCompatVersion: String by extra
val mezzConfigForgeDependency: String by rootProject.extra
val nativeTestModId = "mezz_config_gui_test_forge_native"

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { content { includeGroup("net.mezzdev.config") } }
}

val forge = project(":Forge")
evaluationDependsOn(forge.path)
val forgeSourceSets = forge.extensions.getByType<SourceSetContainer>()
val fixture = forgeSourceSets["nativeTestMod"]

fun binaryModSourceSet(name: String) = sourceSets.maybeCreate(name).apply {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
    val directory = layout.buildDirectory.dir("binary-mods/$name")
    java.destinationDirectory.set(directory)
    output.setResourcesDir(directory)
    tasks.named(compileJavaTaskName) { enabled = false }
    tasks.named(processResourcesTaskName) { enabled = false }
}

// Load the Forge-built jar and fixture unchanged instead of recompiling them against NeoForge.
val guiMod = binaryModSourceSet("main")
val configMod = binaryModSourceSet("mezzConfigRun")
val forgeJar = forge.tasks.named<Jar>("jar")
val prepareGuiMod = tasks.register<Sync>("prepareForgeGuiMod") {
    from(forgeJar.flatMap { it.archiveFile }.map { zipTree(it) })
    into(guiMod.java.destinationDirectory)
}
tasks.named(guiMod.classesTaskName) { dependsOn(prepareGuiMod) }

val mezzConfigRun = configurations.create("mezzConfigRun") {
    isCanBeConsumed = false
    isTransitive = false
}
val prepareConfigMod = tasks.register("prepareMezzConfigRun") {
    dependsOn("createMinecraftArtifacts")
    outputs.dir(configMod.java.destinationDirectory)
    outputs.upToDateWhen { false }
    doLast {
        // Resolve the remapped Forge dependency only after NeoForge's mappings have been generated.
        project.sync {
            from(mezzConfigRun.map { zipTree(it) })
            into(configMod.java.destinationDirectory)
        }
    }
}
tasks.named(configMod.classesTaskName) { dependsOn(prepareConfigMod) }

val testDirectory = layout.buildDirectory.dir("run/native-config-test")
val testResult = testDirectory.map { it.file("native-config-test-passed") }
val targetNeoForgeArtifact = "$minecraftVersion-$neoForgeCompatVersion"
legacyForge {
    enable {
        setNeoForgeVersion(targetNeoForgeArtifact)
        setDisableRecompilation(true)
        setObfuscateJar(false)
    }
    mods {
        create(configModId) { sourceSet(configMod) }
        create(configGuiModId) { sourceSet(guiMod) }
        create(nativeTestModId) { sourceSet(fixture) }
    }
    runs {
        create("nativeConfigTest") {
            client()
            gameDirectory = testDirectory.get().asFile
            systemProperty("mezzConfigGui.nativeTest.successFile", testResult.get().asFile.absolutePath)
            systemProperty("mezzConfigGui.nativeTest.expectedGroup", "net.neoforged")
            systemProperty("mezzConfigGui.nativeTest.expectedVersion", neoForgeCompatVersion)
            logLevel = Level.INFO
        }
    }
}

val remappedMezzConfig = obfuscation.createRemappingConfiguration(mezzConfigRun)
dependencies { add(remappedMezzConfig.name, mezzConfigForgeDependency) }

java.toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
tasks.jar { enabled = false }
tasks.named("runNativeConfigTest") {
    dependsOn(prepareGuiMod, prepareConfigMod, forge.tasks.named(fixture.classesTaskName))
    timeout.set(Duration.ofMinutes(5))
    outputs.file(testResult)
    outputs.upToDateWhen { false }
    doFirst {
        val directory = testDirectory.get().asFile
        directory.resolve("config").deleteRecursively()
        directory.resolve("options.txt").writeText("onboardAccessibility:false\n")
        testResult.get().asFile.delete()
    }
    doLast {
        check(testResult.get().asFile.isFile) { "The NeoForge compatibility client test did not report success." }
    }
}

idea.module {
    for (directory in listOf("build", "run", "out", "logs")) excludeDirs.add(file(directory))
}
