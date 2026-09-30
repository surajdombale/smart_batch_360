package com.smartbatch360.api.recipe;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ceiling on what a recipe may add up to.
 *
 * A recipe's total is the sum of its material lines, so without a limit a mix
 * can be saved that the plant cannot physically batch. The value is the plant's
 * own (its mixer capacity), so it is not guessed in code: unset means no limit,
 * which is how every recipe behaved before this existed.
 */
class RecipeTotalLimitTest {

    @Test
    void unsetMeansNoLimit() {
        RecipeTotalLimit limit = RecipeTotalLimit.none();

        assertThat(limit.isSet()).isFalse();
        assertThat(limit.isExceededBy(new BigDecimal("9999999.99"))).isFalse();
    }

    @Test
    void blankConfigurationIsTreatedAsUnset() {
        assertThat(new RecipeTotalLimit(null).isSet()).isFalse();
        assertThat(new RecipeTotalLimit("   ").isSet()).isFalse();
    }

    @Test
    void aTotalOverTheLimitIsExceeded() {
        RecipeTotalLimit limit = RecipeTotalLimit.of("1000");

        assertThat(limit.isExceededBy(new BigDecimal("1000.01"))).isTrue();
        assertThat(limit.isExceededBy(new BigDecimal("2500.00"))).isTrue();
    }

    /** Exactly at the limit is allowed - a full batch is a legitimate mix. */
    @Test
    void aTotalExactlyAtTheLimitIsAllowed() {
        RecipeTotalLimit limit = RecipeTotalLimit.of("1000");

        assertThat(limit.isExceededBy(new BigDecimal("1000"))).isFalse();
        assertThat(limit.isExceededBy(new BigDecimal("1000.00"))).isFalse();
        assertThat(limit.isExceededBy(new BigDecimal("999.99"))).isFalse();
    }

    /** Trailing zeros must not make an equal total look different. */
    @Test
    void scaleDoesNotChangeTheComparison() {
        assertThat(RecipeTotalLimit.of("1000.00").isExceededBy(new BigDecimal("1000"))).isFalse();
        assertThat(RecipeTotalLimit.of("1000").isExceededBy(new BigDecimal("1000.000"))).isFalse();
    }
}
