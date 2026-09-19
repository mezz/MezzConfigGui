# Automatic changelog

Changelogs are generated automatically from Git commit messages, like JEI's.
Modrinth receives the generated Markdown; CurseForge receives HTML rendered from
that same content.

The commit list includes changes since the nearest reachable
`mc<minecraft>/v<major>.<minor>.<patch>` release tag, excluding the version
being built. With no previous tag, it includes all history. Successful CI builds
do not shorten the release notes. Fetch full history and tags before generating
them; GitHub Actions and Jenkins already do this. To select an explicit starting
revision, pass `-PchangelogFromRevision=<tag-or-sha>`.

Generate and review both files locally without publishing:

```console
./gradlew :Changelog:makeChangelog
```

The outputs are `Changelog/build/changelog.md` and
`Changelog/build/changelog.html`. These paths are also consumed by the publishing
tasks and archived by Jenkins.
