package net.mezzdev.config.gui.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets the config GUI detect config screen plugins on Forge and NeoForge.
 * Annotated {@link IConfigGuiPlugin} implementations must have a constructor with no arguments.
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigGuiPlugin {
}
