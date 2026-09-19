import groovy.lang.Binding
import groovy.lang.GroovyShell
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.Locale

plugins {
	base

	// https://github.com/modmuss50/mod-publish-plugin
	id("me.modmuss50.mod-publish-plugin") version("1.1.0") apply(false)

	// https://github.com/mezz/JavaFormatting
	id("net.mezzdev.java-formatting") version("0.4.0")

    // https://plugins.gradle.org/plugin/com.dorongold.task-tree
    id("com.dorongold.task-tree") version("4.0.2")

    // https://github.com/neoforged/JarCompatibilityChecker
    id("net.neoforged.jarcompatibilitychecker") version("0.1.19") apply(false)

    // https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml
    id("fabric-loom") version("1.13.6") apply(false)

    // https://projects.neoforged.net/neoforged/moddevgradle
    id("net.neoforged.moddev") version("2.0.146") apply(false)

    // https://files.minecraftforge.net/net/minecraftforge/gradle/ForgeGradle/index.html
    id("net.minecraftforge.gradle") version("6.0.54") apply(false)

    // https://mvnrepository.com/artifact/org.parchmentmc.librarian.forgegradle/org.parchmentmc.librarian.forgegradle.gradle.plugin
    id("org.parchmentmc.librarian.forgegradle") version("1.2.0") apply(false)
}
apply {
	from("buildtools/ColoredOutput.gradle")
}
repositories {
    val deployDir = findProperty("DEPLOY_DIR")
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
val configApiModId: String by extra
val configGuiApiModId: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val configModId: String by extra
val fabricApiVersion: String by extra
val fabricApiVersionRange: String by extra
val fabricLoaderVersion: String by extra
val fabricLoaderVersionRange: String by extra
val forgeVersionRange: String by extra
val githubUrl: String by extra
val forgeLoaderVersionRange: String by extra
val mezzConfigVersion: String by extra
val mezzConfigFabricVersionRange: String by extra
val mezzConfigVersionRange: String by extra
val minecraftVersion: String by extra
val minecraftVersionRange: String by extra
val modAuthor: String by extra
val modDescription: String by extra
val modGroup: String by extra
val modId: String by extra
val modJavaVersion: String by extra
val modName: String by extra
val neoforgeVersionRange: String by extra
val neoforgeLoaderVersionRange: String by extra
val jeiVersion: String by extra
val specificationVersion: String by extra
val releaseSpecificationVersion = specificationVersion
val modPublishDryRun = providers.gradleProperty("publishDryRun").orElse("true")
    .map { it.toBooleanStrict() }.get()

abstract class ValidateReleaseVersion : DefaultTask() {
    @get:Input
    abstract val releaseVersion: Property<String>

    @get:Input
    abstract val specificationVersion: Property<String>

    @TaskAction
    fun validate() {
        val releaseVersion = releaseVersion.get()
        val specificationVersion = specificationVersion.get()
        if (releaseVersion.isBlank()) {
            throw GradleException("No release version was provided; set RELEASE_VERSION or TAG_NAME.")
        }
        if (releaseVersion != specificationVersion) {
            throw GradleException(
                "Release version '$releaseVersion' does not match specificationVersion '$specificationVersion'."
            )
        }
    }
}

abstract class ValidateReleasePipeline : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pipelineFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val binding = Binding(mapOf("pipelineFile" to pipelineFile.get().asFile))
        GroovyShell(binding).evaluate(testFile.get().asFile)
    }
}

fun normalizeReleaseVersion(value: String): String {
    val tagName = value.trim().substringAfterLast('/')
    return tagName.removePrefix("v")
}

fun Configuration.singleFileContents(): Provider<String> =
    incoming.files.elements.map { elements -> elements.single().asFile.readText() }

val configuredReleaseVersion = providers.gradleProperty("RELEASE_VERSION")
    .orElse(providers.environmentVariable("TAG_NAME"))
    .orNull
    ?.let(::normalizeReleaseVersion)
    ?.takeIf(String::isNotEmpty)
val buildNumber = providers.gradleProperty("BUILD_NUMBER")
    .orElse("9999")
    .get()
val projectVersion = configuredReleaseVersion ?: "${releaseSpecificationVersion}.${buildNumber}"

