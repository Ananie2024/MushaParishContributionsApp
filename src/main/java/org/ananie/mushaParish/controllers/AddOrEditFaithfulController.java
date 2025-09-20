package org.ananie.mushaParish.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.ananie.mushaParish.model.BEC;
import org.ananie.mushaParish.model.Faithful;
import org.ananie.mushaParish.model.SubParish;
import org.ananie.mushaParish.services.BECService;
import org.ananie.mushaParish.services.FaithfulService;
import org.ananie.mushaParish.services.LoggingService;
import org.ananie.mushaParish.services.SubParishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddOrEditFaithfulController {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameTextField;
    @FXML private TextField contactNumberTextField;
    @FXML private TextField addressTextField;
    @FXML private ComboBox<SubParish> subParishComboBox;
    @FXML private ComboBox<BEC> becComboBox;
    @FXML private TextField baptismYearTextField;
    @FXML private TextField occupationTextField;
    @FXML private Button saveFaithfulButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private HBox specialCharsToolbar;

    private final FaithfulService faithfulService;
    private final SubParishService subParishService;
    private final BECService becService;
    private final LoggingService loggingService;

    private Faithful currentFaithful; // To hold the faithful being edited
    private Runnable refreshCallback;
	private int lastCaretPosition;
    // Constructor injection
    @Autowired
    public AddOrEditFaithfulController(FaithfulService faithfulService, SubParishService subParishService, BECService becService, LoggingService loggingService) {
        this.faithfulService = faithfulService;
        this.subParishService = subParishService;
        this.becService = becService;
		this.loggingService = loggingService;
    }

    @FXML
    public void initialize() {
        loggingService.logUserAction("FAITHFUL_FORM", "Faithful form initialized");
     // Toggle toolbar visibility based on focus
        nameTextField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                showToolbar();
                // Update caret position when field gains focus
                lastCaretPosition = nameTextField.getCaretPosition();
            }
        });
        
        // Handle character insertion
        setupSpecialCharButtons();
     // Track caret position whenever it changes
        nameTextField.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            lastCaretPosition = newPos.intValue();
        });
        // Track when user clicks in the text field to position caret
        nameTextField.setOnMouseClicked(e -> {
            // Use Platform.runLater to ensure the caret position is updated after the click
            Platform.runLater(() -> {
                lastCaretPosition = nameTextField.getCaretPosition();
            });
        });

        // Track when user uses keyboard to move caret
        nameTextField.setOnKeyPressed(e -> {
            // Update on key press to catch navigation keys
            Platform.runLater(() -> {
                if (nameTextField.isFocused()) {
                    lastCaretPosition = nameTextField.getCaretPosition();
                }
            });
        });

        // Hide toolbar if user clicks anywhere else
        nameTextField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    Node target = (Node) event.getTarget();

                    boolean clickedInsideTextField = nameTextField.isFocused();
                    boolean clickedOnToolbar = specialCharsToolbar.isVisible() && isDescendant(specialCharsToolbar, target);

                    if (!clickedInsideTextField && !clickedOnToolbar) {
                        hideToolbar();
                    }
                });
            }
        });
            
        populateSubParishComboBox();

        subParishComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loggingService.logUserAction("FAITHFUL_FORM_SUBPARISH_SELECT", 
                    "Selected sub-parish: " + newVal.getName());
                populateBecComboBox(newVal);
            } else {
                loggingService.logUserAction("FAITHFUL_FORM_SUBPARISH_CLEAR", "Cleared sub-parish selection");
                becComboBox.getItems().clear();
                becComboBox.setDisable(true);
            }
        });

        saveFaithfulButton.setOnAction(event -> {
            loggingService.logUserAction("FAITHFUL_SAVE_ATTEMPT", "Attempting to save faithful");
            saveFaithful();
            clearFields();
        });
        
        cancelButton.setOnAction(event -> {
            loggingService.logUserAction("FAITHFUL_FORM_CANCEL", "Cancelled the faithful form");
            clearFormAndNavigateBack();
        });
        
        statusLabel.setText("");
    }

    /**
     * Initializes the controller with data for editing or setting up add mode.
     * This method is called by HomePageController or FaithfulContributionsManagerController.
     * @param mode "add" or "edit"
     * @param faithfulToEdit The Faithful object if in "edit" mode, null otherwise.
     * @param managerController Reference to the manager controller for refreshing its table.
     */
    public void initData(String mode, Faithful faithfulToEdit, Runnable refreshCallback) {
        this.currentFaithful = faithfulToEdit;
        this.refreshCallback = refreshCallback;

        if ("edit".equals(mode) && faithfulToEdit != null) {
            loggingService.logUserAction("FAITHFUL_EDIT_MODE", 
                "Editing faithful: " + faithfulToEdit.getName() + " (ID: " + faithfulToEdit.getId() + ")");
            formTitleLabel.setText("UMWIRONDORO: " + faithfulToEdit.getName());
            populateFields(faithfulToEdit);
        } else {
            loggingService.logUserAction("FAITHFUL_ADD_MODE", "Adding new faithful");
            formTitleLabel.setText("UMWIRONDORO");
            clearFields();
        }
    }

    // Overload for when only mode is passed (from HomePageController's Add New Faithful)
    public void initData(String mode,Runnable refreshCallback) {
        initData(mode, null,refreshCallback);
    }


