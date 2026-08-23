package com.smartbatch360.desktop.navigation;

import javafx.scene.layout.Region;

import java.util.function.Supplier;

/**
 * A single sidebar entry, standalone or inside a NavGroup. {@code viewFactory}
 * is invoked fresh each time the item is selected so every screen reloads its
 * own data from the REST API (docs/05_CRUD_SPECIFICATION.md "List" flow).
 */
public record NavItem(String id, String label, Supplier<Region> viewFactory) implements NavEntry {
}
