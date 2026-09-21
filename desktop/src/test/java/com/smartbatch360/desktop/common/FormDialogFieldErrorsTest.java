package com.smartbatch360.desktop.common;

import com.smartbatch360.desktop.api.ApiErrorDto.FieldErrorDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a form reports validation errors that have no field of their own - in
 * practice, one line of a list, "materials[1].quantity", since forms register
 * the list rather than each line.
 *
 * Two things used to go wrong: each such error replaced the previous one, so
 * with two bad lines only the last was ever shown; and the line was never
 * named, so with several lines the operator had to guess which was wrong.
 */
class FormDialogFieldErrorsTest {

    private static final Set<String> RECIPE_FORM_FIELDS = Set.of("name", "description", "status", "materials");

    @Test
    void namesTheLineAnErrorBelongsTo() {
        String message = FormDialog.unplacedMessage(
                List.of(new FieldErrorDto("materials[0].quantity", "Quantity must be at most 999999.99.")),
                RECIPE_FORM_FIELDS);

        assertThat(message).isEqualTo("Line 1: Quantity must be at most 999999.99.");
    }

    /** The bug: two bad lines used to leave only the second message on screen. */
    @Test
    void showsEveryUnplacedErrorNotJustTheLast() {
        String message = FormDialog.unplacedMessage(List.of(
                        new FieldErrorDto("materials[0].quantity", "Quantity must be at most 999999.99."),
                        new FieldErrorDto("materials[2].quantity", "Quantity must be greater than zero.")),
                RECIPE_FORM_FIELDS);

        assertThat(message).contains("Line 1: Quantity must be at most 999999.99.")
                .contains("Line 3: Quantity must be greater than zero.");
    }

    /** Errors that do have a field on the form are shown there, not repeated at the top. */
    @Test
    void leavesOutErrorsThatHaveAFieldOfTheirOwn() {
        String message = FormDialog.unplacedMessage(List.of(
                        new FieldErrorDto("name", "Recipe name is required."),
                        new FieldErrorDto("materials[1].quantity", "Quantity must be greater than zero.")),
                RECIPE_FORM_FIELDS);

        assertThat(message).isEqualTo("Line 2: Quantity must be greater than zero.");
    }

    @Test
    void returnsNullWhenEveryErrorWasPlaced() {
        assertThat(FormDialog.unplacedMessage(
                List.of(new FieldErrorDto("name", "Recipe name is required.")), RECIPE_FORM_FIELDS)).isNull();
    }

    /** A plain unknown field (not a list item) is shown as-is, without an invented line number. */
    @Test
    void passesThroughAnUnknownFieldThatIsNotAListItem() {
        assertThat(FormDialog.unplacedMessage(
                List.of(new FieldErrorDto("somethingElse", "Something is wrong.")), RECIPE_FORM_FIELDS))
                .isEqualTo("Something is wrong.");
    }
}
