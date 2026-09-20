// Load the real Jenkins helper methods without running the pipeline or its shell steps.
def releaseCommit = '1111111111111111111111111111111111111111'
def previousReleaseCommit = '2222222222222222222222222222222222222222'
def previousSuccessfulCommit = '3333333333333333333333333333333333333333'
def env = [
    BRANCH_NAME: '26.2',
    CHANGE_ID: null,
    RELEASE_TAG: 'mc26.2-0.3.0',
    GIT_COMMIT: releaseCommit,
    GIT_PREVIOUS_SUCCESSFUL_COMMIT: previousSuccessfulCommit
]
def currentBuild = [previousBuild: null]
def binding = new Binding([
    env: env,
    currentBuild: currentBuild,
    pipeline: { Closure ignored -> },
    modernSCM: { Map scm -> scm },
    library: { Map ignored -> },
    error: { String message -> throw new IllegalStateException(message) }
])
def jenkins = new GroovyShell(binding).parse(pipelineFile)
jenkins.run()

assert jenkins.shouldPublishRelease()
assert jenkins.getReleaseVersion() == 'mc26.2-0.3.0'
assert jenkins.getReleaseGradleArgs() == "-PRELEASE_VERSION='mc26.2-0.3.0'"

env.RELEASE_TAG = ''
assert !jenkins.shouldPublishRelease()
assert jenkins.getReleaseGradleArgs() == ''
try {
    jenkins.getReleaseVersion()
    assert false: 'Missing release tags must be rejected'
} catch (IllegalStateException expected) {
    assert expected.message.contains('No release tag')
}

env.RELEASE_TAG = 'mc26.2-0.3.0'
env.BRANCH_NAME = 'feature/config-screen'
assert !jenkins.shouldPublishRelease()
env.BRANCH_NAME = '26.2'
env.CHANGE_ID = '123'
assert !jenkins.shouldPublishRelease()
env.CHANGE_ID = null

// A successful branch build before the tag is pushed must not suppress its release.
currentBuild.previousBuild = [result: 'SUCCESS', description: '', previousBuild: null]
assert jenkins.shouldPublishRelease()

// Search beyond intervening builds for the completed release of this exact tag.
currentBuild.previousBuild.previousBuild = [
    result: 'SUCCESS',
    description: jenkins.releaseMarker('mc26.2-0.3.0'),
    previousBuild: null
]
assert !jenkins.shouldPublishRelease()
assert jenkins.findPreviousReleaseCommit() == previousSuccessfulCommit

// New release records also retain the source commit used by the notifier.
def completedDescription = jenkins.completedReleaseDescription('mc26.2-0.3.0', previousReleaseCommit)
assert jenkins.buildDescriptionMarkers(completedDescription) == [
    jenkins.releaseMarker('mc26.2-0.3.0'),
    jenkins.notifierSourceMarker(previousReleaseCommit)
]
currentBuild.previousBuild.previousBuild.description = completedDescription
assert jenkins.wasReleasePublished('mc26.2-0.3.0'): "currentBuild=${currentBuild.inspect()}"
assert !jenkins.shouldPublishRelease()
assert jenkins.findPreviousReleaseCommit() == previousReleaseCommit
env.RELEASE_TAG = 'mc26.2-0.3.1'
assert jenkins.shouldPublishRelease()
env.RELEASE_TAG = 'mc26.2-0.3.0'

// Failed releases may be retried; only a successful release record is authoritative.
currentBuild.previousBuild.previousBuild.result = 'FAILURE'
assert jenkins.shouldPublishRelease()
currentBuild.previousBuild.previousBuild.description = null
assert jenkins.shouldPublishRelease()
assert jenkins.findPreviousReleaseCommit() == previousSuccessfulCommit

env.GIT_PREVIOUS_SUCCESSFUL_COMMIT = null
assert jenkins.findPreviousReleaseCommit() == releaseCommit

env.RELEASE_TAG = 'refs/tags/mc26.2-0.3.0'
assert jenkins.getReleaseVersion() == 'mc26.2-0.3.0'
assert jenkins.shellQuote("path with 'quotes'") == "'path with '\"'\"'quotes'\"'\"''"

println 'Jenkins release policy passed: new tags, ordinary builds, branches, pull requests, completed releases, and retries.'

env.RELEASE_TAG = 'mc1.21.1-0.3.0'
assert !jenkins.shouldPublishRelease()
try {
    jenkins.getReleaseVersion()
    assert false: 'A tag from another Minecraft branch must be rejected'
} catch (IllegalStateException expected) {
    assert expected.message.contains('Could not determine release version')
}
