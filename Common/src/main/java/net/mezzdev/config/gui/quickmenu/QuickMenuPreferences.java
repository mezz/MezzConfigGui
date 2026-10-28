package net.mezzdev.config.gui.quickmenu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

/** Saved choices owned by the GUI, independent of the settings being edited. */
final class QuickMenuPreferences implements IConfigScreenValue<List<String>> {
	private static final Gson GSON = new Gson();
	private static final Map<Path, SavedSelection> LOADED = new HashMap<>();
	private final Path file;
	private final List<String> defaults;
	private final List<String> available;
	private final SavedSelection saved;

	QuickMenuPreferences(Path directory, String modId, List<String> defaults, List<String> additional) {
		this.defaults = List.copyOf(defaults);
		this.available = java.util.stream.Stream.concat(defaults.stream(), additional.stream()).toList();
		this.file = directory.resolve(key(modId, defaults, additional) + ".json").toAbsolutePath().normalize();
		this.saved = LOADED.computeIfAbsent(file, ignored -> new SavedSelection(read()));
	}

	private static String key(String modId, List<String> defaults, List<String> additional) {
		try {
			byte[] bytes = GSON.toJson(List.of(modId, defaults, additional)).getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getName() {
		return "quickMenuSelection";
	}

	@Override
	public String getLocalizationKey() {
		return "mezz_config_gui.quickMenu.customize";
	}

	@Override
	public List<String> getValue() {
		return saved.value;
	}

	private List<String> read() {
		if (!Files.exists(file)) {
			return defaults;
		}
		try {
			List<String> saved = GSON.fromJson(Files.readString(file), new TypeToken<List<String>>() {}.getType());
			if (saved == null) {
				throw new IllegalStateException("Invalid quick menu preferences: " + file);
			}
			return saved.stream().filter(available::contains).distinct().toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read quick menu preferences: " + file, e);
		}
	}

	@Override
	public List<String> getDefaultValue() {
		return defaults;
	}

	@Override
	public boolean set(List<String> value) {
		List<String> next = List.copyOf(value);
		if (!getEditorMetadata().isValid(next)) {
			throw new IllegalArgumentException("Invalid quick menu selection: " + value);
		}
		if (next.equals(getValue())) {
			return false;
		}
		try {
			Files.createDirectories(file.getParent());
			Path temporary = Files.createTempFile(file.getParent(), "selection-", ".tmp");
			try {
				Files.writeString(temporary, GSON.toJson(next));
				try {
					Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(temporary);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Could not save quick menu preferences: " + file, e);
		}
		saved.value = next;
		List.copyOf(saved.listeners).forEach(listener -> listener.accept(next));
		return true;
	}

	@Override
	public Runnable addListener(Consumer<List<String>> listener) {
		saved.listeners.add(listener);
		return () -> saved.listeners.remove(listener);
	}

	@Override
	public IConfigValueEditorMetadata<List<String>> getEditorMetadata() {
		return new IConfigValueEditorMetadata<>() {
			@Override
			public ConfigValueEditorType<List<String>> getEditorType() {
				return ConfigValueEditorTypes.getList();
			}

			@Override
			public boolean isValid(List<String> value) {
				return available.containsAll(value) && value.stream().distinct().count() == value.size();
			}

			@Override
			public Optional<List<List<String>>> getAllValidValues() {
				return Optional.empty();
			}

			@Override
			public Component getLocalizedValueName(String key, List<String> value) {
				return Component.literal(String.join(", ", value));
			}
		};
	}

	private static final class SavedSelection {
		private List<String> value;
		private final List<Consumer<List<String>>> listeners = new ArrayList<>();

		private SavedSelection(List<String> value) {
			this.value = value;
		}
	}
}