private void populateFields(Faithful faithful) {
    loggingService.logUserAction("FAITHFUL_FORM_POPULATE", 
        "Populating fields for faithful: " + faithful.getName() + " (ID: " + faithful.getId() + ")");
        
    nameTextField.setText(faithful.getName());
    contactNumberTextField.setText(faithful.getContactNumber());
    addressTextField.setText(faithful.getAddress());
    baptismYearTextField.setText(faithful.getBaptismYear());
    occupationTextField.setText(faithful.getOccupation());

    if (faithful.getBec() != null && faithful.getBec().getSubParish() != null) {
        subParishComboBox.getSelectionModel().select(faithful.getBec().getSubParish());
        Platform.runLater(() -> {
            if (faithful.getBec() != null) {
                becComboBox.getSelectionModel().select(faithful.getBec());
            }
        });
    }
}

    private void populateSubParishComboBox() {
        try {
            List<SubParish> subParishes = subParishService.findAllOrderedByName();
            loggingService.logUserAction("FAITHFUL_FORM_DATA", 
                "Loaded " + subParishes.size() + " sub-parishes for selection");
                
            ObservableList<SubParish> observableSubParishes = FXCollections.observableArrayList(subParishes);
            subParishComboBox.setItems(observableSubParishes);
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
            subParishComboBox.setPromptText("Hitamo santarali");
        } catch (Exception e) {
            loggingService.logError("FAITHFUL_FORM_DATA_ERROR", e);
            e.printStackTrace();
        }
    }

    private void populateBecComboBox(SubParish subParish) {
        try {
            becComboBox.getItems().clear();
            becComboBox.setDisable(false);
            becComboBox.setPromptText("Hitamo impuza");
            
            if (subParish != null) {
                List<BEC> becs = becService.findBySubParish(subParish);
                loggingService.logUserAction("FAITHFUL_FORM_DATA", 
                    "Loaded " + becs.size() + " BECs for sub-parish: " + subParish.getName());
                    
                ObservableList<BEC> observableBECs = FXCollections.observableArrayList(becs);
                becComboBox.setItems(observableBECs);
                becComboBox.setConverter(new StringConverter<BEC>() {
                    @Override
                    public String toString(BEC object) {
                        return object == null ? null : object.getName();
                    }
                    @Override
                    public BEC fromString(String string) {
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            loggingService.logError("FAITHFUL_FORM_DATA_ERROR", e);
             e.printStackTrace();
        }
    }
    private void saveFaithful() {
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: black;");

        String name = nameTextField.getText();
        String contactNumber = contactNumberTextField.getText();
        String address = addressTextField.getText();
        String baptismYear = baptismYearTextField.getText();
        String occupation = occupationTextField.getText();
        BEC selectedBec = becComboBox.getSelectionModel().getSelectedItem();

        // Validation with logging
        if (name == null || name.trim().isEmpty()) {
            loggingService.logUserAction("FAITHFUL_VALIDATION_FAIL", "Empty name rejected");
            showError("Nta zina wanditse!");
            return;
        }
        if (selectedBec == null) {
            loggingService.logUserAction("FAITHFUL_VALIDATION_FAIL", "No BEC selected");
            showError("Nta mpuza wahisemo!");
            return;
        }

        Faithful faithfulToSave = (currentFaithful != null) ? currentFaithful : new Faithful();
        faithfulToSave.setName(name);
        faithfulToSave.setContactNumber(contactNumber);
        faithfulToSave.setAddress(address);
        faithfulToSave.setBaptismYear(baptismYear);
        faithfulToSave.setOccupation(occupation);
        faithfulToSave.setBec(selectedBec);

        try {
            if (faithfulToSave.getId() == null) {
                faithfulService.save(faithfulToSave);
                loggingService.logUserAction("FAITHFUL_ADD_SUCCESS", 
                    "Added new faithful: " + name + " (ID: " + faithfulToSave.getId() + 
                    "), BEC: " + selectedBec.getName());
                showSuccess(faithfulToSave.getName() + " Yanditswe neza");
                clearFields();
            } else {
                faithfulService.update(faithfulToSave);
                loggingService.logUserAction("FAITHFUL_UPDATE_SUCCESS", 
                    "Updated faithful: " + name + " (ID: " + faithfulToSave.getId() + 
                    "), BEC: " + selectedBec.getName());
                showSuccess(faithfulToSave.getName() + "Yanditswe neza");
            }

            if (refreshCallback != null) {
                loggingService.logUserAction("FAITHFUL_REFRESH", "Triggering parent view refresh");
                refreshCallback.run();
            }

        } catch (IllegalArgumentException e) {
            loggingService.logError("FAITHFUL_VALIDATION_ERROR", e);
            showError(e.getMessage());
        } catch (Exception e) {
            loggingService.logError("FAITHFUL_SAVE_ERROR", e);
            showError("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void clearFields() {
        loggingService.logUserAction("FAITHFUL_FORM_CLEAR", "Clearing form fields");
        nameTextField.clear();
        contactNumberTextField.clear();
        addressTextField.clear();
        subParishComboBox.getSelectionModel().clearSelection();
        becComboBox.getSelectionModel().clearSelection();
        becComboBox.getItems().clear();
        becComboBox.setDisable(true);
        baptismYearTextField.clear();
        occupationTextField.clear();
        currentFaithful = null; // reset for next operation
    }

    private void showError(String message) {
        loggingService.logUserAction("FAITHFUL_FORM_ERROR", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        loggingService.logUserAction("FAITHFUL_FORM_SUCCESS", message);
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

    private void clearFormAndNavigateBack() {
        loggingService.logUserAction("FAITHFUL_FORM_CLOSE", "Closing faithful form");
        clearFields();
        
        if (refreshCallback != null) {
            refreshCallback.run();
        }
        
        // Try multiple approaches to get the stage and close it
        Stage stage = null;
        
        // First, try using the cancel button
        if (cancelButton != null && cancelButton.getScene() != null) {
            Scene scene = cancelButton.getScene();
            if (scene.getWindow() instanceof Stage) {
                stage = (Stage) scene.getWindow();
            }
        }
        
        // If that fails, try using any other FXML component that might be available
        if (stage == null) {
            Node[] nodesToTry = {
                formTitleLabel, nameTextField, contactNumberTextField, 
                addressTextField, subParishComboBox, becComboBox, 
                baptismYearTextField, occupationTextField, saveFaithfulButton, statusLabel
            };
            
            for (Node node : nodesToTry) {
                if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) {
                    stage = (Stage) node.getScene().getWindow();
                    break;
                }
            }
        }
        
        // If we found a stage, close it
        if (stage != null) {
            try {
                stage.close();
                loggingService.logUserAction("FAITHFUL_FORM_CLOSED", "Successfully closed faithful form");
            } catch (Exception e) {
                loggingService.logError("DIALOG_CLOSE_ERROR", e);
            }
        } else {
            // Log the issue but don't crash the application
            loggingService.logError("DIALOG_CLOSE_ERROR", 
                new RuntimeException("Unable to find Stage - no FXML components are attached to a Scene"));
        }
    }
    private void setupSpecialCharButtons() {
        for (Node node : specialCharsToolbar.getChildren()) {
            if (node instanceof Button btn) {
                // Prevent the button from taking focus when clicked
                btn.setFocusTraversable(false);
                
                btn.setOnAction(e -> {
                    // Use the stored caret position since the text field may lose focus
                    String originalText = nameTextField.getText();
                    String charToInsert = btn.getText();

                    // Ensure we have a valid caret position
                    int caretPos = Math.max(0, Math.min(lastCaretPosition, originalText.length()));

                    String before = originalText.substring(0, caretPos);
                    String after = originalText.substring(caretPos);

                    String newText = before + charToInsert + after;
                    nameTextField.setText(newText);
                    
                    // Calculate new caret position
                    int newCaretPosition = caretPos + charToInsert.length();
                    
                    // Use Platform.runLater to ensure proper focus and caret positioning
                    Platform.runLater(() -> {
                        nameTextField.requestFocus();
                        nameTextField.positionCaret(newCaretPosition);
                        lastCaretPosition = newCaretPosition;
                    });
                });
            }
        }
    }
    private void showToolbar() {
        specialCharsToolbar.setVisible(true);
        specialCharsToolbar.setManaged(true);
    }

    private void hideToolbar() {
        specialCharsToolbar.setVisible(false);
        specialCharsToolbar.setManaged(false);
    }

    /**
     * Recursively check if target is inside the given parent node.
     */
    private boolean isDescendant(Node parent, Node target) {
        while (target != null) {
            if (target == parent) return true;
            target = target.getParent();
        }
        return false;
    }
 }

