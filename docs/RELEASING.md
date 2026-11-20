# Releasing MezzConfig GUI

Release each Minecraft branch independently. Maven versions, platform release
versions, and Git tags use `mc<minecraft>-<mod-version>`, for example `mc1.20.1-0.7.2`.
`specificationVersion` and loader metadata keep the numeric mod version (`0.7.2`)
so API and loader dependency ranges continue to work.

1. Update `specificationVersion` on the branch being released.
   Increase the minor version and reset the patch to zero in the commit that
   adds or deprecates public API, including on `0.x` branches. Increase the
   patch version for backward-compatible fixes. Use a major version bump for
   incompatible API changes after 1.0, or a minor bump during `0.x` development.
   Use the introducing version in new `@since` and deprecation annotations.

   Set `apiBaselineVersion` to the first published API release in the compatible
   line, using its exact Maven version. This branch uses `mc1.20.1-0.5.1` as its API baseline.
   Keep this baseline for compatible releases, including minor releases that add API.
   Set a new baseline when introducing an incompatible API line. The first release
   establishes that baseline and may build before the baseline artifact is published.
2. Commit and push that branch, then wait for its CI checks to pass.
3. Tag the verified commit `mc<minecraft>-<mod-version>`. This branch uses
   `mc1.20.1-0.7.2`. Push the intended tag.
4. Trigger or rescan the corresponding Jenkins branch job.

A fix on one branch does not require releases on unaffected branches. Backport and
validate shared fixes separately, retaining each target's own versions and pins.
Adding support for an older Minecraft version needs no global version allocation.
Existing release tags keep their original targets and are never moved or reused.

Each job accepts only the new tag format for its own branch and skips tags already
published successfully. Ordinary branch and pull request builds do not publish.
To validate a release locally, pass `-PRELEASE_VERSION=mc1.20.1-0.7.2` to Gradle.
Untagged development builds append the build number, such as `mc1.20.1-0.7.2.9999`.

This branch needs a Jenkins `jdk-21` installation and an installed Java 17 toolchain.
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
