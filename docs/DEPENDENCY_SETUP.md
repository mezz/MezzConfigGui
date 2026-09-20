# Dependency Setup

Compile only against the MezzConfig GUI API, then load the full artifact for your platform at runtime.
This keeps implementation classes off your compile classpath, to avoid having your mod depend on unstable internal classes that will change over time.

Select a supported Minecraft and loader combination from the [branch table](BRANCHES.md).
Every artifact name includes its exact Minecraft target; a jar is not shared across game versions.
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
```

## NeoForge with ModDevGradle

The [Minecraft 1.21.1 ModDevGradle template](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) uses a local runtime configuration for optional full-mod dependencies. This makes the platform artifact available to development runs without publishing it as a dependency of your mod:

```kotlin
val localRuntime by configurations.getting
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    compileOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion")
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
    compileOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion")
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
    compileOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion")
    modRuntimeOnly(
        "net.mezzdev.config:mezz_config_gui-$minecraftVersion-fabric:$mezzConfigGuiVersion"
    )
}
```

For unobfuscated Minecraft 26.x with Fabric Loom, use `runtimeOnly` instead of
`modRuntimeOnly`; there is no Minecraft remapping step.

## ForgeGradle 7 (1.21.1)

Forge 1.21.1 uses official mappings at runtime. Declare the API and complete
Forge runtime as ordinary dependencies:

```kotlin
dependencies {
    compileOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion")
    runtimeOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-forge:$mezzConfigGuiVersion")
}
```

The Forge runtime currently depends on MezzConfig 0.5.9, the latest published
Forge 1.21.1 artifact. Fabric and NeoForge use MezzConfig 0.5.11.

## ForgeGradle 6 (1.19.2 and 1.20.1)

Pass both artifacts through ForgeGradle's deobfuscation helper:

```kotlin
dependencies {
    compileOnly(fg.deobf("net.mezzdev.config:mezz_config_gui-$minecraftVersion-config-gui-api:$mezzConfigGuiVersion"))
    runtimeOnly(
        fg.deobf(
            "net.mezzdev.config:mezz_config_gui-$minecraftVersion-forge:$mezzConfigGuiVersion"
        )
    )
}
```
