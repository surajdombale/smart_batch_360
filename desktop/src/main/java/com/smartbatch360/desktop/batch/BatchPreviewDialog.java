package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.header.HeaderDto;
import com.smartbatch360.desktop.header.HeaderLogoDto;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Batch Reports > View: the batch's report as it will be downloaded, as a PDF
 * or as an Excel workbook, with Download beside Close - 31-Oct-2026, at the
 * user's request. It replaces both the read-only detail dialog View used to
 * open and the row's Report menu, which saved the same two files unseen. The
 * button keeps the name View until the user settles on another.
 *
 * Each format is built once, the first time it is shown, and Download saves
 * that very build: the PDF's own bytes, and the SpreadsheetLayout the grid was
 * drawn from. What was looked at is what gets saved.
 *
 * The dialog takes most of the screen rather than sizing itself to its content
 * - a report page is taller than any screen - and the preview scrolls.
 */
public final class BatchPreviewDialog {

    /** The two files a batch report is saved as. */
    enum Format {
        PDF(".pdf", "PDF document"),
        EXCEL(".xlsx", "Excel workbook");

        private final String extension;
        private final String description;

        Format(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }
    }

    /** The most of the screen the dialog takes, and no wider than a page needs to read well. */
    private static final double MAX_SCREEN_WIDTH = 0.9;
    private static final double MAX_SCREEN_HEIGHT = 0.8;
    private static final double MOST_WIDTH = 1000;
    private static final double PAGE_GAP = 16;

    private record RenderedPdf(byte[] bytes, List<Image> pages) {
    }

    private final BatchDto batch;
    private final List<BatchCycleDto> cycles;
    private final HeaderDto company;
    private final HeaderLogoDto logo;
    private final BatchSheetPdfExporter pdfExporter = new BatchSheetPdfExporter();
    private final BatchSheetExcelExporter excelExporter = new BatchSheetExcelExporter();

    private final Dialog<Void> dialog = new Dialog<>();
    private final NotificationBanner banner = new NotificationBanner();
    private final StackPane previewArea = new StackPane();
    private final Label fileLabel = new Label();
    private final Map<Format, Node> previews = new EnumMap<>(Format.class);
    private final Button downloadButton;

    // Read and written on the JavaFX thread only; the PDF is handed over once built.
    private byte[] pdf;
    private SpreadsheetLayout workbook;
    private Format shown;
    private boolean saving;

    /**
     * @param company the letterhead, or null when no company is recorded
     * @param logo    its logo, or null when none was uploaded or it would not load
     */
    public static void show(Window owner, BatchDto batch, List<BatchCycleDto> cycles,
                            HeaderDto company, HeaderLogoDto logo) {
        new BatchPreviewDialog(owner, batch, cycles, company, logo).dialog.showAndWait();
    }

