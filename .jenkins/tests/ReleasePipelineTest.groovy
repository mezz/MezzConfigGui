// Load the real Jenkins helper methods without running the pipeline or its shell steps.
def env = [BRANCH_NAME: '1.21.11', CHANGE_ID: null, RELEASE_TAG: 'mc1.21.11/v0.3.0']
def currentBuild = [previousBuild: null]
def binding = new Binding([
    env: env,
    currentBuild: currentBuild,
    pipeline: { Closure ignored -> },
    error: { String message -> throw new IllegalStateException(message) }
])
def jenkins = new GroovyShell(binding).parse(pipelineFile)
jenkins.run()

assert jenkins.shouldPublishRelease()
assert jenkins.getReleaseVersion() == '0.3.0'
assert jenkins.getReleaseGradleArgs() == "-PRELEASE_VERSION='0.3.0'"

env.RELEASE_TAG = ''
assert !jenkins.shouldPublishRelease()
assert jenkins.getReleaseGradleArgs() == ''
try {
    jenkins.getReleaseVersion()
    assert false: 'Missing release tags must be rejected'
} catch (IllegalStateException expected) {
    assert expected.message.contains('No release tag')
}

env.RELEASE_TAG = 'mc1.21.11/v0.3.0'
env.BRANCH_NAME = 'feature/config-screen'
assert !jenkins.shouldPublishRelease()
env.BRANCH_NAME = '1.21.11'
env.CHANGE_ID = '123'
assert !jenkins.shouldPublishRelease()
env.CHANGE_ID = null

// A successful branch build before the tag is pushed must not suppress its release.
currentBuild.previousBuild = [result: 'SUCCESS', description: '', previousBuild: null]
assert jenkins.shouldPublishRelease()

// Search beyond intervening builds for the completed release of this exact tag.
currentBuild.previousBuild.previousBuild = [
    result: 'SUCCESS',
    description: jenkins.releaseMarker('mc1.21.11/v0.3.0'),
    previousBuild: null
]
assert !jenkins.shouldPublishRelease()
env.RELEASE_TAG = 'mc1.21.11/v0.3.1'
assert jenkins.shouldPublishRelease()
env.RELEASE_TAG = 'mc1.21.11/v0.3.0'

// Failed releases may be retried; only a successful release record is authoritative.
currentBuild.previousBuild.previousBuild.result = 'FAILURE'
assert jenkins.shouldPublishRelease()
currentBuild.previousBuild.previousBuild.description = null
assert jenkins.shouldPublishRelease()

env.RELEASE_TAG = 'refs/tags/mc1.21.11/v0.3.0'
assert jenkins.getReleaseVersion() == '0.3.0'
assert jenkins.shellQuote("path with 'quotes'") == "'path with '\"'\"'quotes'\"'\"''"

println 'Jenkins release policy passed: new tags, ordinary builds, branches, pull requests, completed releases, and retries.'

env.RELEASE_TAG = 'mc1.21.1/v0.3.0'
assert !jenkins.shouldPublishRelease()
try {
    jenkins.getReleaseVersion()
    assert false: 'A tag from another Minecraft branch must be rejected'
} catch (IllegalStateException expected) {
    assert expected.message.contains('Could not determine release version')
}
