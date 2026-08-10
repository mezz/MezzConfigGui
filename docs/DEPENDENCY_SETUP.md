# Dependency Setup for Minecraft 1.21.1

Compile only against the Mezz Config GUI API, then load the full artifact for your platform at runtime. This keeps implementation classes off your compile classpath and follows the same separation described in [JEI's dependency setup](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-26.1.2-and-26.2%5D).

The examples use Gradle Kotlin DSL. Set the versions in `gradle.properties`:

```properties
minecraftVersion=1.21.1
mezzConfigGuiVersion=<version>
```

Add the Maven repository to `build.gradle.kts`:

```kotlin
repositories {
    maven("https://maven.blamejared.com") {
        content {
            includeGroup("net.mezzdev.config")
        }
    }
}

val minecraftVersion: String by project
val mezzConfigGuiVersion: String by project
val mezzConfigGuiApi =
    "net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion"
```

## Fabric Loom

Use the API as a normal compile-only dependency. Load the remapped Fabric artifact as a mod at runtime:

```kotlin
dependencies {
    compileOnly(mezzConfigGuiApi)
    modRuntimeOnly(
        "net.mezzdev.config:mezz_config_gui-$minecraftVersion-fabric:$mezzConfigGuiVersion"
    )
}
```

## ForgeGradle

Pass both artifacts through ForgeGradle's deobfuscation helper:

```kotlin
dependencies {
    compileOnly(fg.deobf(mezzConfigGuiApi))
    runtimeOnly(
        fg.deobf(
            "net.mezzdev.config:mezz_config_gui-$minecraftVersion-forge:$mezzConfigGuiVersion"
        )
    )
}
```

## NeoForge ModDevGradle

Use normal Gradle dependency configurations with ModDevGradle:

```kotlin
dependencies {
    compileOnly(mezzConfigGuiApi)
    runtimeOnly(
        "net.mezzdev.config:mezz_config_gui-$minecraftVersion-neoforge:$mezzConfigGuiVersion"
    )
}
```

The platform runtime dependency brings in the loader-independent GUI implementation and matching MezzConfig platform artifact transitively. Do not add either implementation artifact to your compile classpath.
