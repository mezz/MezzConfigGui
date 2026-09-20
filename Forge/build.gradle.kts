plugins {
    java
    idea
    eclipse
    `maven-publish`
    id("net.minecraftforge.gradle")
    id("me.modmuss50.mod-publish-plugin")
}

val forgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val modJavaVersion: String by extra
val jsr305Version: String by extra
val jetbrainsAnnotationsVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigForgeDependency: String by rootProject.extra
val forgeServerSmokeTestModId = "mezz_config_gui_test_forge_smoke"
val baseArchivesName = "${configGuiModId}-${minecraftVersion}-forge"
base.archivesName.set(baseArchivesName)

val commonProject = project(":Common")
evaluationDependsOn(commonProject.path)
val commonApiSourceSet = commonProject.sourceSets["api"]
val dependencySourceSets = listOf(commonProject.sourceSets.main.get(), commonApiSourceSet)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    withSourcesJar()
}

repositories {
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
    rootProject.findProperty("DEPLOY_DIR")?.let { deployDir ->
        maven(deployDir) { content { includeGroup("net.mezzdev.config") } }
    }
    maven("https://maven.blamejared.com") {
        content { includeGroup("net.mezzdev.config") }
    }
    mavenLocal { content { includeGroup("net.mezzdev.config") } }
}
minecraft.mavenizer(repositories)

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:$minecraftVersion-$forgeVersion"))
    compileOnly(commonProject)
    compileOnly(commonApiSourceSet.output)
    compileOnly(mezzConfigApiDependency)
    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    runtimeOnly(mezzConfigForgeDependency) { isTransitive = false }
}
// Forge's bootstrap also scans the runtime classpath for modules. Keep all mod
// classes and metadata together so plain class directories are not separate modules.
val configGuiRunOutput = layout.buildDirectory.dir("sourceSets/configGuiRun")
val configGuiRunSourceSet = sourceSets.create("configGuiRun") {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
    java.destinationDirectory.set(configGuiRunOutput)
    output.setResourcesDir(configGuiRunOutput)
}
val prepareConfigGuiRun = tasks.register<Sync>("prepareConfigGuiRun") {
    from(sourceSets.main.get().output)
    dependencySourceSets.forEach { from(it.output) }
    into(configGuiRunOutput)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named(configGuiRunSourceSet.compileJavaTaskName) { enabled = false }
tasks.named(configGuiRunSourceSet.processResourcesTaskName) { enabled = false }
tasks.named(configGuiRunSourceSet.classesTaskName) { dependsOn(prepareConfigGuiRun) }
sourceSets.main {
    runtimeClasspath = configurations.runtimeClasspath.get() + configGuiRunSourceSet.output
}

val serverSmokeTestModSourceSet = sourceSets.create("serverSmokeTestMod") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += configGuiRunSourceSet.output
    val combinedOutput = layout.buildDirectory.dir("sourceSets/$name")
    java.destinationDirectory.set(combinedOutput)
    output.setResourcesDir(combinedOutput)
}
configurations.named(serverSmokeTestModSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(serverSmokeTestModSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}
configurations.named(serverSmokeTestModSourceSet.compileOnlyConfigurationName) {
    extendsFrom(configurations.compileOnly.get())
}
val serverSmokeTestRunDir = layout.buildDirectory.dir("run/server-smoke")
val serverSmokeTestSuccessFile = serverSmokeTestRunDir.map { it.file("smoke-test-passed") }

minecraft {
    mappings("official", minecraftVersion)
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
    runs {
        configureEach {
            systemProperty("forge.logging.console.level", "info")
            mods.create(configGuiModId) {
                source(configGuiRunSourceSet)
            }
        }
        create("client") {
            workingDir.set(layout.projectDirectory.dir("run/client"))
            if (providers.systemProperty("os.name").get().startsWith("Mac")) {
                jvmArgs("-XstartOnFirstThread")
            }
        }
        create("server") {
            workingDir.set(layout.projectDirectory.dir("run/server"))
            args("--nogui")
            systemProperty("terminal.jline", "false")
            systemProperty("terminal.ansi", "false")
            with(serverSmokeTestModSourceSet) {
                workingDir.set(serverSmokeTestRunDir)
                systemProperty("mezzConfigGui.loaderSmokeTest.successFile", serverSmokeTestSuccessFile.get().asFile.absolutePath)
                mods.create(forgeServerSmokeTestModId) { source(serverSmokeTestModSourceSet) }
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    })
}
tasks.matching { it.name == "runServerSmokeTestModServer" }.configureEach {
    outputs.file(serverSmokeTestSuccessFile)
    outputs.upToDateWhen { false }
    doFirst {
        val directory = serverSmokeTestRunDir.get().asFile
        directory.mkdirs()
        directory.resolve("eula.txt").writeText("eula=true\n")
        directory.resolve("server.properties").writeText("online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\n")
        serverSmokeTestSuccessFile.get().asFile.delete()
    }
    doLast {
        check(serverSmokeTestSuccessFile.get().asFile.isFile) { "The Forge loader smoke test did not report success." }
    }
}
val runServerSmokeTest = tasks.register("runServerSmokeTest") {
    group = "verification"
    description = "Starts the Forge dedicated server and verifies both config mods load."
    dependsOn("runServerSmokeTestModServer")
}
tasks.check { dependsOn(runServerSmokeTest) }

extra["configLanguageDependencyProjects"] = listOf(commonProject)
apply(from = rootProject.file("buildtools/ConfigLanguageResources.gradle.kts"))
val mergedConfigLanguageResources = tasks.named("mergeConfigLanguageResources")

tasks.jar {
    dependsOn(mergedConfigLanguageResources)
    dependencySourceSets.forEach { dependencySourceSet ->
        from(dependencySourceSet.output) { exclude("assets/mezz_config/lang/*.json") }
    }
    from(mergedConfigLanguageResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
    dependencySourceSets.forEach { from(it.allJava) }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
publishMods { file.set(tasks.jar.flatMap { it.archiveFile }) }

publishing {
    publications.register<MavenPublication>("configGuiForgeJar") {
        artifactId = baseArchivesName
        artifact(tasks.jar)
        artifact(sourcesJarTask)
        pom.withXml {
            val (groupId, dependencyArtifactId, dependencyVersion) = mezzConfigForgeDependency.split(":")
            val dependency = asNode().appendNode("dependencies").appendNode("dependency")
            dependency.appendNode("groupId", groupId)
            dependency.appendNode("artifactId", dependencyArtifactId)
            dependency.appendNode("version", dependencyVersion)
        }
    }
}

idea.module {
    for (fileName in listOf("build", "run", "runs", "out", "logs")) {
        excludeDirs.add(file(fileName))
    }
}
