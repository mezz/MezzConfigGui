pluginManagement {
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loomVersion").get()
        id("net.neoforged.moddev.legacyforge") version providers.gradleProperty("moddevVersion").get()
        id("me.modmuss50.mod-publish-plugin") version providers.gradleProperty("publishPluginVersion").get()
    }
	repositories {
		fun exclusiveMaven(url: String, filter: Action<InclusiveRepositoryContentDescriptor>) =
			exclusiveContent {
				forRepository { maven(url) }
				filter(filter)
			}
		exclusiveMaven("https://maven.parchmentmc.org") {
			includeGroupByRegex("org\\.parchmentmc.*")
		}
		exclusiveMaven("https://maven.fabricmc.net/") {
			includeGroupByRegex("net\\.fabricmc.*")
		}
		exclusiveMaven("https://maven.neoforged.net/releases") {
			includeGroupByRegex("net\\.neoforged.*")
			includeGroup("codechicken")
			includeGroup("net.covers1624")
		}
		exclusiveMaven("https://maven.blamejared.com/") {
			includeGroup("net.mezzdev.java-formatting")
			includeModule("net.mezzdev.gradle", "JavaFormatting")
		}
		gradlePluginPortal()
	}
}

rootProject.name = "MezzConfigGui"
include("Changelog", "Common", "Fabric", "Forge")
