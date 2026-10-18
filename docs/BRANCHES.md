# Minecraft branches

Each branch has one Minecraft version, ordinary editable Java sources, and its own
dependency pins in `gradle.properties`. Switch branches to change Minecraft versions.
Run `./gradlew clean` after switching, then reload the Gradle project in the IDE.

Supported Minecraft versions match
[JEI](https://github.com/mezz/JustEnoughItems#latest-versions), except that
MezzConfig GUI does not support Minecraft 1.12.2.

| Branch | Loaders | Gradle JVM | Minecraft Java |
| --- | --- | --- | --- |
| `1.19.2` | Fabric, Forge | 21 | 17 |
| `1.20.1` | Fabric, Forge | 21 | 17 |
| `1.21.1` | Fabric, Forge, NeoForge | 21 | 21 |
| `1.21.11` | Fabric, NeoForge | 21 | 21 |
| `26.1.2` | Fabric, NeoForge | 25 | 25 |
| `26.3` | Fabric, NeoForge | 25 | 25 |

Forge 1.21.1 uses ForgeGradle 7. AMECS integration is available through 26.1.2.
26.3 currently uses vanilla Fabric key bindings because AMECS has no SDL port.

Apply shared fixes across branches as normal commits and adapt them to each branch's
Minecraft API. There are no version overlays or generated Java source rewrites.
The `api`, `test`, and loader test-mod source sets serve their usual roles within
one Minecraft version.

Release identifiers put Minecraft first: `mc1.21.1-0.5.2`. Each branch advances its
mod version independently; unaffected branches do not need a matching release.
The Minecraft prefix identifies compatibility, so adding an older Minecraft branch
does not require renumbering any existing release line. Start a new branch from
the mod version it ports, then advance it independently.

Backport shared fixes as branch-level changes. Preserve the target branch's loader
and Java requirements. Advance its version and API baseline according to
[the release policy](RELEASING.md). Consult each branch's changelog for the
changes in its releases.

Every branch runs the same verification and publishing flow. See [releasing](RELEASING.md)
for the branch-specific release process.
