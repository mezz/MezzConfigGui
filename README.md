# MezzConfig GUI

MezzConfig GUI is a client-side config screen for Minecraft mods.
It can discover config schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

String values written as `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB`, or `0xAARRGGBB`
get a clickable color swatch in the standard text and list editors, including
auto-detected NeoForge configs. Eight-digit colors use alpha first. Picker edits
retain the string's prefix and number of digits and respect the config's
validation rules. Ordinary text editing remains available; bare hex strings
without a prefix are left as text to avoid mistaking identifiers for colors.

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
