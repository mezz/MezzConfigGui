package net.mezzdev.config.gui.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.loading.moddiscovery.NightConfigWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeConfigIntegrationTest {
	@TempDir
	Path directory;

	@BeforeEach
	void initializeForgeConfigDirectory() {
		FMLPaths.loadAbsolutePaths(directory);
		FMLConfig.load();
	}

	@Test
	void allFilesOfTheSameTypeAreDiscoveredWithTheirNestedPaths() throws ReflectiveOperationException {
		ModContainer container = new TestContainer();
		for (String name : List.of("zebra", "animals")) {
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			builder.define("general." + name + ".enabled", true);
			ModConfig config = new ModConfig(ModConfig.Type.CLIENT, builder.build(), container, container.getModId() + "-" + name + ".toml");
			// Attach real parsed data without dispatching events that require the running mod loader.
			var loadConfig = ModConfig.class.getDeclaredMethod("setConfigData", CommentedConfig.class);
			loadConfig.setAccessible(true);
			loadConfig.invoke(config, new TomlParser().parse("[general." + name + "]\nenabled = true\n"));
		}
		ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
		server.define("enabled", true);
		new ModConfig(ModConfig.Type.SERVER, server.build(), container);

		var categories = ForgeConfigScreenConfigs.getCategories(container);

		assertEquals(2, categories.size(), "Every loaded file must be included; unloaded Server configs must be omitted");
		assertTrue(categories.get(0).name().endsWith("-animals.toml"));
		assertTrue(categories.get(1).name().endsWith("-zebra.toml"));
		assertEquals(List.of("general", "animals"), ConfigValueCategoryPath.getCategories(categories.get(0).configValues().get(0))
			.stream().map(ConfigValueCategoryPath.Category::name).toList());
	}

	@Test
	@SuppressWarnings("unchecked")
	void nativeEditsPersistToTomlAndRejectInvalidValuesWithoutNotifying() throws Exception {
		ModContainer container = new TestContainer();
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		var nativeValue = builder.defineInRange("general.limit", 4, 0, 32);
		ForgeConfigSpec spec = builder.build();
		ModConfig config = new ModConfig(ModConfig.Type.CLIENT, spec, container);
		Path path = directory.resolve(config.getFileName());
		Files.writeString(path, "[general]\nlimit = 4\n");
		try (CommentedFileConfig file = CommentedFileConfig.builder(path).sync().build()) {
			file.load();
			spec.setConfig(file);
			ForgeConfigSpec.ValueSpec valueSpec = spec.getSpec().get(nativeValue.getPath());
			IConfigScreenValue<Integer> value = (IConfigScreenValue<Integer>) ForgeConfigValueFactory.create(container.getModId(), config, spec, nativeValue, valueSpec).orElseThrow();
			List<Integer> notifications = new ArrayList<>();
			Runnable removeListener = value.addListener(notifications::add);

			assertTrue(value.set(12));
			assertFalse(value.set(12));
			assertThrows(IllegalArgumentException.class, () -> value.set(33));
			assertThrows(IllegalArgumentException.class, () -> value.set(null));
			removeListener.run();
			assertTrue(value.set(16));

			try (var reader = Files.newBufferedReader(path)) {
				assertEquals(16, new TomlParser().parse(reader).<Integer>get("general.limit"));
			}
			assertEquals(List.of(12), notifications);
			assertEquals(16, nativeValue.get());
		}
	}

	/** Supplies mod identity to Forge's real config registry without starting the mod loader. */
	private static final class TestContainer extends ModContainer {
		TestContainer() {
			super(new ModInfo(null, new NightConfigWrapper(new TomlParser().parse(
				"modId = \"native_test_" + UUID.randomUUID().toString().replace("-", "") + "\"\ndisplayTest = \"NONE\"\n"))));
		}

		@Override
		public boolean matches(Object mod) {
			return mod == this;
		}

		@Override
		public Object getMod() {
			return this;
		}
	}
}
