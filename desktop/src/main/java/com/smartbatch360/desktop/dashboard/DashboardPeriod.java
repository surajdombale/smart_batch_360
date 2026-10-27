package com.smartbatch360.desktop.dashboard;

/** The periods the dashboard offers, matching the backend's DashboardPeriod. */
public enum DashboardPeriod {
    TODAY("Today", "today", "vs this time yesterday"),
    YESTERDAY("Yesterday", "yesterday", "vs the day before"),
    LAST_7_DAYS("Last 7 Days", "in the last 7 days", "vs the 7 days before"),
    LAST_30_DAYS("Last 30 Days", "in the last 30 days", "vs the 30 days before");

    private final String label;
    private final String within;
    private final String comparison;

    DashboardPeriod(String label, String within, String comparison) {
        this.label = label;
        this.within = within;
        this.comparison = comparison;
    }

    /** "No batches " + within() reads as a sentence. */
    public String within() {
        return within;
    }

    /** What the change under each headline figure is measured against. */
    public String comparison() {
        return comparison;
    }

    public boolean singleDay() {
        return this == TODAY || this == YESTERDAY;
    }

    @Override
    public String toString() {
        return label;
    }
}
