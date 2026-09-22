import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.jarcompatibilitychecker.gradle.CompatibilityTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("idea")
    id("java")
    id("net.fabricmc.fabric-loom") apply false
    id("net.neoforged.jarcompatibilitychecker")
    id("maven-publish")
}

pluginManager.apply("net.fabricmc.fabric-loom-remap")
val loom = extensions.getByType<LoomGradleExtensionAPI>()

repositories {
    maven {
        name = "publicationValidation"
        url = rootProject.layout.buildDirectory.dir("publication-validation").get().asFile.toURI()
        content {
            includeGroup("net.mezzdev.config")
        }
    }
    val deployDir = rootProject.findProperty("DEPLOY_DIR")
    if (deployDir != null) {
        maven(deployDir) {
            content {
                includeGroup("net.mezzdev.config")
            }
        }
    }
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

// gradle.properties
val jUnitVersion: String by extra
val targetMinecraftVersion = providers.gradleProperty("minecraftVersion").get()
val configGuiModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val jsr305Version: String by extra
val fastutilVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigRuntimeDependency = rootProject.extra["mezzConfigForgeDependency"].toString()
val jeiApiDependency: String by rootProject.extra
val apiBaselineVersion: String by extra
val releaseVersion: String by rootProject.extra
val isInitialApiRelease = apiBaselineVersion == releaseVersion

group = configModGroup

val baseArchivesName = "${configGuiModId}-${targetMinecraftVersion}-config-gui"
val apiArchivesName = "${configGuiModId}-${targetMinecraftVersion}-config-gui-api"
base {
    archivesName.set(baseArchivesName)
}

val apiSourceSet = sourceSets.create("api")
afterEvaluate {
    configurations.named(apiSourceSet.compileClasspathConfigurationName) {
        extendsFrom(configurations.named("minecraftNamedCompile").get())
    }
}

configure<LoomGradleExtensionAPI> {
    accessWidenerPath.set(file("src/main/resources/mezz_config.accesswidener"))
}

dependencies {
    add("minecraft", "com.mojang:minecraft:$targetMinecraftVersion")
    add("mappings", loom.officialMojangMappings())

    implementation(apiSourceSet.output)
    add(apiSourceSet.implementationConfigurationName, "org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    add(apiSourceSet.compileOnlyConfigurationName, mezzConfigApiDependency)
    add(apiSourceSet.compileOnlyConfigurationName, "com.google.code.findbugs:jsr305:$jsr305Version")

    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    compileOnly(jeiApiDependency)
    compileOnly(mezzConfigApiDependency)
    implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    implementation("it.unimi.dsi:fastutil:$fastutilVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$jUnitVersion")
    testImplementation(mezzConfigApiDependency)
    testCompileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    // Exercise sorting persistence with the released implementation, not a reimplementation in a test double.
    testImplementation(mezzConfigRuntimeDependency) {
        isTransitive = false
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    include("net/mezzdev/config/gui/**/*Test.class")
    outputs.upToDateWhen { false }
    testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val commonCompileClasspath = configurations.named(sourceSets.main.get().compileClasspathConfigurationName)
val validateVanillaMinecraftClasspath = tasks.register("validateVanillaMinecraftClasspath") {
    group = "verification"
    description = "Checks that Common compiles against vanilla Minecraft instead of a loader-patched jar."
    inputs.files(commonCompileClasspath)

    doLast {
        val components = commonCompileClasspath.get().incoming.resolutionResult.allComponents
            .mapNotNull { it.id as? ModuleComponentIdentifier }
        val patchedMinecraft = components.filter {
            (it.group == "net.minecraftforge" && it.module == "forge") ||
                (it.group == "net.neoforged" && it.module == "neoforge")
        }
        require(patchedMinecraft.isEmpty()) {
            "Common compile classpath contains loader-patched Minecraft: ${patchedMinecraft.joinToString()}"
        }

        val vanillaMinecraft = components.filter {
            it.group == "net.minecraft" && it.module.startsWith("minecraft-")
        }
        require(vanillaMinecraft.size == 1) {
            "Expected one vanilla Minecraft component on the Common compile classpath, found: ${vanillaMinecraft.joinToString()}"
        }
    }
}

tasks.matching { it.name == "remapJar" || it.name == "remapSourcesJar" }.configureEach {
    enabled = false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    }
    withSourcesJar()
}

tasks.jar {
    from(apiSourceSet.output)
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
    from(apiSourceSet.allJava)
}

val apiJarTask = tasks.register<Jar>("apiJar") {
    archiveBaseName.set(apiArchivesName)
    from(apiSourceSet.output)
}

val apiSourcesJarTask = tasks.register<Jar>("apiSourcesJar") {
    archiveBaseName.set(apiArchivesName)
    archiveClassifier.set("sources")
    from(apiSourceSet.allJava)
}

val apiJavadocDir = layout.buildDirectory.dir("docs/apiJavadoc")
val apiJavadocTask = tasks.register<Javadoc>("apiJavadoc") {
    source(apiSourceSet.allJava)
    classpath = apiSourceSet.compileClasspath
    destinationDir = apiJavadocDir.get().asFile
}

val apiJavadocJarTask = tasks.register<Jar>("apiJavadocJar") {
    dependsOn(apiJavadocTask)
    archiveBaseName.set(apiArchivesName)
    archiveClassifier.set("javadoc")
    from(apiJavadocDir)
}

tasks.assemble {
    dependsOn(apiJarTask, apiSourcesJarTask, apiJavadocJarTask)
}

val apiBaseline by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    apiBaseline("$group:$apiArchivesName:$apiBaselineVersion")
}

val apiBaselineArchives = apiBaseline.incoming.artifactView {
    isLenient = isInitialApiRelease
}.files
val missingApiBaselineArchive = layout.buildDirectory.file("api-baseline/missing-$apiBaselineVersion.jar")
val apiBaselineArchive = layout.file(apiBaselineArchives.elements.map { archives ->
    archives.singleOrNull()?.asFile ?: missingApiBaselineArchive.get().asFile
})

val checkJarCompatibility = tasks.named<CompatibilityTask>("checkJarCompatibility") {
    group = "verification"
    description = "Checks the public API artifact against the first released baseline."

    inputJar.set(apiJarTask.flatMap { it.archiveFile })
    baseJar.set(apiBaselineArchive)
    libraries.setFrom(apiSourceSet.compileClasspath.filter(File::exists))
    fail.set(true)
    onlyIf("the initial MezzConfig GUI API $apiBaselineVersion baseline has been published") {
        baseJar.get().asFile.exists()
    }
}

tasks.check {
    dependsOn(checkJarCompatibility, validateVanillaMinecraftClasspath)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    javaToolchains {
        compilerFor {
            languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("configGuiApiJar") {
            artifactId = apiArchivesName
            artifact(apiJarTask)
            artifact(apiSourcesJarTask)
            artifact(apiJavadocJarTask)

            pom {
                name.set("MezzConfig GUI API")
            }

            val dependencyInfos = listOf(
                dependencyInfo("org.jspecify:jspecify:${rootProject.extra["jspecifyVersion"]}"),
                mapOf(
                    "groupId" to "org.jetbrains",
                    "artifactId" to "annotations",
                    "version" to jetbrainsAnnotationsVersion
                ),
                dependencyInfo(mezzConfigApiDependency)
            )

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
        register<MavenPublication>("configGuiJar") {
            artifactId = baseArchivesName
            artifact(tasks.jar)
            artifact(sourcesJarTask)

            val dependencyInfos = listOf(
                dependencyInfo("org.jspecify:jspecify:${rootProject.extra["jspecifyVersion"]}"),
                mapOf(
                    "groupId" to "org.jetbrains",
                    "artifactId" to "annotations",
                    "version" to jetbrainsAnnotationsVersion
                ),
                mapOf(
                    "groupId" to "it.unimi.dsi",
                    "artifactId" to "fastutil",
                    "version" to fastutilVersion
                ),
                dependencyInfo(mezzConfigApiDependency)
            )

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
