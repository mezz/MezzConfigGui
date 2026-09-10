import org.slf4j.event.Level

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("net.neoforged.moddev")
}

repositories {
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
val configModGroup: String by extra
val modJavaVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigNeoForgeDependency: String by rootProject.extra
val configGuiApiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui-api")
val configGuiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui")
val neoForgeNativeDefaultsTestModId = "mezz_config_gui_test_neoforge_defaults"
val neoForgeNativeCustomTestModId = "mezz_config_gui_test_neoforge_custom"
val neoForgeNativeDefaultsTestModProject: Project = project(":NeoForgeNativeDefaultsTest")
val neoForgeNativeCustomTestModProject: Project = project(":NeoForgeNativeCustomTest")

group = configModGroup

val baseArchivesName = "${configGuiModId}-${minecraftVersion}-neoforge"
base {
    archivesName.set(baseArchivesName)
}

val dependencyProjects: List<Project> = listOf(
    configGuiApiProject,
    configGuiProject,
)
val testModProjects: List<Project> = listOf(
    neoForgeNativeDefaultsTestModProject,
    neoForgeNativeCustomTestModProject,
)

(dependencyProjects + testModProjects).forEach {
    project.evaluationDependsOn(it.path)
}
val testModSourceSets = testModProjects.map {
    it.sourceSets.main.get()
}

extra["configLanguageDependencyProjects"] = dependencyProjects
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))

val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")
val configGuiAccessTransformer = configGuiProject.layout.projectDirectory.file("src/main/accesstransformer.cfg")

neoForge {
    version = neoforgeVersion
    accessTransformers {
        from(configGuiAccessTransformer)
    }

    mods {
        create(configGuiModId) {
            sourceSet(sourceSets.main.get())
            for (dependencyProject in dependencyProjects) {
                sourceSet(dependencyProject.sourceSets.main.get())
            }
        }
        create(neoForgeNativeDefaultsTestModId) {
            sourceSet(testModSourceSets[0])
        }
        create(neoForgeNativeCustomTestModId) {
            sourceSet(testModSourceSets[1])
        }
    }

    runs {
        val configGuiMod = mods.named(configGuiModId)
        val neoForgeNativeDefaultsTestMod = mods.named(neoForgeNativeDefaultsTestModId)
        val neoForgeNativeCustomTestMod = mods.named(neoForgeNativeCustomTestModId)

        configureEach {
            getLoadedMods().set(setOf(
                configGuiMod.get(),
                neoForgeNativeDefaultsTestMod.get(),
                neoForgeNativeCustomTestMod.get()
            ))
        }
        create("client") {
            client()
            gameDirectory = file("run/client/Dev")
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

val testModClassesTasks = testModProjects.mapIndexed { index, testModProject ->
    testModProject.tasks.named(testModSourceSets[index].classesTaskName)
}
tasks.matching { it.name == "runClient" }.configureEach {
    dependsOn(testModClassesTasks)
}
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    dependsOn(testModClassesTasks)
}

sourceSets {
    named("test") {
        //The test module has no resources
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly(mezzConfigApiDependency)
    runtimeOnly(mezzConfigNeoForgeDependency)
    dependencyProjects.forEach {
        implementation(it)
    }
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
    for (p in dependencyProjects) {
        from(p.sourceSets.main.get().output) {
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
    for (p in dependencyProjects) {
        from(p.sourceSets.main.get().allJava)
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
        register<MavenPublication>("configGuiNeoForgeJar") {
            artifactId = baseArchivesName
            artifact(mavenJarTask.get())
            artifact(mavenSourcesJarTask.get())

            val dependencyInfos = listOf(dependencyInfo(mezzConfigNeoForgeDependency)) + dependencyProjects.map {
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
