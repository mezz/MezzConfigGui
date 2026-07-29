plugins {
    java
    idea
    id("fabric-loom")
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
    fun exclusiveMaven(url: String, filter: Action<InclusiveRepositoryContentDescriptor>) =
        exclusiveContent {
            forRepository { maven(url) }
            filter(filter)
        }
    exclusiveMaven("https://maven.parchmentmc.org") {
        includeGroupByRegex("org\\.parchmentmc.*")
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
val configGuiModId: String by extra
val configModGroup: String by extra
val configModId: String by extra
val configApiModId: String by extra
val fabricApiVersion: String by extra
val fabricLoaderVersion: String by extra
val minecraftVersion: String by extra
val modJavaVersion: String by extra
val parchmentMinecraftVersion: String by extra
val parchmentVersionFabric: String by extra
val jsr305Version: String by extra
val mezzConfigVersion: String by extra
val mezzConfigApiDependency: String by rootProject.extra
val mezzConfigApiFabricDependency: String by rootProject.extra
val configGuiApiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui-api")
val testModId = "mezz_config_gui_test_fabric_custom"

base {
    archivesName.set("${testModId}-${minecraftVersion}")
}

listOf(configGuiApiProject).forEach {
    project.evaluationDependsOn(it.path)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
    }
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
    minecraft("com.mojang:minecraft:$minecraftVersion")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentMinecraftVersion}:${parchmentVersionFabric}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    compileOnly(mezzConfigApiDependency)
    compileOnly(configGuiApiProject)
    modRuntimeOnly(mezzConfigApiFabricDependency)
    modRuntimeOnly("$configModGroup:${configModId}-${minecraftVersion}-fabric:$mezzConfigVersion")
}

loom {
    mods {
        create(testModId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    enabled = false
}

idea {
    module {
        for (fileName in listOf("build", "run", "out", "logs")) {
            excludeDirs.add(file(fileName))
        }
    }
}
