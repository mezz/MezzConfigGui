import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    idea
    `maven-publish`
    id("net.fabricmc.fabric-loom") apply false
    id("me.modmuss50.mod-publish-plugin")
    id("net.mezzdev.config-language-resources")
}

pluginManager.apply("net.fabricmc.fabric-loom")
val loom = extensions.getByType<LoomGradleExtensionAPI>()
val runtimeJar = tasks.named<AbstractArchiveTask>("jar")
publishMods { file.set(runtimeJar.flatMap { it.archiveFile }) }
val modDependencyConfiguration = "implementation"
val modCompileConfiguration = "compileOnly"
val modRuntimeConfiguration = "runtimeOnly"

repositories {
    val deployDir = rootProject.findProperty("DEPLOY_DIR")
    if (deployDir != null) {
        maven(deployDir) {
            content {
                includeGroup("net.mezzdev.config")
            }
        }
    }
    fun exclusiveMaven(url: String, filter: Action<InclusiveRepositoryContentDescriptor>) =
        exclusiveContent {
            forRepository { maven(url) }
            filter(filter)
        }
    exclusiveMaven("https://maven.parchmentmc.org") {
        includeGroupByRegex("org\\.parchmentmc.*")
    }
    maven("https://maven.terraformersmc.com/releases/") {
        // for optional Mod Menu integration
        content {
            includeGroup("com.terraformersmc")
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
val fabricApiVersion: String by extra
val fabricLoaderVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val configModId: String by extra
val modJavaVersion: String by extra
val modMenuVersionFabric: String by extra
val jsr305Version: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigFabricDependency: String by rootProject.extra
val fabricDefaultsTestModId = "mezz_config_gui_test_fabric_defaults"
val fabricCustomTestModId = "mezz_config_gui_test_fabric_custom"
val fabricServerSmokeTestModId = "mezz_config_gui_test_fabric_smoke"

group = configModGroup

val baseArchivesName = "${configGuiModId}-${minecraftVersion}-fabric"
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
val defaultsTestModSourceSet = sourceSets.create("defaultsTestMod") {
    compileClasspath += sourceSets.main.get().output
    compileClasspath += sourceSets.main.get().compileClasspath
}
val customTestModSourceSet = sourceSets.create("customTestMod") {
    compileClasspath += sourceSets.main.get().output
    compileClasspath += sourceSets.main.get().compileClasspath
}
val testModSourceSets = listOf(defaultsTestModSourceSet, customTestModSourceSet)
val serverSmokeTestModSourceSet = sourceSets.create("serverSmokeTestMod") {
    compileClasspath += sourceSets.main.get().output
    compileClasspath += sourceSets.main.get().compileClasspath
}
val serverSmokeTestRunDir = layout.buildDirectory.dir("run/server-smoke")
val serverSmokeTestSuccessFile = serverSmokeTestRunDir.map { it.file("smoke-test-passed") }

configLanguageResources {
    from(*dependencyProjects.toTypedArray())
}

val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")

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

dependencies {
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    add(modDependencyConfiguration, "net.fabricmc:fabric-loader:$fabricLoaderVersion")
    add(modDependencyConfiguration, "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    add(modCompileConfiguration, "com.terraformersmc:modmenu:$modMenuVersionFabric") { isTransitive = false }
    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    compileOnly(mezzConfigApiDependency)
    add(modRuntimeConfiguration, mezzConfigFabricDependency)
    add("localRuntime", "com.terraformersmc:modmenu:$modMenuVersionFabric") { isTransitive = false }
    dependencyProjects.forEach {
        implementation(it)
    }
    compileOnly(commonApiSourceSet.output)
}

val configGuiAccessWidener = commonProject.file("src/main/resources/mezz_config.accesswidener")

configure<LoomGradleExtensionAPI> {
    mods {
        create(configGuiModId) {
            sourceSet(sourceSets.main.get())
            for (dependencySourceSet in dependencySourceSets) {
                sourceSet(dependencySourceSet)
            }
        }
        create(fabricDefaultsTestModId) {
            sourceSet(defaultsTestModSourceSet)
        }
        create(fabricCustomTestModId) {
            sourceSet(customTestModSourceSet)
        }
        create(fabricServerSmokeTestModId) {
            sourceSet(serverSmokeTestModSourceSet)
        }
    }
    runs {
        val dependencyJarPaths = dependencyProjects.map {
            it.tasks.jar.get().archiveFile.get().asFile
        }
        val classPaths = sourceSets.main.get().output.classesDirs
        val resourcesPaths = listOfNotNull(
            sourceSets.main.get().output.resourcesDir
        )
        val classPathGroups = listOf(dependencyJarPaths, classPaths, resourcesPaths).flatten()
        val classPathGroupsString = classPathGroups
            .filterNotNull()
            .joinToString(separator = File.pathSeparator) {
                it.absoluteFile.toString()
            }

        val loomRunDir = File("run")

        named("client") {
            client()
            configName = "MezzConfig GUI Fabric Client"
            ideConfigGenerated(true)
            runDir(loomRunDir.resolve("client").toString())
            vmArgs(
                "-Dfabric.classPathGroups=${classPathGroupsString}",
                "-Dfabric.log.level=info"
            )
        }
        named("server") {
            server()
            configName = "MezzConfig GUI Fabric Server"
            ideConfigGenerated(true)
            runDir(loomRunDir.resolve("server").toString())
            vmArgs(
                "-Dfabric.classPathGroups=${classPathGroupsString}",
                "-Dfabric.log.level=info"
            )
        }
        create("serverSmokeTest") {
            server()
            configName = "MezzConfig GUI Fabric Server Smoke Test"
            runDir(serverSmokeTestRunDir.get().asFile.relativeTo(projectDir).path)
            programArgs("--nogui")
            vmArgs(
                "-Dfabric.classPathGroups=${classPathGroupsString}",
                "-Dfabric.log.level=info",
                "-DmezzConfigGui.loaderSmokeTest.successFile=${serverSmokeTestSuccessFile.get().asFile.absolutePath}"
            )
        }
    }

    accessWidenerPath.set(configGuiAccessWidener)
}

tasks.jar {
    dependsOn(mergedConfigLanguageResources)
    from(sourceSets.main.get().output)
    for (dependencySourceSet in dependencySourceSets) {
        from(dependencySourceSet.output) {
            exclude("fabric.mod.json")
            exclude("assets/mezz_config/lang/*.json")
            exclude("assets/mezz_config_gui/lang/*.json")
        }
    }
    from(mergedConfigLanguageResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    for (dependencySourceSet in dependencySourceSets) {
        from(dependencySourceSet.allJava)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
}

tasks.assemble {
    dependsOn(runtimeJar, tasks.named("sourcesJar"))
}

val testModClassesTasks = testModSourceSets.map {
    tasks.named(it.classesTaskName)
}
val serverSmokeTestModClassesTask = tasks.named(serverSmokeTestModSourceSet.classesTaskName)
val testModPaths = testModSourceSets.joinToString(separator = File.pathSeparator) {
    layout.buildDirectory.dir("resources/${it.name}")
        .get()
        .asFile
        .absolutePath
}
tasks.matching { it.name == "runClient" }.configureEach {
    dependsOn(testModClassesTasks)
    if (this is JavaExec) {
        for (testModSourceSet in testModSourceSets) {
            classpath(testModSourceSet.output)
        }
        jvmArgs("-Dfabric.addMods=$testModPaths")
    }
}

val runServerSmokeTestTasks = tasks.matching { it.name == "runServerSmokeTest" }
runServerSmokeTestTasks.configureEach {
    dependsOn(serverSmokeTestModClassesTask)
    if (this is JavaExec) {
        classpath(serverSmokeTestModSourceSet.output)
        val smokeTestModPath = layout.buildDirectory.dir("resources/${serverSmokeTestModSourceSet.name}")
            .get()
            .asFile
            .absolutePath
        jvmArgs("-Dfabric.addMods=$smokeTestModPath")
    }
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
            throw GradleException("The Fabric loader smoke test did not report success.")
        }
    }
}

tasks.check {
    dependsOn(testModClassesTasks, runServerSmokeTestTasks)
}

publishing {
    publications {
        register<MavenPublication>("configGuiFabricJar") {
            artifactId = baseArchivesName
            artifact(runtimeJar)
            artifact(tasks.named("sourcesJar"))

            val dependencyInfos = listOf(dependencyInfo(mezzConfigFabricDependency))

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
