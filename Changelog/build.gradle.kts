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

// Exclude the version being built so tagged releases and their preceding CI
// builds use the same range. An empty result includes all history for a first release.
val previousRelease = providers.exec {
    workingDir(rootProject.rootDir)
    commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "mc$minecraftVersion/v[0-9]*.[0-9]*.[0-9]*",
        "--exclude", "mc$minecraftVersion/v$specificationVersion", "--match", "v[0-9]*.[0-9]*.[0-9]*", "--exclude", "v$specificationVersion", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

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
