package org.ananie.mushaParish.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.ananie.mushaParish.model.SubParish;
import org.ananie.mushaParish.services.SubParishService;
import org.ananie.mushaParish.services.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AddSubParishController {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameTextField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    private final SubParishService subParishService;
    private final LoggingService loggingService;
    private SubParish currentSubParish; // For editing existing SubParish
    private Runnable refreshCallback; // Callback to refresh parent view

    @Autowired
    public AddSubParishController(SubParishService subParishService, 
                                LoggingService loggingService) {
        this.subParishService = subParishService;
        this.loggingService = loggingService;
    }

    @FXML
    public void initialize() {
        loggingService.logUserAction("SUBPARISH_FORM", "SubParish form initialized");
        
        saveButton.setOnAction(event -> {
            loggingService.logUserAction("SUBPARISH_SAVE_ATTEMPT", "Attempting to save SubParish");
            saveSubParish();
        });
        
        cancelButton.setOnAction(event -> {
            loggingService.logUserAction("SUBPARISH_FORM_CANCEL", "Cancelled SubParish form");
            closeDialog();
        });
        
        statusLabel.setText("");
    }

    /**
     * Initialize the controller for adding or editing
     * @param subParishToEdit SubParish to edit (null for add mode)
     * @param refreshCallback Callback to refresh parent view
     */
    public void initData(SubParish subParishToEdit, Runnable refreshCallback) {
        this.currentSubParish = subParishToEdit;
        this.refreshCallback = refreshCallback;

        if (subParishToEdit != null) {
            loggingService.logUserAction("SUBPARISH_EDIT_MODE", 
                "Editing SubParish: " + subParishToEdit.getName() + 
                " (ID: " + subParishToEdit.getId() + ")");
            formTitleLabel.setText("HINDURA SANTARALI");
            nameTextField.setText(subParishToEdit.getName());
        } else {
            loggingService.logUserAction("SUBPARISH_ADD_MODE", "Adding new SubParish");
            formTitleLabel.setText("SANTARALI NSHYA");
            nameTextField.clear();
        }
        nameTextField.requestFocus();
    }

    private void saveSubParish() {
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: black;");

        String name = nameTextField.getText();
        if (name == null || name.trim().isEmpty()) {
            loggingService.logUserAction("SUBPARISH_VALIDATION_FAIL", "Empty name rejected");
            showError("Izina ry'santarali ntirushobora kuba ubusa.");
            return;
        }

        name = name.trim();
        loggingService.logUserAction("SUBPARISH_SAVE_PROCESS", 
            "Processing save for SubParish: " + name + 
            (currentSubParish != null ? " (ID: " + currentSubParish.getId() + ")" : ""));

        try {
            // Check if name already exists
            Optional<SubParish> existing = subParishService.findByName(name);
            if (existing.isPresent() && 
                (currentSubParish == null || !existing.get().getId().equals(currentSubParish.getId()))) {
                loggingService.logUserAction("SUBPARISH_VALIDATION_FAIL", 
                    "Duplicate name rejected: " + name);
                showError("Santarali ifite iri zina iranditse");
                return;
            }

            SubParish subParishToSave = currentSubParish != null ? currentSubParish : new SubParish();
            subParishToSave.setName(name);

            if (subParishToSave.getId() == null) {
                // Add new
                subParishService.save(subParishToSave);
                loggingService.logUserAction("SUBPARISH_ADD_SUCCESS", 
                    "Added new SubParish: " + name + " (ID: " + subParishToSave.getId() + ")");
                showSuccess("Santarali '" + name + "' yongeweho neza!");
            } else {
                // Update existing
                subParishService.update(subParishToSave);
                loggingService.logUserAction("SUBPARISH_UPDATE_SUCCESS", 
                    "Updated SubParish: " + name + " (ID: " + subParishToSave.getId() + ")");
                showSuccess("Santarali '" + name + "' yahinduwe neza!");
            }

            // Refresh parent view
            if (refreshCallback != null) {
                loggingService.logUserAction("SUBPARISH_REFRESH", "Triggering parent view refresh");
                refreshCallback.run();
            }

            // Close dialog after short delay
            Platform.runLater(() -> {
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                loggingService.logUserAction("SUBPARISH_FORM_CLOSE", 
                                    "Closing form after successful save");
                                closeDialog();
                            });
                        }
                    }, 1500
                );
            });

        } catch (Exception e) {
            loggingService.logError("SUBPARISH_SAVE_ERROR", e);
            showError("Ikosa ryabaye: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        loggingService.logUserAction("SUBPARISH_FORM_ERROR", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        loggingService.logUserAction("SUBPARISH_FORM_SUCCESS", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
        Platform.runLater(() -> {
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> statusLabel.setText(""));
                    }
                }, 3000
            );
        });
    }

    private void closeDialog() {
        try {
            loggingService.logUserAction("SUBPARISH_FORM_CLOSE", "Closing SubParish form dialog");
            if (cancelButton != null) {
                Scene scene = cancelButton.getScene();
                if (scene != null) {
                    if (scene.getWindow() instanceof Stage) {
                        Stage stage = (Stage) scene.getWindow();
                        stage.close();
                    } else {
                        loggingService.logError("DIALOG_CLOSE_ERROR", 
                            new RuntimeException("Scene not attached to Stage"));
                    }
                } else {
                    loggingService.logError("DIALOG_CLOSE_ERROR", 
                        new RuntimeException("Cancel button not attached to Scene"));
                }
            } else {
                loggingService.logError("DIALOG_CLOSE_ERROR", 
                    new RuntimeException("Cancel button FXML field is null"));
            }
        } catch (Exception e) {
            loggingService.logError("DIALOG_CLOSE_ERROR", e);
        }
    }
}