plugins {
    id("idea")
    id("java")
    id("net.neoforged.moddev")
}

// gradle.properties
val minecraftVersion: String by extra
val neoformTimestamp: String by extra
val modJavaVersion: String by extra
val jetbrainsAnnotationsVersion: String by extra
val jsr305Version: String by extra

group = "net.mezzdev.config.local"

base {
    archivesName.set("mezz-config-api-compile-only")
}

neoForge {
    neoFormVersion = "$minecraftVersion-$neoformTimestamp"
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("../MezzConfig/CommonApi/src/main/java")))
        resources.setSrcDirs(emptyList<String>())
    }
    named("test") {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    compileOnly("com.google.code.findbugs:jsr305:$jsr305Version")
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

idea {
    module {
        for (fileName in listOf("build", "run", "out", "logs")) {
            excludeDirs.add(file(fileName))
        }
    }
}
