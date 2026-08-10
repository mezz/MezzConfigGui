# Mezz Config GUI

Mezz Config GUI is a shared, client-side config screen for Minecraft mods. It can discover schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

This branch targets Minecraft 1.21.1 on Fabric, Forge, and NeoForge. Java 21 is required.

## Using the mod

Install the loader-specific Mezz Config GUI jar together with the matching MezzConfig jar. Mod Menu and JEI integrations are supported when those mods are present.

## Developing with Mezz Config GUI

See [Dependency Setup for Minecraft 1.21.1](docs/DEPENDENCY_SETUP.md) for Fabric, Forge, and NeoForge Gradle examples.

## Building

The build expects sibling MezzConfig and JEI checkouts at the paths configured by `mezzConfigLocalPath` and `jeiLocalPath` in `gradle.properties`.

```console
./gradlew build
```

## License

Mezz Config GUI is available under the [MIT License](LICENSE).
