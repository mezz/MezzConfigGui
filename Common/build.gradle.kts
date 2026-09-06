import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("idea")
    id("java")
    id("net.neoforged.moddev")
    id("maven-publish")
}

repositories {
    val deployDir = rootProject.findProperty("DEPLOY_DIR")
    if (deployDir != null) {
        maven(deployDir) {
            content {
                includeGroup("mezz.jei")
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
            includeGroup("mezz.jei")
            includeGroup("net.mezzdev.config")
        }
    }
}

// gradle.properties
val jUnitVersion: String by extra
val targetMinecraftVersion = providers.gradleProperty("minecraftVersion").get()
val neoformTimestamp: String by extra
val configGuiModId: String by extra
val configModGroup: String by extra
val modJavaVersion: String by extra
val mixinVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val fastutilVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigApiCompileDependency: Any by rootProject.extra
val jeiApiDependency: Any by rootProject.extra
val configGuiApiProject: Project = project(":${configGuiModId}-${targetMinecraftVersion}-config-gui-api")

group = configModGroup

val baseArchivesName = "${configGuiModId}-${targetMinecraftVersion}-config-gui"
base {
    archivesName.set(baseArchivesName)
}

val dependencyProjects: List<Project> = listOf(
    configGuiApiProject,
)

(dependencyProjects).forEach {
    project.evaluationDependsOn(it.path)
}

neoForge {
    neoFormVersion = "$targetMinecraftVersion-$neoformTimestamp"
    accessTransformers {
        from("src/main/accesstransformer.cfg")
    }
    addModdingDependenciesTo(sourceSets.test.get())
}

sourceSets {
    named("test") {
        //The test module has no resources
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:$mixinVersion")
    compileOnly(jeiApiDependency)
    compileOnly(mezzConfigApiCompileDependency)
    implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    implementation("it.unimi.dsi:fastutil:$fastutilVersion")
    dependencyProjects.forEach {
        implementation(it)
    }
    testImplementation("org.junit.jupiter:junit-jupiter:$jUnitVersion")
    testImplementation(mezzConfigApiCompileDependency)
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

val sourcesJarTask = tasks.named<Jar>("sourcesJar")

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
        register<MavenPublication>("configGuiJar") {
            artifactId = baseArchivesName
            artifact(tasks.jar.get())
            artifact(sourcesJarTask.get())

            val dependencyInfos = listOf(
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
            ) + dependencyProjects.map {
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
