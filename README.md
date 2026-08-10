# Mezz Config GUI

Mezz Config GUI is a shared, client-side config screen for Minecraft mods. It can discover schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

This branch targets Minecraft 1.21.1 on Fabric, Forge, and NeoForge. Java 21 is required.

## Using the mod

Install the loader-specific Mezz Config GUI jar together with the matching MezzConfig jar. Mod Menu and JEI integrations are supported when those mods are present.

## Using the API

Artifacts are published under the `net.mezzdev.config` Maven group:

| Artifact | Purpose |
| --- | --- |
| `mezz_config_gui-1.21.1-config-gui-api` | Public compile-time API |
| `mezz_config_gui-1.21.1-config-gui` | Loader-independent runtime implementation |
| `mezz_config_gui-1.21.1-fabric` | Full Fabric runtime |
| `mezz_config_gui-1.21.1-forge` | Full Forge runtime |
| `mezz_config_gui-1.21.1-neoforge` | Full NeoForge runtime |

Like [JEI's dependency model](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-26.1.2-and-26.2%5D), mods should compile only against the stable API and load the full platform-specific artifact at runtime. For Fabric Loom:

```kotlin
repositories {
    maven("https://maven.blamejared.com")
}

dependencies {
    // Compile against the API without exposing implementation classes.
    compileOnly("net.mezzdev.config:mezz_config_gui-1.21.1-config-gui-api:<version>")

    // Run development clients and servers with the full Fabric implementation.
    modRuntimeOnly("net.mezzdev.config:mezz_config_gui-1.21.1-fabric:<version>")
}
```

Use the equivalent `compileOnly` and `runtimeOnly` configurations for Forge or NeoForge, including the loader's normal deobfuscation wrapper when required. The platform artifacts declare the common GUI implementation and matching MezzConfig platform artifact as transitive dependencies, so the runtime dependency supplies the complete mod without putting implementation classes on the compile classpath.

## Building

The build expects sibling MezzConfig and JEI checkouts at the paths configured by `mezzConfigLocalPath` and `jeiLocalPath` in `gradle.properties`.

```console
./gradlew build
```

## License

Mezz Config GUI is available under the [MIT License](LICENSE).
