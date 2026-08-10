# Dependency Setup for Minecraft 1.21.1

Compile only against the MezzConfig GUI API, then load the full artifact for your platform at runtime.
This keeps implementation classes off your compile classpath, to avoid having your mod depend on unstable internal classes that will change over time.

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

## NeoForge with ModDevGradle

The [Minecraft 1.21.1 ModDevGradle template](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) uses a local runtime configuration for optional full-mod dependencies. This makes the platform artifact available to development runs without publishing it as a dependency of your mod:

```kotlin
val localRuntime by configurations.getting
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    compileOnly(mezzConfigGuiApi)
    add(
        localRuntime.name,
        "net.mezzdev.config:mezz_config_gui-$minecraftVersion-neoforge:$mezzConfigGuiVersion"
    )
}
```

## NeoForge with NeoGradle

The [Minecraft 1.21.1 NeoGradle template](https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle) uses the same local runtime pattern:

```kotlin
val localRuntime by configurations.getting
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    compileOnly(mezzConfigGuiApi)
    add(
        localRuntime.name,
        "net.mezzdev.config:mezz_config_gui-$minecraftVersion-neoforge:$mezzConfigGuiVersion"
    )
}
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
