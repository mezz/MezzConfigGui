import org.slf4j.event.Level

plugins {
    java
    idea
    `maven-publish`
    id("net.neoforged.moddev.legacyforge")
    id("me.modmuss50.mod-publish-plugin")
}

val minecraftVersion: String by extra
val forgeVersion: String by extra
val modJavaVersion: String by extra
val configGuiModId: String by extra
val configModId: String by extra
val configModGroup: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigForgeDependency: String by rootProject.extra
val artifactName = "$configGuiModId-$minecraftVersion-forge"
group = configModGroup
base.archivesName.set(artifactName)

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { content { includeGroup("net.mezzdev.config") } }
}

val common = project(":Common")
evaluationDependsOn(common.path)
val commonSources = common.extensions.getByType<SourceSetContainer>()
val commonOutputs = listOf(commonSources["main"], commonSources["api"])
val smokeMod = sourceSets.create("serverSmokeTestMod") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations.named(smokeMod.implementationConfigurationName) { extendsFrom(configurations.implementation.get()) }
configurations.named(smokeMod.compileOnlyConfigurationName) { extendsFrom(configurations.compileOnly.get()) }
val mezzConfigRun = configurations.create("mezzConfigRun") {
    isCanBeConsumed = false
    isTransitive = false
}
val configRun = sourceSets.create("mezzConfigRun") {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
    val directory = layout.buildDirectory.dir("mezz-config-run")
    java.destinationDirectory.set(directory)
    output.setResourcesDir(directory)
}
val prepareMezzConfigRun = tasks.register("prepareMezzConfigRun") {
    dependsOn("createMinecraftArtifacts")
    outputs.dir(configRun.java.destinationDirectory)
    outputs.upToDateWhen { false }
    doLast {
        // Legacy ModDev's remapper requires the generated mapping archive to exist.
        // Resolve this custom configuration only after createMinecraftArtifacts has run.
        project.sync {
            from(mezzConfigRun.map { zipTree(it) })
            into(configRun.java.destinationDirectory)
        }
    }
}

tasks.named(configRun.classesTaskName) { dependsOn(prepareMezzConfigRun) }

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    withSourcesJar()
}
dependencies {
    compileOnly(project(path = common.path, configuration = "namedElements"))
    compileOnly(commonSources["api"].output)
    compileOnly(mezzConfigApiDependency)
    compileOnly("org.jetbrains:annotations:${project.extra["jetbrainsAnnotationsVersion"]}")
    compileOnly("com.google.code.findbugs:jsr305:${project.extra["jsr305Version"]}")
}

val smokeDirectory = layout.buildDirectory.dir("run/server-smoke")
val smokeResult = smokeDirectory.map { it.file("smoke-test-passed") }
val targetForgeArtifact = "$minecraftVersion-$forgeVersion"
legacyForge {
    enable {
        setForgeVersion(targetForgeArtifact)
        setEnabledSourceSets(setOf(sourceSets.main.get(), smokeMod))
    }
    mods {
        create(configModId) { sourceSet(configRun) }
        create(configGuiModId) {
            sourceSet(sourceSets.main.get())
            commonOutputs.forEach { sourceSet(it) }
        }
        create("mezz_config_gui_test_forge_smoke") { sourceSet(smokeMod) }
    }
    runs {
        create("client") {
            client()
            gameDirectory = file("run/client")
        }
        create("server") {
            server()
            gameDirectory = file("run/server")
            programArguments.add("nogui")
        }
        create("serverSmokeTest") {
            server()
            gameDirectory = smokeDirectory.get().asFile
            programArguments.add("nogui")
            systemProperty("com.mojang.eula.agree", "true")
            systemProperty("mezzConfigGui.loaderSmokeTest.successFile", smokeResult.get().asFile.absolutePath)
            logLevel = Level.INFO
        }
    }
}

val remappedMezzConfig = obfuscation.createRemappingConfiguration(mezzConfigRun)
dependencies { add(remappedMezzConfig.name, mezzConfigForgeDependency) }

extra["configLanguageDependencyProjects"] = listOf(common)
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))
val languages = tasks.named("mergeConfigLanguageResources")
tasks.jar {
    from(commonOutputs.map { it.output }) { exclude("assets/mezz_config/lang/*.json") }
    from(languages)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<Jar>("sourcesJar") {
    from(commonOutputs.map { it.allJava })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
val runtimeJar = tasks.named<AbstractArchiveTask>("reobfJar")
publishMods { file.set(runtimeJar.flatMap { it.archiveFile }) }
publishing {
    publications {
        register<MavenPublication>("configGuiForgeJar") {
            artifactId = artifactName
            artifact(runtimeJar)
            artifact(tasks.named("sourcesJar"))
            pom.withXml {
                val dependency = asNode().appendNode("dependencies").appendNode("dependency")
                val (group, artifact, version) = mezzConfigForgeDependency.split(":")
                dependency.appendNode("groupId", group)
                dependency.appendNode("artifactId", artifact)
                dependency.appendNode("version", version)
            }
        }
    }
}
tasks.matching { it.name in setOf("runClient", "runServer", "runServerSmokeTest") }.configureEach {
    dependsOn(prepareMezzConfigRun, tasks.named(smokeMod.classesTaskName))
}
tasks.named("runServerSmokeTest") {
    outputs.file(smokeResult)
    outputs.upToDateWhen { false }
    doFirst {
        val result = outputs.files.singleFile
        result.parentFile.mkdirs()
        result.resolveSibling("eula.txt").writeText("eula=true\n")
        result.resolveSibling("server.properties").writeText("online-mode=false\nserver-port=0\n")
        result.delete()
    }
    doLast {
        check(outputs.files.singleFile.isFile) { "The Forge loader smoke test did not report success." }
    }
}
tasks.check { dependsOn(tasks.named(smokeMod.classesTaskName), tasks.named("runServerSmokeTest")) }

idea.module {
    for (directory in listOf("build", "run", "out", "logs")) excludeDirs.add(file(directory))
}
