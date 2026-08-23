package com.smartbatch360.desktop.navigation;

import java.util.List;

/**
 * A labeled section of the sidebar containing several NavItems, rendered under
 * a section heading (e.g. "Resources" grouping Clients/Sites/Vehicles/Drivers,
 * requested by the user 2026-08-23). Always expanded - no interactive
 * collapse state, since a handful of always-visible children reads more
 * cleanly than an accordion for this few items.
 */
public record NavGroup(String label, List<NavItem> children) implements NavEntry {
}
