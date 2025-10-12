package org.ananie.parishApp.utilities;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.ananie.parishApp.configurations.PDFConfig;
import org.ananie.parishApp.model.FaithfulContributionRow;
import org.ananie.parishApp.services.ContributionReportPDFService;
import org.ananie.parishApp.services.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class ContributionReportPDFUtility {

    private final ContributionReportPDFService contributionReportPDFService;
    private final PDFConfig pdfConfig;
    private final LoggingService loggingService;

    @Autowired
    public ContributionReportPDFUtility(
        ContributionReportPDFService contributionReportPDFService,
        PDFConfig pdfConfig,
        LoggingService loggingService) {
        this.contributionReportPDFService = contributionReportPDFService;
        this.pdfConfig = pdfConfig;
        this.loggingService = loggingService;
        loggingService.logUserAction("Utility Initialization", "ContributionReportPDFUtility initialized");
    }

    /**
     * Export a table of contributions to a PDF file with a file chooser.
     */
    public void exportReportToPDF(TableView<FaithfulContributionRow> reportTable, Window ownerWindow, String reportTitle) {
        if (reportTable.getItems().isEmpty()) {
            loggingService.logUserAction("PDF Export Attempt", "Attempted to export empty report: " + reportTitle);
            showAlert(Alert.AlertType.WARNING, "Ikosa", "Nta makuru agize iyo raporo.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Bika raporo ya PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        // Set default filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName(reportTitle + "_" + timestamp + ".pdf");
        
        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            loggingService.logUserAction("PDF Export", "User selected file path: " + file.getAbsolutePath());
            exportReportToPDFAsync(reportTable.getColumns(), reportTable.getItems(), file.getAbsolutePath(), reportTitle);
        } else {
            loggingService.logUserAction("PDF Export", "User cancelled file selection");
        }
    }

    /**
     * Quick export to the configured default output directory.
     */
    public void quickExportReportToPDF(TableView<FaithfulContributionRow> reportTable, String reportTitle) {
        if (reportTable.getItems().isEmpty()) {
            loggingService.logUserAction("Quick PDF Export", "Attempted quick export of empty report");
            showAlert(Alert.AlertType.WARNING, "Ikosa", "Nta makuru agize iyo raporo.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = reportTitle + "_" + timestamp + ".pdf";
        String filepath = pdfConfig.getDefaultOutputDirectory() + File.separator + filename;
        
        loggingService.logUserAction("Quick PDF Export", "Exporting to default location: " + filepath);
        exportReportToPDFAsync(reportTable.getColumns(), reportTable.getItems(), filepath, reportTitle);
    }
    
    /**
     * Export the report to PDF asynchronously with progress indication.
     */
    public void exportReportToPDFAsync(
        List<javafx.scene.control.TableColumn<FaithfulContributionRow, ?>> columns,
        List<FaithfulContributionRow> data,
        String filePath,
        String reportTitle) {
        
        long startTime = System.currentTimeMillis();
        loggingService.logUserAction("PDF Generation Start", "Starting PDF generation for: " + reportTitle);
        
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Gukora raporo ya PDF...");
                updateProgress(0, 100);
                
                loggingService.logPDFGeneration("Contribution Report", "Starting generation for: " + reportTitle, true);
                
                // Debug information
                loggingService.logUserAction("PDF Debug", 
                    "Creating PDF at: " + filePath + 
                    " | Rows: " + (data != null ? data.size() : "null") + 
                    " | Columns: " + (columns != null ? columns.size() : "null"));
                
                // Ensure directory exists
                File file = new File(filePath);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirCreated = parentDir.mkdirs();
                    loggingService.logUserAction("Directory Creation", 
                        "Created directory: " + dirCreated + " at " + parentDir.getAbsolutePath());
                }
                
                contributionReportPDFService.generateContributionReportPDF(
                    columns, data, reportTitle, filePath);
                
                updateProgress(100, 100);
                updateMessage("Raporo yakozwe");
                
                return null;
            }

            @Override
            protected void succeeded() {
                long duration = System.currentTimeMillis() - startTime;
                loggingService.logPerformance("PDF Generation", duration);
                
                Platform.runLater(() -> {
                    File finalFile = new File(filePath);
                    if (finalFile.exists()) {
                        loggingService.logPDFGeneration("Contribution Report", "Successfully generated: " + filePath, true);
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Byarangiye");
                        alert.setHeaderText(null);
                        alert.setContentText("Raporo yabitswe neza kuri:\n" + filePath);

                        ButtonType openButton = new ButtonType("Reba raporo");
                        ButtonType okButton = new ButtonType("Bireke");
                        alert.getButtonTypes().setAll(openButton, okButton);

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == openButton) {
                            loggingService.logUserAction("PDF Open", "User chose to open PDF: " + filePath);
                            openPDFFile(filePath);
                        }
                    } else {
                        loggingService.logPDFGeneration("Contribution Report", "Failed to verify file existence: " + filePath, false);
                        showAlert(Alert.AlertType.ERROR, "Ikosa", "Raporo ntabwo yashoboye kubikwa. genzura itariki n'amazina.");
                    }
                });
            }
            
            @Override
            protected void failed() {
                long duration = System.currentTimeMillis() - startTime;
                loggingService.logPerformance("Failed PDF Generation", duration);
                
                Throwable exception = getException();
                loggingService.logError("PDF Generation", exception);
                loggingService.logPDFGeneration("Contribution Report", "Failed to generate: " + filePath, false);
                
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Ikosa", 
                              "Raporo ntabwo yashoboye gukorwa: " + 
                              (exception != null ? exception.getMessage() : "Ikosa ritazwi"))
                );
            }
        };

        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }
    
    private void openPDFFile(String filePath) {
        loggingService.logUserAction("PDF_OPEN_ATTEMPT", "Attempting to open PDF: " + filePath);
        
        new Thread(() -> {
            try {
                File pdfFile = new File(filePath);
                
                // First verify the file exists and is readable
                if (!pdfFile.exists()) {
                    loggingService.logUserAction("PDF_OPEN_ERROR", "File not found: " + filePath);
                    Platform.runLater(() -> 
                        showAlert(Alert.AlertType.ERROR, "Ikosa", "raporo ntabwo ibonetse kuri: " + filePath)
                    );
                    return;
                }
                
                if (!pdfFile.canRead()) {
                    loggingService.logUserAction("PDF_OPEN_ERROR", "File not readable: " + filePath);
                    Platform.runLater(() -> 
                        showAlert(Alert.AlertType.ERROR, "Ikosa", "Ntabwo dosiye isomeka: " + filePath)
                    );
                    return;
                }

                String osName = System.getProperty("os.name").toLowerCase();
                String[] command;
                
                if (osName.contains("win")) {
                    command = new String[]{"cmd", "/c", "start", "\"\"", "\"" + filePath + "\""};
                } else if (osName.contains("mac")) {
                    command = new String[]{"open", filePath};
                } else {
                    command = new String[]{"xdg-open", filePath};
                }
                
                loggingService.logUserAction("PDF_OPEN_COMMAND", "Executing: " + String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process process = pb.start();
                
                // Special handling for Linux - don't check exit code
                if (osName.contains("linux") || osName.contains("nix")) {
                    loggingService.logUserAction("PDF_OPEN_SUCCESS", "Assuming success for Linux system");
                    return;
                }
                
                // For non-Linux systems, verify the process
                Thread.sleep(500); // Brief wait
                if (!process.isAlive() && process.exitValue() != 0) {
                    throw new IOException("PDF viewer failed to start. Exit code: " + process.exitValue());
                }
                
                loggingService.logUserAction("PDF_OPEN_SUCCESS", "PDF viewer launched successfully");
                
            } catch (Exception e) {
                loggingService.logError("PDF_OPEN_ERROR", e);
                
                // Only show error if not Linux (since Linux often returns false negatives)
                if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
                    Platform.runLater(() -> 
                        showAlert(Alert.AlertType.WARNING, "Ikosa",
                                "Ntabwo raporo yashoboye gufunguka: " + e.getMessage() + 
                                "\nUshobora kuyifungura kuri: " + filePath)
                    );
                }
            }
        }).start();
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        loggingService.logUserAction("Alert Shown", title + ": " + message);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}