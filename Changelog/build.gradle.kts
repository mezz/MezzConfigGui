import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import se.bjurr.gitchangelog.api.InclusivenessStrategy
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.30.0")
    }
}

plugins {
    base
    id("se.bjurr.gitchangelog.git-changelog-gradle-plugin") version("3.1.2")
}

val specificationVersion: String by extra
val minecraftVersion: String by extra
val changelogUntaggedName = "Current release $specificationVersion"

// Match complete tag names so legacy v* tags cannot also match another Minecraft
// version's v*/mc* tags. Keep both legacy formats as changelog boundaries.
val previousReleaseTags = providers.exec {
    workingDir(rootProject.rootDir)
    commandLine("git", "tag", "--list")
}.standardOutput.asText.map { output ->
    output.lineSequence().filter { tag ->
        val versionTag = if (tag.startsWith("mc$minecraftVersion/")) {
            tag.removePrefix("mc$minecraftVersion/")
        } else {
            tag.removeSuffix("/mc$minecraftVersion")
        }
        Regex("v[0-9]+\\.[0-9]+\\.[0-9]+").matches(versionTag) && versionTag != "v$specificationVersion"
    }.toList()
}

// Exclude the version being built so tagged releases and their preceding CI
// builds use the same range. An empty result includes all history for a first release.
val previousRelease = previousReleaseTags.map { tags ->
    if (tags.isEmpty()) {
        ""
    } else {
        providers.exec {
            workingDir(rootProject.rootDir)
            commandLine(listOf("git", "describe", "--tags", "--abbrev=0") + tags.flatMap { listOf("--match", it) } + "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()
    }
}

val makeMarkdownChangelog = tasks.register<GitChangelogTask>("makeMarkdownChangelog") {
    val output = layout.buildDirectory.file("changelog.md")

    fromRepo.set(rootProject.rootDir.absolutePath)
    file.set(output.get().asFile)
    untaggedName.set(changelogUntaggedName)
    fromRevision.set(providers.gradleProperty("changelogFromRevision").orElse(previousRelease))
    fromRevisionStrategy.set(InclusivenessStrategy.EXCLUSIVE)
    toRevision.set("HEAD")
    templateContent.set(providers.fileContents(layout.projectDirectory.file("changelog-markdown.mustache")).asText)

    outputs.file(output)
}

abstract class RenderHtmlChangelog : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val markdownFile: RegularFileProperty

    @get:OutputFile
    abstract val htmlFile: RegularFileProperty

    @TaskAction
    fun render() {
        val document = Parser.builder().build().parse(markdownFile.get().asFile.readText())
        val html = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build().render(document)
        val output = htmlFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(html)
    }
}

val makeHtmlChangelog = tasks.register<RenderHtmlChangelog>("makeHtmlChangelog") {
    markdownFile.fileProvider(makeMarkdownChangelog.map { it.outputs.files.singleFile })
    htmlFile.set(layout.buildDirectory.file("changelog.html"))
}

val makeChangelog = tasks.register("makeChangelog") {
    group = "documentation"
    description = "Generates matching CurseForge HTML and Modrinth Markdown changelogs from Git history."
    dependsOn(makeHtmlChangelog, makeMarkdownChangelog)
}

tasks.assemble {
    dependsOn(makeChangelog)
}

tasks.withType<GitChangelogTask>().configureEach {
    outputs.upToDateWhen { false }
}

configurations.create("changelogHtml") {
    isCanBeConsumed = true
    isCanBeResolved = false
    isVisible = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("changelogHtml"))
    }
    outgoing.artifact(makeHtmlChangelog.map { it.outputs.files.singleFile }) {
        type = "html"
    }
}

configurations.create("changelogMarkdown") {
    isCanBeConsumed = true
    isCanBeResolved = false
    isVisible = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("changelogMarkdown"))
    }
    outgoing.artifact(makeMarkdownChangelog.map { it.outputs.files.singleFile }) {
        type = "markdown"
    }
}
