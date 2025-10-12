package org.ananie.parishApp.utilities;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.services.FaithfulPDFService;
import org.ananie.parishApp.services.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class FaithfulPDFUtility {
    private final FaithfulPDFService faithfulPDFService;
    private final LoggingService loggingService;
    
    @Autowired
    public FaithfulPDFUtility(FaithfulPDFService faithfulPDFService, LoggingService loggingService) {
        this.faithfulPDFService = faithfulPDFService;
        this.loggingService = loggingService;
        loggingService.logUserAction("Utility Initialization", "FaithfulPDFUtility initialized");
    }

    /**
     * Main export method - handles file selection and exports PDF asynchronously
     */
    public void exportFaithfulToPDF(Faithful faithful, List<Contribution> contributions, Window ownerWindow) {
        if (faithful == null) {
            loggingService.logUserAction("PDF Export Attempt", "Attempted to export null faithful/no faithful selected");
            showAlert(Alert.AlertType.WARNING, "Ikosa", "Nta mukristu wahisemo.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Bika ifishi ya PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedName = faithful.getName().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_");
        fileChooser.setInitialFileName("Amaturo_" + sanitizedName + "_" + timestamp + ".pdf");

        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            loggingService.logUserAction("PDF Export", 
                "Exporting faithful PDF: " + faithful.getName() + " to " + file.getAbsolutePath());
            
            // Debug logo path before export
            debugLogoPath();
            
            exportToPDFAsync(faithful, contributions, file.getAbsolutePath());
        } else {
            loggingService.logUserAction("PDF Export", "User cancelled file selection");
        }
    }

    /**
     * Export with confirmation dialog
     */
    public void exportWithConfirmation(Faithful faithful, List<Contribution> contributions, Window ownerWindow) {
        if (faithful == null) {
            loggingService.logUserAction("PDF_EXPORT", "Attempted export for null faithful");
            showAlert(Alert.AlertType.WARNING, "Ikosa", "Nta mukristu wahisemo.");
            return;
        }

        loggingService.logUserAction("PDF_EXPORT_CONFIRMATION", "Showing confirmation for: " + faithful.getName());
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Kwemeza");
        confirmAlert.setHeaderText("Gukora Ifishi ya PDF");
        confirmAlert.setContentText("Urashaka gukora ifishi ya PDF y'amaturo ya " + faithful.getName() + "?");
        
        ButtonType confirmButton = new ButtonType("Yego");
        ButtonType cancelButton = new ButtonType("Oya");
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            loggingService.logUserAction("PDF_EXPORT_CONFIRMED", "User confirmed export for: " + faithful.getName());
            exportFaithfulToPDF(faithful, contributions, ownerWindow);
        } else {
            loggingService.logUserAction("PDF_EXPORT_CANCELLED", "User cancelled export for: " + faithful.getName());
        }
    }

    /**
     * Core async export method
     */
    private CompletableFuture<Void> exportToPDFAsync(Faithful faithful, List<Contribution> contributions, String filePath) {
        long startTime = System.currentTimeMillis();
        loggingService.logUserAction("Async PDF Export Start", 
            "Starting async PDF export for: " + faithful.getName());
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Generate PDF with logo
                faithfulPDFService.generateFaithfulDetailsPDF(faithful, contributions, filePath);
                
                long duration = System.currentTimeMillis() - startTime;
                loggingService.logPerformance("PDF Generation", duration);
                loggingService.logPDFGeneration("Faithful Report", filePath, true);

                javafx.application.Platform.runLater(() -> {
                    showSuccessDialog(filePath);
                });
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                loggingService.logPerformance("Failed PDF Generation", duration);
                loggingService.logError("PDF Generation", e);
                loggingService.logPDFGeneration("Faithful Report", filePath, false);
                
                javafx.application.Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Ikosa", 
                            "Ifishi ntabwo yashoboye kubikwa: " + e.getMessage())
                );
            }
        });
    }

    /**
     * Debug logo path and existence
     */
    private void debugLogoPath() {
        String logoPath = ViewPaths.LOGO;
        loggingService.logUserAction("PDF_LOGO_DEBUG", "Logo path from ViewPaths.LOGO: " + logoPath);
        
        File logoFile = new File(logoPath);
		loggingService.logUserAction("PDF_LOGO_DEBUG", "Logo file exists: " + logoFile.exists());
		loggingService.logUserAction("PDF_LOGO_DEBUG", "Logo file absolute path: " + logoFile.getAbsolutePath());
		loggingService.logUserAction("PDF_LOGO_DEBUG", "Logo file can read: " + logoFile.canRead());
		loggingService.logUserAction("PDF_LOGO_DEBUG", "Logo file length: " + logoFile.length() + " bytes");
        
        // Try different logo paths as resources
        String[] possiblePaths = {
            "/parish_logo.png",
            "/images/parish_logo.png", 
            "/assets/parish_logo.png",
            "src/main/resources/parish_logo.png",
            "parish_slogo.png"
        };
        
        for (String path : possiblePaths) {
            try {
                // Check as resource
                if (getClass().getResource(path) != null) {
                    loggingService.logUserAction("PDF_LOGO_DEBUG", "Found logo as resource: " + path);
                }
                // Check as file
                File file = new File(path);
                if (file.exists()) {
                    loggingService.logUserAction("PDF_LOGO_DEBUG", "Found logo as file: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                // Ignore and continue checking
            }
        }
    }

    /**
     * Show success dialog with option to open PDF
     */
    private void showSuccessDialog(String filePath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Byarangiye");
        alert.setHeaderText(null);
        alert.setContentText("Ifishi yabitswe neza kuri:\n" + filePath);
        
        ButtonType openButton = new ButtonType("Reba ifishi");
        ButtonType okButton = new ButtonType("Bireke");
        alert.getButtonTypes().setAll(openButton, okButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openButton) {
            loggingService.logUserAction("PDF Open", "User chose to open PDF: " + filePath);
            openPDFFile(filePath);
        }
    }

    /**
     * Open PDF file with system default application
     */
    private void openPDFFile(String filePath) {
        loggingService.logUserAction("PDF Open Attempt", "Attempting to open PDF: " + filePath);
        
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String command;
            
            if (osName.contains("win")) {
                command = "rundll32 url.dll,FileProtocolHandler " + filePath;
            } else if (osName.contains("mac")) {
                command = "open " + filePath;
            } else {
                command = "xdg-open " + filePath;
            }
            
            loggingService.logUserAction("PDF Open Command", "Executing: " + command);
            Runtime.getRuntime().exec(command);
            loggingService.logUserAction("PDF Open Success", "Successfully opened: " + filePath);
            
        } catch (Exception e) {
            loggingService.logError("PDF Open", e);
            showAlert(Alert.AlertType.WARNING, "Ikosa", 
                    "Ntabwo dosiye yashoboye gufunguka. Ushobora kuyifungura kuri: " + filePath);
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        loggingService.logUserAction("Alert Shown", title + ": " + message);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}