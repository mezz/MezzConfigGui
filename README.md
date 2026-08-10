# Mezz Config GUI

Mezz Config GUI is a shared, client-side config screen for Minecraft mods. It can discover schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

This branch targets Minecraft 1.21.1 on Fabric, Forge, and NeoForge. Java 21 is required.

## Using the mod

Install the loader-specific Mezz Config GUI jar together with the matching MezzConfig jar. Mod Menu and JEI integrations are supported when those mods are present.

## Using the API

Artifacts are published under the `net.mezzdev.config` Maven group:

| Artifact | Purpose |
| --- | --- |
| `mezz_config_gui-1.21.1-config-gui-api` | Public config GUI API |
| `mezz_config_gui-1.21.1-config-gui` | Loader-independent implementation |
| `mezz_config_gui-1.21.1-fabric` | Fabric integration |
| `mezz_config_gui-1.21.1-forge` | Forge integration |
| `mezz_config_gui-1.21.1-neoforge` | NeoForge integration |

Add the Maven repository and use the artifact for your loader with its normal deobfuscating dependency configuration:

```kotlin
repositories {
    maven("https://maven.blamejared.com")
}

dependencies {
    modImplementation("net.mezzdev.config:mezz_config_gui-1.21.1-fabric:<version>")
}
```

The loader artifacts declare the common GUI artifacts and matching MezzConfig loader artifact as transitive dependencies.

## Building

The build expects sibling MezzConfig and JEI checkouts at the paths configured by `mezzConfigLocalPath` and `jeiLocalPath` in `gradle.properties`.

```console
./gradlew build
```

## License

Mezz Config GUI is available under the [MIT License](LICENSE).
