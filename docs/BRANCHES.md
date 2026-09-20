# Minecraft branches

Each branch has one Minecraft version, ordinary editable Java sources, and its own
dependency pins in `gradle.properties`. Switch branches to change Minecraft versions.
Run `./gradlew clean` after switching, then reload the Gradle project in the IDE.

| Branch | Loaders | Gradle JVM | Minecraft Java |
| --- | --- | --- | --- |
| `1.19.2` | Fabric, Forge | 21 | 17 |
| `1.20.1` | Fabric, Forge | 21 | 17 |
| `1.21.1` | Fabric, Forge, NeoForge | 21 | 21 |
| `1.21.11` | Fabric, NeoForge | 21 | 21 |
| `26.1.2` | Fabric, NeoForge | 25 | 25 |
| `26.2` | Fabric, NeoForge | 25 | 25 |
| `26.3` | Fabric, NeoForge | 25 | 25 |

Forge 1.21.1 uses ForgeGradle 7. AMECS integration is available through 26.2;
26.3 currently uses vanilla Fabric key bindings because AMECS has no SDL port.

Apply shared fixes across branches as normal commits and adapt them to each branch's
Minecraft API. There are no version overlays or generated Java source rewrites.
The `api`, `test`, and loader test-mod source sets serve their usual roles within
one Minecraft version.

Every branch runs the same verification and publishing flow. See [releasing](RELEASING.md)
to coordinate a release across all seven branches.
