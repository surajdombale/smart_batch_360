package com.smartbatch360.api.recipe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The largest total a recipe may add up to, in kilograms.
 *
 * A recipe's total is the sum of its material lines, so without a ceiling it
 * grows with whatever is typed - a mix can be saved that the plant cannot
 * physically batch. This is that ceiling, configured with
 * smartbatch360.recipe.max-total-kg.
 *
 * Unset means no limit, which is the behaviour every recipe was saved under
 * before this existed. The value belongs to the plant (its mixer capacity), so
 * it is not guessed here - it stays off until someone sets it.
 */
@Component
public class RecipeTotalLimit {

    private final BigDecimal maxTotalKg;

    public RecipeTotalLimit(@Value("${smartbatch360.recipe.max-total-kg:}") String configured) {
        this.maxTotalKg = configured == null || configured.isBlank()
                ? null
                : new BigDecimal(configured.trim());
    }

    /** For tests and any caller that needs a specific limit rather than the configured one. */
    public static RecipeTotalLimit of(String maxTotalKg) {
        return new RecipeTotalLimit(maxTotalKg);
    }

    public static RecipeTotalLimit none() {
        return new RecipeTotalLimit("");
    }

    public boolean isSet() {
        return maxTotalKg != null;
    }

    public BigDecimal maxTotalKg() {
        return maxTotalKg;
    }

    public boolean isExceededBy(BigDecimal total) {
        return isSet() && total != null && total.compareTo(maxTotalKg) > 0;
    }
}
