package net.mezzdev.config.gui.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class ConfigGuiIdentifiableResourceReloadListener implements PreparableReloadListener {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	private final Identifier fabricId;
	private final Supplier<PreparableReloadListener> listenerSupplier;

	public ConfigGuiIdentifiableResourceReloadListener(String id, Supplier<PreparableReloadListener> listenerSupplier) {
		this.fabricId = Identifier.fromNamespaceAndPath(CONFIG_GUI_MOD_ID, id);
		this.listenerSupplier = listenerSupplier;
	}

	public Identifier getFabricId() {
		return fabricId;
	}

	private PreparableReloadListener listener;

	@Override
	public void prepareSharedState(SharedState state) {
		listener = listenerSupplier.get();
		listener.prepareSharedState(state);
	}

	@Override
	public CompletableFuture<Void> reload(SharedState state, Executor backgroundExecutor, PreparationBarrier barrier, Executor gameExecutor) {
		return listener.reload(state, backgroundExecutor, barrier, gameExecutor);
	}
}
