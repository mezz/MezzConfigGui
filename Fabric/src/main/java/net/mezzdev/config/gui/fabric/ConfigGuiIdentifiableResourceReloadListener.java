package net.mezzdev.config.gui.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class ConfigGuiIdentifiableResourceReloadListener implements IdentifiableResourceReloadListener {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	private final ResourceLocation fabricId;
	private final Supplier<PreparableReloadListener> listenerSupplier;

	public ConfigGuiIdentifiableResourceReloadListener(String id, Supplier<PreparableReloadListener> listenerSupplier) {
		this.fabricId = ResourceLocation.fromNamespaceAndPath(CONFIG_GUI_MOD_ID, id);
		this.listenerSupplier = listenerSupplier;
	}

	@Override
	public ResourceLocation getFabricId() {
		return fabricId;
	}

	@Override
	public CompletableFuture<Void> reload(
		PreparationBarrier preparationBarrier,
		ResourceManager resourceManager,
		ProfilerFiller preparationsProfiler,
		ProfilerFiller reloadProfiler,
		Executor backgroundExecutor,
		Executor gameExecutor
	) {
		PreparableReloadListener listener = listenerSupplier.get();
		return listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
	}
}