extra["mezzConfigApiDependency"] = "$configModGroup:${configModId}-${minecraftVersion}-config-api:$mezzConfigVersion"
extra["mezzConfigFabricDependency"] = "$configModGroup:${configModId}-${minecraftVersion}-fabric:$mezzConfigVersion"
extra["mezzConfigForgeDependency"] = "$configModGroup:${configModId}-${minecraftVersion}-forge:$mezzConfigVersion"
extra["mezzConfigNeoForgeDependency"] = "$configModGroup:${configModId}-${minecraftVersion}-neoforge:$mezzConfigVersion"
extra["jeiApiDependency"] = "mezz.jei:jei-${minecraftVersion}-common-api:$jeiVersion"

javaFormatting {
	target("*/src/*/java/net/mezzdev/**/*.java")
	all()
}

tasks.register<ValidateReleaseVersion>("validateReleaseVersion") {
    group = "verification"
    description = "Checks that a release tag matches specificationVersion."

    releaseVersion.set(configuredReleaseVersion ?: "")
    specificationVersion.set(releaseSpecificationVersion)
}

val validatePublishing = tasks.register("validatePublishing") {
    group = "verification"
    description = "Publishes every Maven publication to a local validation repository."
}

val publishMavenRelease = tasks.register("publishMavenRelease") {
    group = "publishing"
    description = "Publishes the supported Maven artifacts to the release repository."
    dependsOn(tasks.named("validateReleaseVersion"))
}

val validateReleasePipeline = tasks.register<ValidateReleasePipeline>("validateReleasePipeline") {
    group = "verification"
    description = "Checks Jenkins release eligibility and repeated-tag handling without publishing."
    pipelineFile.set(layout.projectDirectory.file(".jenkins/Jenkinsfile"))
    testFile.set(layout.projectDirectory.file(".jenkins/tests/ReleasePipelineTest.groovy"))
}

tasks.assemble {
    dependsOn(subprojects.map { "${it.path}:assemble" })
}

tasks.check {
    description = "Runs project checks and validates Maven publications locally."
    dependsOn(subprojects.map { "${it.path}:check" })
    dependsOn(validatePublishing)
    dependsOn(validateReleasePipeline)
}

