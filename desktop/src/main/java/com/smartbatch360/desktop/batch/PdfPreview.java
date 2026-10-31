package com.smartbatch360.desktop.batch;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A PDF's pages as pictures, for Batch Reports > View to show a report before
 * it is saved. Rendered by PDFBox from the very bytes Download writes, so the
 * preview is the file rather than a second drawing of it.
 */
final class PdfPreview {

    /**
     * Pixels per PDF point: twice the 72 a point is defined at, so a page stays
     * sharp when it is fitted to a wide dialog or a high-density screen.
     */
    static final float RENDER_SCALE = 2f;

    private PdfPreview() {
    }

    /** Every page, in order. Slow enough to keep off the JavaFX thread. */
    static List<BufferedImage> render(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                pages.add(renderer.renderImage(i, RENDER_SCALE, ImageType.RGB));
            }
            return pages;
        }
    }

    /** As a JavaFX image - copied pixel for pixel, rather than adding javafx-swing for SwingFXUtils. */
    static Image toFxImage(BufferedImage page) {
        int width = page.getWidth();
        int height = page.getHeight();
        int[] pixels = page.getRGB(0, 0, width, height, null, 0, width);
        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
    }
}
