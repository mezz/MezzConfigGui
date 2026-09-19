# Releasing MezzConfig GUI

GitHub Actions validates pushes and pull requests. Jenkins validates builds and
publishes Maven artifacts, CurseForge files, and Modrinth versions from the
`1.21.1` branch when a new release tag points at the checked-out commit.

## Prepare a release

1. Set `specificationVersion` in `gradle.properties` to the version being released.
2. Commit the changes and push the `1.21.1` branch so CI can validate it.
3. Create and push a `v<major>.<minor>.<patch>` tag on that commit, for example
   `v0.3.0` for `specificationVersion=0.3.0`.
4. Trigger or rescan the Jenkins branch job after pushing the tag.

Jenkins fetches tags and selects a release tag pointing exactly at `HEAD`. It
checks that the tag version matches `specificationVersion` before building.
Tags on older commits do not publish the current build. Builds without a release
tag use `<specificationVersion>.<BUILD_NUMBER>` and perform validation only.

After validation, Jenkins runs `publishMavenRelease`, then publishes all three
loaders to CurseForge and Modrinth as beta files. The Maven release task includes
the public API, the shared implementation required by the platform Maven jars,
and the Fabric, Forge, and NeoForge artifacts. Changelogs are generated
automatically from Git history.

## Repeated builds

After all publishing stages succeed, Jenkins records the tag in the build's
description as `mezzconfiggui-release:v1:<tag>`. Later builds of that tag still
validate, but skip publishing when that successful release record is present in
the job's build history. Concurrent builds of the branch job are disabled so
they cannot both start publishing the same tag.

An ordinary successful build before the tag is created does not mark the version
as released. A failed release is also not marked as complete; before retrying a
partially published release, check which destinations already received files.
Keep successful release build records when pruning Jenkins history.

## Build and publishing tasks

- `build` and `check` run project verification, including publishing artifacts to
  `build/publication-validation` for local verification.
- `publishMods -PpublishDryRun=true` checks platform metadata without uploading.
- `publishMavenRelease` targets the named release repository and requires
  `DEPLOY_DIR` and a release version matching `specificationVersion`.
- Real CurseForge and Modrinth publishing also requires that matching release
  version and `-PpublishDryRun=false`.

Jenkins supplies `RELEASE_VERSION` from the detected tag. Its Maven destination
comes from `MAVEN_DEPLOY_DIR`, `local_maven_url`, or `local_maven`; platform tokens
come from the existing Jenkins credentials.
