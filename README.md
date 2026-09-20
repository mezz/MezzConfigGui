[![CI](https://github.com/mezz/MezzConfigGui/actions/workflows/ci.yml/badge.svg?branch=1.21.1)](https://github.com/mezz/MezzConfigGui/actions/workflows/ci.yml)
[![CurseForge downloads](https://cf.way2muchnoise.eu/full_1700987_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/mezzconfiggui)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/BOHUKqOz?logo=modrinth&label=Modrinth)](https://modrinth.com/project/mezzconfiggui)
[![Discord](https://img.shields.io/discord/358816755646332941?color=5865F2&logo=discord&logoColor=white&label=Discord)](https://discord.gg/sCQcWU2)
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

# MezzConfig GUI

MezzConfig GUI is a client-side config screen for Minecraft mods.
It can discover config schemas registered with [MezzConfig](https://github.com/mezz/MezzConfig), supplies editors for common value types, and supports additional screen customization through its API.

## Developing with MezzConfig GUI

Pack authors can [customize mod order and visibility](docs/PACK_CONFIGURATION.md).
See [server configuration support](docs/SERVER_CONFIGS.md) for the difference
between MezzConfig remote editing and native NeoForge configs.

See [Dependency Setup](docs/DEPENDENCY_SETUP.md) for loader-specific Gradle examples.
This branch builds Minecraft 1.21.1 for Fabric, Forge, and NeoForge.
See [Minecraft branches](docs/BRANCHES.md) for the other supported versions.
Run Gradle with Java 21.
Forge uses ForgeGradle 7. Run `./gradlew :Forge:runClient` or
`./gradlew :Forge:runServer` for development.

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
