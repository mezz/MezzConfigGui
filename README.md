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

`build` assembles every artifact, runs the tests and API checks, and validates
Maven publications in a local build directory. Run the verification lifecycle
directly with:

```console
./gradlew check
```

CI also checks platform release metadata separately with
`./gradlew publishMods -PpublishDryRun=true`.

See [automatic changelogs](Changelog/README.md) to preview the Git-based changelog
shared by CurseForge and Modrinth.

See [releasing](docs/RELEASING.md) for the Jenkins tag-based publishing flow.

## Contributing

Contributors must sign the [Contributor License Agreement](CLA.md) before their
pull requests can be merged. Sign through
[CLA Assistant](https://cla-assistant.io/mezz/MezzConfigGui) using the GitHub account
associated with your contributions.
