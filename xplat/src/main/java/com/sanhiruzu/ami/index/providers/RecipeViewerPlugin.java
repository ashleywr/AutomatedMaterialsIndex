package com.sanhiruzu.ami.index.providers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a recipe-viewer plugin for automatic discovery.
 * The annotated class must implement {@link IRecipeViewerPlugin}
 * and have a public no-argument constructor.
 *
 * Scanned at startup from all loaded mod jars — no explicit registration needed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RecipeViewerPlugin {
}
