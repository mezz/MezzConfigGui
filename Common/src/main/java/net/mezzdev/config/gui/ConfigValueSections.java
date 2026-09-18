package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Native section metadata, kept on a value's identity through GUI customization wrappers.
 */
public interface ConfigValueSections {
	String getSectionCategoryName();

	List<Section> getSections();

	static List<Section> getSections(IConfigScreenValue<?> value) {
		if (value.getIdentityKey() instanceof ConfigValueSections sections) {
			return sections.getSections();
		}
		return List.of();
	}

	static Component getContextualName(IConfigScreenValue<?> value, Component name) {
		var result = Component.empty();
		for (Section section : getSections(value)) {
			result.append(section.title()).append(" › ");
		}
		return result.append(name);
	}

	record Section(String name, Component title, Component description) {
	}
}
