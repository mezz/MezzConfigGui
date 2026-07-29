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
			includeGroup("net.fabricmc")
			includeGroup("fabric-loom")
		}
		exclusiveMaven("https://maven.neoforged.net/releases") {
			includeGroupByRegex("net\\.neoforged.*")
			includeGroup("codechicken")
			includeGroup("net.covers1624")
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
val mezzConfigLocalPath: String by settings

rootProject.name = "MezzConfigGui"

includeBuild(mezzConfigLocalPath) {
	dependencySubstitution {
		substitute(module("net.mezzdev.config:CommonApi")).using(project(":CommonApi"))
		substitute(module("net.mezzdev.config:Common")).using(project(":Common"))
		substitute(module("net.mezzdev.config:FabricApi")).using(project(":FabricApi"))
		substitute(module("net.mezzdev.config:ForgeApi")).using(project(":ForgeApi"))
		substitute(module("net.mezzdev.config:NeoForgeApi")).using(project(":NeoForgeApi"))
		substitute(module("net.mezzdev.config:mezz_config-${minecraftVersion}-config-api")).using(project(":CommonApi"))
		substitute(module("net.mezzdev.config:mezz_config-${minecraftVersion}-config")).using(project(":Common"))
		substitute(module("net.mezzdev.config:mezz_config_api-${minecraftVersion}-fabric")).using(project(":FabricApi"))
		substitute(module("net.mezzdev.config:mezz_config_api-${minecraftVersion}-forge")).using(project(":ForgeApi"))
		substitute(module("net.mezzdev.config:mezz_config_api-${minecraftVersion}-neoforge")).using(project(":NeoForgeApi"))
		substitute(module("net.mezzdev.config:mezz_config-${minecraftVersion}-fabric")).using(project(":Fabric"))
		substitute(module("net.mezzdev.config:mezz_config-${minecraftVersion}-forge")).using(project(":Forge"))
		substitute(module("net.mezzdev.config:mezz_config-${minecraftVersion}-neoforge")).using(project(":NeoForge"))
	}
}

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
