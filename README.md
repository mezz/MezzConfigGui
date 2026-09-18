# MezzConfig GUI

MezzConfig GUI is a client-side config screen for Minecraft mods.
It can discover config schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

## Developing with MezzConfig GUI

Pack authors can [customize mod order and visibility](docs/PACK_CONFIGURATION.md).
See [server configuration support](docs/SERVER_CONFIGS.md) for the difference
between MezzConfig remote editing and native NeoForge configs.

See [Dependency Setup for Minecraft 1.21.1](docs/DEPENDENCY_SETUP.md) for Fabric, Forge, and NeoForge Gradle examples.

## Building

```console
./gradlew build
```

`build` assembles every artifact and runs the complete release verification used
by CI. Run the verification lifecycle directly with:

```console
./gradlew check
```