    private BatchPreviewDialog(Window owner, BatchDto batch, List<BatchCycleDto> cycles,
                               HeaderDto company, HeaderLogoDto logo) {
        this.batch = batch;
        this.cycles = cycles;
        this.company = company;
        this.logo = logo;

        dialog.setTitle("Batch " + batch.batchNumber());
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(BatchPreviewDialog.class.getResource("/css/theme.css").toExternalForm());
        // RIGHT sits after Close in the button bar, so the order reads Close, Download.
        ButtonType download = new ButtonType("Download", ButtonBar.ButtonData.RIGHT);
        pane.getButtonTypes().addAll(ButtonType.CLOSE, download);
        pane.lookupButton(ButtonType.CLOSE).getStyleClass().add("button-secondary");
        downloadButton = (Button) pane.lookupButton(download);
        downloadButton.getStyleClass().add("button-primary");
        // Saving leaves the preview open, so the other format can be saved as well.
        downloadButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            download();
        });

        VBox content = new VBox(10, toolbar(), banner, previewArea);
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        pane.setContent(content);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        pane.setPrefSize(Math.min(screen.getWidth() * MAX_SCREEN_WIDTH, MOST_WIDTH),
                screen.getHeight() * MAX_SCREEN_HEIGHT);

        show(Format.PDF);
    }

    /** PDF and Excel as one switch of two halves, PDF chosen to begin with, and the file it saves. */
    private HBox toolbar() {
        ToggleGroup group = new ToggleGroup();
        ToggleButton pdfToggle = toggle("PDF", Format.PDF, group, "format-toggle-first");
        ToggleButton excelToggle = toggle("Excel", Format.EXCEL, group, "format-toggle-last");
        pdfToggle.setSelected(true);
        group.selectedToggleProperty().addListener((observable, was, now) -> {
            if (now == null) {
                group.selectToggle(was);   // clicking the chosen half again keeps it chosen
            } else {
                show((Format) now.getUserData());
            }
        });

        HBox toggle = new HBox(pdfToggle, excelToggle);
        toggle.getStyleClass().add("format-toggle");
        fileLabel.getStyleClass().add("format-file");
        HBox bar = new HBox(12, toggle, fileLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ToggleButton toggle(String text, Format format, ToggleGroup group, String position) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(format);
        button.setToggleGroup(group);
        button.getStyleClass().add(position);
        return button;
    }

    private void show(Format format) {
        if (format == shown) {
            return;
        }
        shown = format;
        banner.hide();
        fileLabel.setText(fileName(format));
        Node preview = previews.computeIfAbsent(format, f -> f == Format.PDF ? preparePdf() : prepareWorkbook());
        previewArea.getChildren().setAll(preview);
        refreshDownload();
    }

    /** Built and drawn on a worker thread; a spinner holds its place until then. */
    private Node preparePdf() {
        StackPane holder = new StackPane(loading("Preparing the PDF..."));
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        byte[] bytes = pdfBytes();
                        List<Image> pages = PdfPreview.render(bytes).stream().map(PdfPreview::toFxImage).toList();
                        return new RenderedPdf(bytes, pages);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .whenComplete((rendered, failure) -> Platform.runLater(() -> {
                    if (failure != null) {
                        holder.getChildren().setAll(failed("The PDF could not be prepared: " + rootCauseMessage(failure)));
                        return;
                    }
                    pdf = rendered.bytes();
                    holder.getChildren().setAll(pages(rendered.pages()));
                    refreshDownload();
                }));
        return holder;
    }

    private byte[] pdfBytes() throws IOException {
        try (PDDocument document = pdfExporter.build(batch, cycles, company, logo);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.save(out);
            return out.toByteArray();
        }
    }

    /** One page under another, each fitted to the dialog's width, as a PDF reader shows them. */
    private ScrollPane pages(List<Image> images) {
        VBox column = new VBox(PAGE_GAP);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPadding(new Insets(PAGE_GAP));

        ScrollPane scroller = new ScrollPane(column);
        scroller.getStyleClass().add("pdf-scroll");
        scroller.setFitToWidth(true);
        // The pages follow the viewport's width, so the bar must not come and go and change it.
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        for (Image image : images) {
            ImageView page = new ImageView(image);
            page.getStyleClass().add("pdf-page");
            page.setPreserveRatio(true);
            page.setSmooth(true);
            // Never drawn larger than it was rendered, so the type stays sharp.
            page.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(image.getWidth(),
                            Math.max(200, scroller.getViewportBounds().getWidth() - (2 * PAGE_GAP))),
                    scroller.viewportBoundsProperty()));
            column.getChildren().add(page);
        }
        return scroller;
    }

    /** Laid out here, on the JavaFX thread: a sheet is a few hundred cells, quick to build. */
    private Node prepareWorkbook() {
        try {
            SpreadsheetLayout layout = excelExporter.layout(batch, cycles, company);
            ScrollPane scroller = new ScrollPane(SpreadsheetPreview.of(layout));
            scroller.getStyleClass().add("sheet-scroll");
            workbook = layout;
            return scroller;
        } catch (RuntimeException e) {
            return failed("The workbook could not be prepared: " + rootCauseMessage(e));
        }
    }

    private boolean ready(Format format) {
        return format == Format.PDF ? pdf != null : workbook != null;
    }

    private void refreshDownload() {
        downloadButton.setDisable(saving || !ready(shown));
    }

    private void download() {
        Format format = shown;
        if (saving || !ready(format)) {
            return;
        }
        File file = chooseFile(format);
        if (file == null) {
            return;   // cancelled at the save dialog
        }
        byte[] pdfBytes = pdf;
        SpreadsheetLayout layout = workbook;
        saving = true;
        refreshDownload();
        banner.hide();
        CompletableFuture
                .runAsync(() -> {
                    try {
                        if (format == Format.PDF) {
                            Files.write(file.toPath(), pdfBytes);
                        } else {
                            excelExporter.write(file, layout);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .whenComplete((done, failure) -> Platform.runLater(() -> {
                    saving = false;
                    refreshDownload();
                    if (failure != null) {
                        banner.showError("Could not save the batch report: " + rootCauseMessage(failure));
                    } else {
                        banner.showSuccess("Batch " + batch.batchNumber() + " report saved to " + file.getName()
                                + " (" + cycles.size() + (cycles.size() == 1 ? " cycle)." : " cycles)."));
                    }
                }));
    }

    private File chooseFile(Format format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Batch Report");
        chooser.setInitialFileName(fileName(format));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.description, "*" + format.extension));
        File documents = new File(System.getProperty("user.home"), "Documents");
        chooser.setInitialDirectory(documents.isDirectory()
                ? documents : new File(System.getProperty("user.home")));
        return chooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
    }

    /** A batch number can be typed by hand, so it may hold characters a file name cannot. */
    private String fileName(Format format) {
        return "batch-report-" + String.valueOf(batch.batchNumber()).replaceAll("[^A-Za-z0-9._-]", "_")
                + format.extension;
    }

    private static VBox loading(String message) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(48, 48);
        VBox box = new VBox(10, indicator, new Label(message));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Label failed(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("state-message");
        label.setWrapText(true);
        return label;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
