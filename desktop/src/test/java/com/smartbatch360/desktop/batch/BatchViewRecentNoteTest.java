package com.smartbatch360.desktop.batch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production shows only the most recent batches, so it has to say so - a list
 * that silently omits history is worse than a slow one. The note appears only
 * when something is actually left out.
 */
class BatchViewRecentNoteTest {

    @Test
    void saysWhatIsShownAndWhereTheRestIs() {
        String note = BatchView.recentNote(200, 4801);

        assertThat(note).isEqualTo(
                "Showing the 200 most recent batches of 4801. Use Batch Reports to search the rest.");
    }

    @Test
    void staysQuietWhenEverythingIsOnScreen() {
        assertThat(BatchView.recentNote(37, 37)).isNull();
        assertThat(BatchView.recentNote(0, 0)).isNull();
    }

    /** Defensive: a total below the shown count still means nothing is missing. */
    @Test
    void staysQuietIfTheTotalIsSmallerThanTheList() {
        assertThat(BatchView.recentNote(200, 12)).isNull();
    }
}