subprojects {
    version = projectVersion
    group = modGroup

    plugins.withId("me.modmuss50.mod-publish-plugin") {
        val loaderName = project.name
        val curseProjectId = providers.gradleProperty("curseProjectId")
        val modrinthId = providers.gradleProperty("modrinthId")
        val changelogHtml = configurations.create("changelogHtml") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("changelogHtml"))
            }
        }
        val changelogMarkdown = configurations.create("changelogMarkdown") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("changelogMarkdown"))
            }
        }
        dependencies {
            add(changelogHtml.name, project(":Changelog"))
            add(changelogMarkdown.name, project(":Changelog"))
        }
        extensions.configure<ModPublishExtension> {
            dryRun.set(modPublishDryRun)
            version.set(projectVersion)
            displayName.set("$modName $projectVersion for $loaderName $minecraftVersion")
            type.set(BETA)
            modLoaders.add(loaderName.lowercase(Locale.ROOT))
            changelog.set(changelogMarkdown.singleFileContents())

            curseforge {
                projectId.set(curseProjectId.orElse("0"))
                projectSlug.set("mezzconfiggui")
                accessToken.set(providers.gradleProperty("curseforgeApikey"))
                changelog.set(changelogHtml.singleFileContents())
                changelogType.set("html")
                minecraftVersions.add(minecraftVersion)
                javaVersions.add(JavaVersion.toVersion(modJavaVersion))
                clientRequired.set(true)
                serverRequired.set(false)
                requires("mezzconfig")
                if (loaderName == "Fabric") {
                    requires("fabric-api")
                }
            }

            modrinth {
                projectId.set(modrinthId.orElse("00000000"))
                accessToken.set(providers.gradleProperty("modrinthToken"))
                minecraftVersions.add(minecraftVersion)
                requires("7tEfOcA7")
                if (loaderName == "Fabric") {
                    requires("fabric-api")
                }
            }
        }
        tasks.withType<PublishModTask>().configureEach {
            if (!modPublishDryRun) {
                dependsOn(rootProject.tasks.named("validateReleaseVersion"))
                doFirst {
                    if (!curseProjectId.isPresent || !modrinthId.isPresent) {
                        throw GradleException(
                            "Real platform publishing requires the curseProjectId and modrinthId Gradle properties."
                        )
                    }
                }
            }
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "validation"
                    url = rootProject.layout.buildDirectory.dir("publication-validation").get().asFile.toURI()
                }
                providers.gradleProperty("DEPLOY_DIR").orNull?.let { deployDir ->
                    maven {
                        name = "release"
                        url = uri(deployDir)
                    }
                }
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set("$modName ${project.name}")
                    description.set(modDescription)
                    url.set(githubUrl)

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/license/mit")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set(modAuthor)
                            name.set(modAuthor)
                        }
                    }
                    scm {
                        connection.set("scm:git:$githubUrl.git")
                        developerConnection.set("scm:git:$githubUrl.git")
                        url.set(githubUrl)
                    }
                }
            }
        }

        val validationPublicationTaskPath = "$path:publishAllPublicationsToValidationRepository"
        validatePublishing.configure {
            dependsOn(validationPublicationTaskPath)
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        // The compatibility baseline may come from the same local validation repository.
        // Finish reading it before any publication task can replace the artifact.
        dependsOn(":Common:checkJarCompatibility")
        if (name.endsWith("ToReleaseRepository")) {
            dependsOn(rootProject.tasks.named("validateReleaseVersion"))
        }
    }

    tasks.withType<Javadoc> {
        // workaround cast for https://github.com/gradle/gradle/issues/7038
        val standardJavadocDocletOptions = options as StandardJavadocDocletOptions
        // prevent java 8's strict doclint for javadocs from failing builds
        standardJavadocDocletOptions.addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(JavaLanguageVersion.of(modJavaVersion).asInt())
    }

    tasks.withType<Jar> {
        from(rootProject.file("LICENSE")) {
            into("META-INF")
            rename("LICENSE", "LICENSE-MezzConfigGui")
        }
        manifest {
            attributes(mapOf(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Specification-Version" to specificationVersion,
                "Implementation-Title" to name,
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to modAuthor
            ))
        }
    }

    tasks.withType<ProcessResources> {
        exclude("**/.DS_Store")

        val properties = mapOf(
            "configApiModId" to configApiModId,
            "configGuiApiModId" to configGuiApiModId,
            "configGuiModId" to configGuiModId,
            "configModId" to configModId,
            "fabricApiVersion" to fabricApiVersion,
            "fabricApiVersionRange" to fabricApiVersionRange,
            "fabricLoaderVersion" to fabricLoaderVersion,
            "fabricLoaderVersionRange" to fabricLoaderVersionRange,
            "forgeVersionRange" to forgeVersionRange,
            "githubUrl" to githubUrl,
            "forgeLoaderVersionRange" to forgeLoaderVersionRange,
            "neoforgeVersionRange" to neoforgeVersionRange,
            "neoforgeLoaderVersionRange" to neoforgeLoaderVersionRange,
            "minecraftVersion" to minecraftVersion,
            "minecraftVersionRange" to minecraftVersionRange,
            "mezzConfigFabricVersionRange" to mezzConfigFabricVersionRange,
            "mezzConfigVersionRange" to mezzConfigVersionRange,
            "modAuthor" to modAuthor,
            "modDescription" to modDescription,
            "modId" to modId,
            "modJavaVersion" to modJavaVersion,
            "modName" to modName,
            "specificationVersion" to specificationVersion,
            "version" to version,
        )
        inputs.properties(properties)
        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta", "fabric.mod.json")) {
            expand(properties)
        }
    }

    // Activate reproducible builds
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

publishMavenRelease.configure {
    if (providers.gradleProperty("DEPLOY_DIR").isPresent) {
        // Platform Maven jars depend on the shared implementation as well as the public API.
        dependsOn(":Common:publishConfigGuiApiJarPublicationToReleaseRepository")
        dependsOn(":Common:publishConfigGuiJarPublicationToReleaseRepository")
        listOf("Fabric", "Forge", "NeoForge").forEach {
            dependsOn(":$it:publishConfigGui${it}JarPublicationToReleaseRepository")
        }
    } else {
        doFirst {
            throw GradleException("No Maven release repository was provided; set DEPLOY_DIR.")
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.isDeprecation = true
        options.compilerArgs.add("-Xlint:unchecked")
    }
}
