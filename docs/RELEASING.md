# Releasing MezzConfig GUI

Jenkins builds and publishes each Minecraft branch separately. Release tags include
the Minecraft version so every branch can release the same MezzConfig GUI version.

1. Update `specificationVersion` in `gradle.properties` on each branch being released.
   Keep `apiBaselineVersion` pinned to the first API release: 0.4.0 on 1.21.1 and
   0.5.0 on the six new branches.
2. Commit and push each branch, then wait for its CI checks to pass.
3. Tag each verified commit `mc<minecraft>/v<version>`. This branch uses
   `mc1.21.11/v0.5.0` for version 0.5.0. Push the intended tags.
4. Trigger or rescan the corresponding Jenkins branch jobs.

To coordinate all seven releases, use the same `specificationVersion` on all seven
[branches](BRANCHES.md), verify them all, and then push their seven scoped tags.
Each job accepts only tags for its own branch and skips tags already published
successfully. Ordinary branch and pull request builds do not publish.

This branch needs a Jenkins `jdk-21` installation.
Jenkins validates complete Maven jars and platform release metadata, then publishes
Maven artifacts and beta files to CurseForge and Modrinth. Changelogs come from Git
history since this Minecraft branch's previous release tag.

Before retrying a partially failed release, check which destinations already
received files. Keep successful release build records when pruning Jenkins history
so completed releases are not published again.
