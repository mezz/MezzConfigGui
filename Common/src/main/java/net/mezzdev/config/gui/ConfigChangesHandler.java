package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
interface ConfigChangesHandler {
	ConfigChangesResult applyChanges(List<ConfigValueChange<?>> changes);

	static ConfigChangesResult applySequentially(List<ConfigValueChange<?>> changes) {
		List<AppliedConfigValueChange<?>> appliedChanges = new ArrayList<>();
		ConfigValueRestartRequirement restartRequirement = ConfigValueRestartRequirement.NONE;
		for (ConfigValueChange<?> change : changes) {
			try {
				Optional<? extends AppliedConfigValueChange<?>> appliedChange = applyChange(change);
				if (appliedChange.isPresent()) {
					appliedChanges.add(appliedChange.get());
					restartRequirement = getLargerRestartRequirement(
						restartRequirement,
						change.configValue().getRestartRequirement()
					);
				}
			} catch (RuntimeException exception) {
				return new ConfigChangesResult(
					appliedChanges,
					restartRequirement,
					Optional.of(new ConfigChangeFailure(change, exception))
				);
			}
		}
		return new ConfigChangesResult(appliedChanges, restartRequirement, Optional.empty());
	}

	private static <T> Optional<AppliedConfigValueChange<T>> applyChange(ConfigValueChange<T> change) {
		T oldValue = change.configValue().getValue();
		if (!change.apply()) {
			return Optional.empty();
		}
		return Optional.of(new AppliedConfigValueChange<>(change.configValue(), oldValue, change.value()));
	}

	private static ConfigValueRestartRequirement getLargerRestartRequirement(
		ConfigValueRestartRequirement first,
		ConfigValueRestartRequirement second
	) {
		if (first == ConfigValueRestartRequirement.GAME_RESTART || second == ConfigValueRestartRequirement.GAME_RESTART) {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}
		if (first == ConfigValueRestartRequirement.WORLD_RESTART || second == ConfigValueRestartRequirement.WORLD_RESTART) {
			return ConfigValueRestartRequirement.WORLD_RESTART;
		}
		return ConfigValueRestartRequirement.NONE;
	}
}

record ConfigChangesResult(
	List<AppliedConfigValueChange<?>> appliedChanges,
	ConfigValueRestartRequirement restartRequirement,
	Optional<ConfigChangeFailure> failure
) {
	ConfigChangesResult {
		appliedChanges = List.copyOf(appliedChanges);
		Objects.requireNonNull(restartRequirement, "restartRequirement");
		Objects.requireNonNull(failure, "failure");
	}

	boolean succeeded() {
		return failure.isEmpty();
	}
}

record ConfigChangeFailure(ConfigValueChange<?> change, RuntimeException exception) {
	ConfigChangeFailure {
		Objects.requireNonNull(change, "change");
		Objects.requireNonNull(exception, "exception");
	}
}
