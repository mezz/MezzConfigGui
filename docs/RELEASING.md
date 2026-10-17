# Releasing MezzConfig GUI

Release each Minecraft branch independently. Maven versions, platform release
versions, and Git tags use `mc<minecraft>-<mod-version>`, for example `mc26.3-0.6.0`.
`specificationVersion` and loader metadata keep the numeric mod version (`0.6.0`)
so API and loader dependency ranges continue to work.

1. Update `specificationVersion` on the branch being released. For beta `0.x.y`
   versions, increase only `y` for backward-compatible changes, including public
   API additions, fixes, and dependency updates. For breaking API changes,
   increase `x` and reset `y` to zero.
   Set `apiBaselineVersion` to the first API release in each compatible version
   line, using its exact Maven version: `mc26.3-0.6.0` for the `0.6.x` line.
   Advance the baseline only on a `0.x` minor bump or a major bump for `1.0.0`
   and later. Keep it pinned for backward-compatible releases.
   A missing baseline is allowed only while building that baseline release;
   later releases require it and fail on API incompatibilities.
2. Commit and push that branch, then wait for its CI checks to pass.
3. Tag the verified commit `mc<minecraft>-<mod-version>`. This branch uses
   `mc26.3-0.6.0`. Push the intended tag.
4. Trigger or rescan the corresponding Jenkins branch job.

A fix on one branch does not require releases on unaffected branches. Backport and
validate shared fixes separately, retaining each target's own versions and pins.
Adding support for an older Minecraft version needs no global version allocation.
Existing release tags keep their original targets and are never moved or reused.

Each job accepts only the new tag format for its own branch and skips tags already
published successfully. Ordinary branch and pull request builds do not publish.
To validate a release locally, pass `-PRELEASE_VERSION=mc26.3-0.6.0` to Gradle.
Untagged development builds append the build number, such as `mc26.3-0.6.0.9999`.

This branch needs a Jenkins `jdk-25` installation.
Jenkins validates complete Maven jars and platform release metadata, then publishes
Maven artifacts and beta files to CurseForge and Modrinth. Changelogs come from Git
history since this Minecraft branch's previous release tag, including legacy tags.
After publishing, Jenkins schedules released-issue and pull-request updates and sends
a Discord build summary through the shared notifier worker. See
[Jenkins release notifications](../.jenkins/releaseComments.md) for the integration
and worker setup.

Before retrying a partially failed release, check which destinations already
received files. Keep successful release build records when pruning Jenkins history
so completed releases are not published again.
