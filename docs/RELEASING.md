# Releasing MezzConfig GUI

Jenkins publishes releases from new version tags on the `1.21.1` branch.

1. Update `specificationVersion` in [gradle.properties](../gradle.properties).
   Keep `apiBaselineVersion` pinned to the first public API release.
2. Commit and push the branch, then wait for CI to pass.
3. Tag that commit with the matching version, such as `v0.4.0` for `0.4.0`,
   and push the tag.
4. Trigger or rescan the Jenkins branch job.

Jenkins publishes Maven artifacts and beta files for Fabric, Forge, and NeoForge
on CurseForge and Modrinth. Changelogs are generated automatically from Git history.

Before retrying a partially failed release, check which destinations already
received files. Keep successful release build records when pruning Jenkins history
so completed releases are not published again.

See the [Jenkins pipeline](../.jenkins/Jenkinsfile) and
[build configuration](../build.gradle.kts) for implementation details.
