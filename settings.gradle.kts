pluginManagement {
	repositories {
		fun exclusiveMaven(url: String, filter: Action<InclusiveRepositoryContentDescriptor>) =
			exclusiveContent {
				forRepository { maven(url) }
				filter(filter)
			}
		maven("https://maven.minecraftforge.net") {
			content {
				includeGroupByRegex("net\\.minecraftforge.*")
			}
		}
		exclusiveMaven("https://maven.parchmentmc.org") {
			includeGroupByRegex("org\\.parchmentmc.*")
		}
		exclusiveMaven("https://maven.fabricmc.net/") {
			includeGroupByRegex("net\\.fabricmc.*")
			includeGroup("fabric-loom")
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
	resolutionStrategy {
		eachPlugin {
			if (requested.id.id == "net.minecraftforge.gradle") {
				useModule("${requested.id}:ForgeGradle:${requested.version}")
			}
		}
	}
}

val minecraftVersion: String by settings

rootProject.name = "MezzConfigGui"

include(
	"Common", "CommonApi",
	"Fabric", "Forge", "NeoForge",
	"FabricMezzConfigDefaultsTest",
	"FabricMezzConfigCustomTest",
	"NeoForgeNativeDefaultsTest",
	"NeoForgeNativeCustomTest"
)

project(":CommonApi").name = "mezz_config_gui-${minecraftVersion}-config-gui-api"
project(":Common").name = "mezz_config_gui-${minecraftVersion}-config-gui"
project(":Fabric").name = "mezz_config_gui-${minecraftVersion}-fabric"
project(":Forge").name = "mezz_config_gui-${minecraftVersion}-forge"
project(":NeoForge").name = "mezz_config_gui-${minecraftVersion}-neoforge"
