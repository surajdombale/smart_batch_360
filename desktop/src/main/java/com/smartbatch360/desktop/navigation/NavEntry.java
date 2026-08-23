package com.smartbatch360.desktop.navigation;

/** Common type for the two kinds of sidebar entry MainShell can render: a flat NavItem, or a NavGroup of them. */
public sealed interface NavEntry permits NavItem, NavGroup {
}
