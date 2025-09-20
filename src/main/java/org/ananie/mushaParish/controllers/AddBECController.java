package org.ananie.mushaParish.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.ananie.mushaParish.model.BEC;
import org.ananie.mushaParish.model.SubParish;
import org.ananie.mushaParish.services.BECService;
import org.ananie.mushaParish.services.LoggingService;
import org.ananie.mushaParish.services.SubParishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AddBECController {
    
    @FXML private Label formTitleLabel;
    @FXML private TextField nameTextField;
    @FXML private ComboBox<SubParish> subParishComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    
    private final BECService becService;
    private final SubParishService subParishService;
    private final LoggingService loggingService;
    private BEC currentBEC; // For editing existing BEC
    private Runnable refreshCallback; // Callback to refresh parent view

    @Autowired
    public AddBECController(BECService becService, SubParishService subParishService, LoggingService loggingService) {
        this.becService = becService;
        this.subParishService = subParishService;
		this.loggingService = loggingService;
    }

    @FXML
    public void initialize() {
        loggingService.logUserAction("BEC_FORM", "Add/Edit BEC form initialized");
        
        populateSubParishComboBox();
        
        saveButton.setOnAction(event -> {
            loggingService.logUserAction("BEC_SAVE_ATTEMPT", "Attempting to save BEC");
            saveBEC();
        });
        
        cancelButton.setOnAction(event -> {
            loggingService.logUserAction("BEC_FORM_CANCEL", "Cancelled BEC form");
            closeDialog();
        });
        
        statusLabel.setText("");
    }

    /**
     * Initialize the controller for adding or editing
     * @param becToEdit BEC to edit (null for add mode)
     * @param preselectedSubParish SubParish to preselect (optional)
     * @param refreshCallback Callback to refresh parent view
     */
    public void initData(BEC becToEdit, SubParish preselectedSubParish, Runnable refreshCallback) {
        this.currentBEC = becToEdit;
        this.refreshCallback = refreshCallback;

        if (becToEdit != null) {
            // Edit mode
            loggingService.logUserAction("BEC_EDIT_MODE","Editing BEC: " + becToEdit.getName() + " (ID: " + becToEdit.getId() + ")");
            formTitleLabel.setText("HINDURA IMPUZA");
            nameTextField.setText(becToEdit.getName());
            subParishComboBox.getSelectionModel().select(becToEdit.getSubParish());
        } else {
            // Add mode
            formTitleLabel.setText("IMPUZA NSHYA");
            loggingService.logUserAction("BEC_ADD_MODE", "Adding new BEC" + 
                    (preselectedSubParish != null ? " with preselected sub-parish" : ""));
            nameTextField.clear();
            if (preselectedSubParish != null) {
                subParishComboBox.getSelectionModel().select(preselectedSubParish);
            }
        }
        nameTextField.requestFocus();
    }

    private void populateSubParishComboBox() {
      try {
    	List<SubParish> subParishes = subParishService.findAllOrderedByName();
        loggingService.logUserAction("BEC_FORM_DATA", "Loaded " + subParishes.size() + " sub-parishes for selection");
        ObservableList<SubParish> observableSubParishes = FXCollections.observableArrayList(subParishes);
        subParishComboBox.setItems(observableSubParishes);
        // Show only the 'name' in the dropdown list
        subParishComboBox.setCellFactory(comboBox -> new ListCell<SubParish>() {
            @Override
            protected void updateItem(SubParish item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Show only the 'name' when an item is selected
        subParishComboBox.setConverter(new StringConverter<SubParish>() {
            @Override
            public String toString(SubParish subParish) {
                return subParish == null ? null : subParish.getName();
                }
            @Override
            public SubParish fromString(String name) {
            	return null;
            }
            });
        subParishComboBox.setPromptText("Hitamo Santarali");
    } catch (Exception e) {
        loggingService.logError("BEC_FORM_DATA_ERROR", e);
         e.printStackTrace();
     }
    }

    private void saveBEC() {
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: black;");

        String name = nameTextField.getText();
        SubParish selectedSubParish = subParishComboBox.getSelectionModel().getSelectedItem();

        // Validation
        if (name == null || name.trim().isEmpty()) {
        	loggingService.logUserAction("BEC_VALIDATION_FAIL", "Empty BEC name rejected");
            showError("Izina ry'impuza ntirushobora kuba ubusa.");
            return;
        }

        if (selectedSubParish == null) {
        	loggingService.logUserAction("BEC_VALIDATION_FAIL", "No sub-parish selected");
            showError("Hitamo santarali.");
            return;
        }

        name = name.trim();
        loggingService.logUserAction("BEC_SAVE_PROCESS", 
                "Processing save for BEC: " + name + 
                " under SubParish: " + selectedSubParish.getName());

        try {
            BEC becToSave = currentBEC != null ? currentBEC : new BEC();
            becToSave.setName(name);
            becToSave.setSubParish(selectedSubParish);

            if (becToSave.getId() == null) {
                // Add new
                becService.save(becToSave);
                loggingService.logUserAction("BEC_ADD_SUCCESS", "Added new BEC: " + name + " (ID: " + becToSave.getId() + ")");
                showSuccess("Impuza '" + name + "' yongeweho neza!");
            } else {
                // Update existing
            	loggingService.logUserAction("BEC_UPDATE_SUCCESS", "Updated BEC: " + name + " (ID: " + becToSave.getId() + ")");
                becService.update(becToSave);
                showSuccess("Impuza '" + name + "' yahinduwe neza!");
            }

            // Refresh parent view
            if (refreshCallback != null) {
            	loggingService.logUserAction("BEC_REFRESH", "Triggering parent view refresh");
                refreshCallback.run();
            }

            // Close dialog after short delay
            Platform.runLater(() -> {
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                        	loggingService.logUserAction("BEC_FORM_CLOSE", "Closing form after successful save");
                            Platform.runLater(() -> closeDialog());
                        }
                    }, 1500 // 1.5 seconds
                );
            });

        } catch (IllegalArgumentException e) {
        	loggingService.logError("BEC_VALIDATION_ERROR", e);
            showError(e.getMessage()); // Business validation from service
        } catch (Exception e) {
        	loggingService.logError("BEC_SAVE_ERROR", e);
            showError("Ikosa ryabaye: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
    	loggingService.logUserAction("BEC_FORM_ERROR", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
    	loggingService.logUserAction("BEC_FORM_SUCCESS", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    private void closeDialog() {
     try {  
    	 loggingService.logUserAction("BEC_FORM_CLOSE", "Closing BEC form dialog");
      	 if (cancelButton != null) {
    
            Scene scene = cancelButton.getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                Stage stage = (Stage) scene.getWindow();
                stage.close();
            }
        }
    } catch (Exception e) {
        loggingService.logError("DIALOG_CLOSE_ERROR", e);
        e.printStackTrace();
        }
    }
}