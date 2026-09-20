# Releasing MezzConfig GUI

Release each Minecraft branch independently. Maven versions, platform release
versions, and Git tags use `mc<minecraft>-<mod-version>`, for example `mc26.3-0.5.2`.
`specificationVersion` and loader metadata keep the numeric mod version (`0.5.2`)
so API and loader dependency ranges continue to work.

1. Update `specificationVersion` on the branch being released. Increase the minor
   version for public API additions and the patch version for fixes or dependency
   updates. Preserve API compatibility; breaking API changes require a major bump
   and are only allowed with a Minecraft update.
   Keep `apiBaselineVersion` pinned to the first published API's exact Maven version:
   `0.4.0` on 1.21.1 and `0.5.1` on 26.3. A branch with no published API starts with
   its first full release version as the baseline. Never reset a published baseline.
2. Commit and push that branch, then wait for its CI checks to pass.
3. Tag the verified commit `mc<minecraft>-<mod-version>`. This branch uses
   `mc26.3-0.5.2`. Push the intended tag.
4. Trigger or rescan the corresponding Jenkins branch job.

A fix on one branch does not require releases on unaffected branches. Backport and
validate shared fixes separately, retaining each target's own versions and pins.
Adding support for an older Minecraft version needs no global version allocation.
Existing release tags keep their original targets and are never moved or reused.

Each job accepts only the new tag format for its own branch and skips tags already
published successfully. Ordinary branch and pull request builds do not publish.
To validate a release locally, pass `-PRELEASE_VERSION=mc26.3-0.5.2` to Gradle.
Untagged development builds append the build number, such as `mc26.3-0.5.2.9999`.

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
