import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.jarcompatibilitychecker.gradle.CompatibilityTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("idea")
    id("java")
    id("net.neoforged.moddev")
    id("net.neoforged.jarcompatibilitychecker")
    id("maven-publish")
}

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
val mixinVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val fastutilVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigRuntimeDependency = rootProject.extra["mezzConfigNeoForgeDependency"].toString()
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

configure<NeoForgeExtension> {
    version = project.extra["neoforgeVersion"].toString()
    accessTransformers { from(file("src/main/accesstransformer.cfg")) }
    addModdingDependenciesTo(apiSourceSet)
    addModdingDependenciesTo(sourceSets.test.get())
}


dependencies {
    implementation(apiSourceSet.output)
    add(apiSourceSet.implementationConfigurationName, "org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    add(apiSourceSet.compileOnlyConfigurationName, mezzConfigApiDependency)

    compileOnly("org.spongepowered:mixin:$mixinVersion")
    compileOnly(jeiApiDependency)
    compileOnly(mezzConfigApiDependency)
    implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    implementation("it.unimi.dsi:fastutil:$fastutilVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$jUnitVersion")
    testImplementation(mezzConfigApiDependency)
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
    dependsOn(checkJarCompatibility)
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
