import org.slf4j.event.Level

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("net.neoforged.moddev")
}

// gradle.properties
val neoforgeVersion: String by extra
val minecraftVersion: String by extra
val configGuiModId: String by extra
val modJavaVersion: String by extra
val jsr305Version: String by extra
val mezzConfigApiProject: Project = project(":MezzConfigApiCompileOnly")
val configGuiApiProject: Project = project(":${configGuiModId}-${minecraftVersion}-config-gui-api")
val testModId = "mezz_config_gui_test_neoforge_custom"

base {
    archivesName.set("${testModId}-${minecraftVersion}")
}

listOf(mezzConfigApiProject, configGuiApiProject).forEach {
    project.evaluationDependsOn(it.path)
}

neoForge {
    version = neoforgeVersion

    mods {
        create(testModId) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        val testMod = mods.named(testModId)

        configureEach {
            getMods().set(setOf(testMod.get()))
        }
        create("client") {
            client()
            gameDirectory = file("run/client")
            logLevel = Level.DEBUG
        }
    }
}

sourceSets {
    named("test") {
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
    compileOnly(mezzConfigApiProject)
    compileOnly(configGuiApiProject)
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
