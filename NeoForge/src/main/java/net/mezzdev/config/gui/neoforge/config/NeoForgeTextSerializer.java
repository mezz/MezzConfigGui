package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collection;
import java.util.Optional;

abstract class NeoForgeTextSerializer<T> implements IConfigValueEditorSerializer<T> {
	private final ModConfigSpec.ValueSpec valueSpec;
	private final String validValuesDescription;

	protected NeoForgeTextSerializer(ModConfigSpec.ValueSpec valueSpec, String validValuesDescription) {
		this.valueSpec = valueSpec;
		this.validValuesDescription = validValuesDescription;
	}

	@Override
	public boolean isValid(T value) {
		if (value == null) {
			return false;
		}
		try {
			return valueSpec.test(value);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	@Override
	public Optional<Collection<T>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, T value) {
		return Component.literal(serialize(value));
	}

	@Override
	public String getValidValuesDescription() {
		return validValuesDescription;
	}

	@Override
	public ConfigValueEditorType<T> getEditorType() {
		return ConfigValueEditorTypes.getText();
	}

}
