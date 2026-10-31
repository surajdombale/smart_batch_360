package com.smartbatch360.desktop.batch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Batch Reports > View shows the PDF's pages as pictures before Download saves
 * the same bytes. Every page has to come through, at the page's own shape.
 */
class PdfPreviewTest {

    private BatchDto batch() {
        return new BatchDto(1L, "250401", 2L, "M25", 9L, 1L, "Shree Cement", 1L, "Kharadi",
                1L, "MH12PQ3457", 1L, "Ganesh More",
                new BigDecimal("900.00"), new BigDecimal("900.00"), BigDecimal.ZERO,
                Instant.parse("2026-10-07T06:30:00Z"), 3, null, BatchStatus.COMPLETED,
                List.of(new BatchMaterialDto(1L, "Cement", new BigDecimal("600.00"), new BigDecimal("200.00"),
                        new BigDecimal("0"), "kg")),
                Instant.now(), Instant.now());
    }

    private List<BatchCycleDto> cycles(int count) {
        List<BatchCycleDto> cycles = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            cycles.add(new BatchCycleDto((long) i, i, Instant.parse("2026-10-07T06:30:00Z").plusSeconds(60L * i),
                    List.of(new BatchCycleDto.Material("Cement", new BigDecimal("600.00"), new BigDecimal("200.00"),
                            new BigDecimal("201"))),
                    new BigDecimal("201")));
        }
        return cycles;
    }

    private byte[] pdf(List<BatchCycleDto> cycles, int[] pageCount) throws IOException {
        try (PDDocument document = new BatchSheetPdfExporter().build(batch(), cycles, null, null);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pageCount[0] = document.getNumberOfPages();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void everyPageIsRendered() throws IOException {
        int[] pages = new int[1];
        byte[] pdf = pdf(cycles(60), pages);

        List<BufferedImage> rendered = PdfPreview.render(pdf);

        assertThat(pages[0]).isGreaterThan(1);
        assertThat(rendered).hasSize(pages[0]);
    }

    @Test
    void aPageIsRenderedAtItsOwnShapeAndTwiceItsPointSize() throws IOException {
        byte[] pdf = pdf(cycles(3), new int[1]);

        BufferedImage page = PdfPreview.render(pdf).get(0);

        // Portrait A4 for a one-material recipe; PDFBox rounds a page's pixels down.
        assertThat(page.getWidth()).isEqualTo((int) (PDRectangle.A4.getWidth() * PdfPreview.RENDER_SCALE));
        assertThat(page.getHeight()).isEqualTo((int) (PDRectangle.A4.getHeight() * PdfPreview.RENDER_SCALE));
    }
}
