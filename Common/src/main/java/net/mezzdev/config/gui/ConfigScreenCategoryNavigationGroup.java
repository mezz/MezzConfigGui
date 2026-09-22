package net.mezzdev.config.gui;

import net.minecraft.network.chat.Component;

/**
 * Groups compatible config screen categories under one navigation root.
 */
public record ConfigScreenCategoryNavigationGroup(String name, Component title, Component description) {
}
