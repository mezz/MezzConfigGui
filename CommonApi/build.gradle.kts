import me.champeau.gradle.japicmp.JapicmpTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    id("idea")
    id("java")
    id("net.neoforged.moddev")
    id("maven-publish")
    id("me.champeau.gradle.japicmp")
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
val targetMinecraftVersion = providers.gradleProperty("minecraftVersion").get()
val neoformTimestamp: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigApiCompileDependency: Any by rootProject.extra
val apiBaselineVersion: String by extra
val apiBaselineRequired: String by extra
val requireApiBaseline = apiBaselineRequired.toBooleanStrict()

group = configModGroup

val baseArchivesName = "${configGuiModId}-${targetMinecraftVersion}-config-gui-api"
base {
    archivesName.set(baseArchivesName)
}

neoForge {
	neoFormVersion = "$targetMinecraftVersion-$neoformTimestamp"
}

sourceSets {
    named("main") {
        //The API has no resources
        resources.setSrcDirs(emptyList<String>())
    }
    named("test") {
        //The test module has no resources
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
	implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
	compileOnly(mezzConfigApiCompileDependency)
}

val apiBaseline by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = true
}

dependencies {
    apiBaseline("$group:$baseArchivesName:$apiBaselineVersion")
}

val apiBaselineClasspath = apiBaseline.incoming.artifactView {
    isLenient = !requireApiBaseline
}.files
val apiBaselineArchives = apiBaseline.incoming.artifactView {
    isLenient = !requireApiBaseline
    componentFilter {
        it is ModuleComponentIdentifier &&
            it.group == project.group.toString() &&
            it.module == baseArchivesName &&
            it.version == apiBaselineVersion
    }
}.files

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    javaToolchains {
        compilerFor {
            languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
        }
    }
}

val apiCompatibilityCheck = tasks.register<JapicmpTask>("apiCompatibilityCheck") {
    group = "verification"
    description = "Checks CommonApi binary compatibility with the first released API baseline."
    dependsOn(tasks.jar)

    oldClasspath.from(apiBaselineClasspath)
    oldArchives.from(apiBaselineArchives)
    newClasspath.from(configurations.runtimeClasspath)
    newArchives.from(tasks.jar)
    onlyModified.set(true)
    ignoreMissingClasses.set(true)
    richReport {
        title.set("MezzConfig GUI CommonApi compatibility")
        description.set("Binary compatibility against CommonApi $apiBaselineVersion.")
        destinationDir.set(layout.buildDirectory.dir("reports/api-compatibility"))
        reportName.set("index.html")
    }
    onlyIf("CommonApi $apiBaselineVersion has been published") {
        !oldArchives.isEmpty
    }
}

tasks.check {
    dependsOn(apiCompatibilityCheck)
}

publishing {
    publications {
        register<MavenPublication>("configGuiApiJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            val dependencyInfos = listOf(
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
