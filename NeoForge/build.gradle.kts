import org.slf4j.event.Level

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("net.neoforged.moddev")
}

// gradle.properties
val neoforgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigApiNeoForgeDependency: String by rootProject.extra
val mezzConfigApiCompileOnlyProject: Project = project(":MezzConfigApiCompileOnly")
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

(dependencyProjects + testModProjects + mezzConfigApiCompileOnlyProject).forEach {
    project.evaluationDependsOn(it.path)
}
val testModSourceSets = testModProjects.map {
    it.sourceSets.main.get()
}

extra["configLanguageDependencyProjects"] = dependencyProjects
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val configLanguageResourceProjects = extra["configLanguageResourceProjects"] as List<Project>
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
            getMods().set(setOf(
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
    }
}

val testModClassesTasks = testModProjects.mapIndexed { index, testModProject ->
    testModProject.tasks.named(testModSourceSets[index].classesTaskName)
}
tasks.matching { it.name == "runClient" }.configureEach {
    dependsOn(testModClassesTasks)
}

sourceSets {
    named("test") {
        //The test module has no resources
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly(mezzConfigApiCompileOnlyProject)
    runtimeOnly(mezzConfigApiNeoForgeDependency)
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
    dependsOn(mergedConfigLanguageResources)
    for (p in configLanguageResourceProjects) {
        from(p.sourceSets.main.get().resources) {
            exclude("assets/mezz_config/lang/*.json")
        }
    }
    from(mergedConfigLanguageResources)
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

tasks.assemble {
    dependsOn(sourcesJarTask)
}

publishing {
    publications {
        register<MavenPublication>("configGuiNeoForgeJar") {
            artifactId = baseArchivesName
            artifact(tasks.jar.get())
            artifact(sourcesJarTask.get())

            val dependencyInfos = listOf(dependencyInfo(mezzConfigApiNeoForgeDependency)) + dependencyProjects.map {
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
