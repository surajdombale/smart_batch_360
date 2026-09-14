package com.smartbatch360.desktop.batch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.List;

/**
 * Prints Batch Reports - the last part of the export line, 2026-09-14.
 *
 * It prints the very document the PDF export writes, through PDFBox's own
 * printing support, so the paper and the PDF cannot disagree and there is no
 * second layout to keep in step. No new dependency either: PDFBox is already
 * here, and java.awt.print ships with the JDK.
 *
 * The standard system print dialog does the choosing - printer, copies, page
 * range. Call this OFF the JavaFX application thread: the dialog is AWT and
 * modal, and holding the FX thread on it would freeze the window it came from.
 */
public final class BatchReportPrinter {

    private final BatchReportPdfExporter exporter;

    public BatchReportPrinter(BatchReportPdfExporter exporter) {
        this.exporter = exporter;
    }

    /** @return true if it went to a printer, false if the operator cancelled the dialog */
    public boolean print(List<BatchDto> batches, String filterSummary, String note)
            throws IOException, PrinterException {
        try (PDDocument document = exporter.build(batches, filterSummary, note)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("SmartBatch360 - Batch Reports");
            job.setPageable(pageableFor(document));
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        }
    }

    /**
     * The report is drawn landscape, so print it landscape explicitly rather
     * than leave it to a guess from the page shape.
     */
    static Pageable pageableFor(PDDocument document) {
        return new PDFPageable(document, Orientation.LANDSCAPE);
    }
}
