package org.ananie.parishApp.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.services.ContributionService;
import org.ananie.parishApp.services.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class AddContributionController {

    @FXML private Label formTitleLabel;
    @FXML private Label faithfulNameLabel;
    @FXML private TextField amountTextField;
    @FXML private TextField yearTextField;
    @FXML private DatePicker datePicker;
    @FXML private TextArea notesTextArea;
    @FXML private Button saveContributionButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;

    private final ContributionService contributionService;
    private final LoggingService loggingService;
    private Faithful selectedFaithful; // The faithful for whom the contribution is made
    private Contribution currentContribution; // If editing an existing contribution
    private Runnable refreshCallback;

    @Autowired
    public AddContributionController(ContributionService contributionService, LoggingService loggingService) {
        this.contributionService = contributionService;
		this.loggingService = loggingService;
    }

    @FXML
    public void initialize() {
        loggingService.logUserAction("CONTRIBUTION_FORM", "Contribution form initialized");
        
        saveContributionButton.setOnAction(event -> {
            loggingService.logUserAction("CONTRIBUTION_SAVE_ATTEMPT", "Attempting to save contribution");
            saveContribution();
            clearFields();
        });
        
        cancelButton.setOnAction(event -> {
            loggingService.logUserAction("CONTRIBUTION_FORM_CANCEL", "Cancelled contribution form");
            clearFormAndNavigateBack();
        });
        
        statusLabel.setText("");
        
        // Default to today's date for contribution date
        datePicker.setValue(LocalDate.now());
        
        // Default to current year for contribution year
        yearTextField.setText(String.valueOf(LocalDate.now().getYear()));
    }
     /**
      *Initializes the controller with data for adding/editing a contribution.
     * @param faithful The Faithful object for whom the contribution is made. (Required for Add)
     * @param contributionToEdit The Contribution object if in "edit" mode, null otherwise.
     * @param managerController Reference to the manager controller for refreshing its table.
     */
    public void initData(Faithful faithful, Contribution contributionToEdit, Runnable refreshCallback) {
        this.selectedFaithful = faithful;
        this.currentContribution = contributionToEdit;
        this.refreshCallback = refreshCallback;

        if (selectedFaithful != null) {
            loggingService.logUserAction("CONTRIBUTION_INIT", 
                "Initializing form for faithful: " + faithful.getName() + 
                (contributionToEdit != null ? " (edit mode)" : " (add mode)"));
            faithfulNameLabel.setText("ITURO RYA: " + selectedFaithful.getName());
        } else {
            loggingService.logUserAction("CONTRIBUTION_INIT_ERROR", "No faithful selected");
            faithfulNameLabel.setText("Nta mukristu wahisemo");
            saveContributionButton.setDisable(true);
        }

        if (currentContribution != null) {
            formTitleLabel.setText("ANDIKA ITURO");
            populateFields(currentContribution);
        } else {
            formTitleLabel.setText("ITURO RISHYA");
            clearFields();
        }
    }
    
    private void populateFields(Contribution contribution) {
        loggingService.logUserAction("CONTRIBUTION_FORM_POPULATE", 
            "Populating fields for contribution ID: " + contribution.getId());
        amountTextField.setText(contribution.getAmount().toPlainString());
        yearTextField.setText(String.valueOf(contribution.getYear()));
        datePicker.setValue(contribution.getDate());
        notesTextArea.setText(contribution.getNotes());
    }
    
    private void saveContribution() {
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: black;");

        // Client-side validation
        if (selectedFaithful == null) {
            loggingService.logUserAction("CONTRIBUTION_VALIDATION_FAIL", "No faithful selected");
            showError("Nta mukristu wahisemo");
            return;
        }

        // Validate amount
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountTextField.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            loggingService.logUserAction("CONTRIBUTION_VALIDATION_FAIL", "Invalid amount: " + amountTextField.getText());
            showError("Shyiramo amafranga atanze");
            return;
        }

        // Validate year
        int contributionYear;
        try {
            contributionYear = Integer.parseInt(yearTextField.getText().trim());
            int currentYear = LocalDate.now().getYear();
            if (contributionYear < 1900 || contributionYear > currentYear + 5) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            loggingService.logUserAction("CONTRIBUTION_VALIDATION_FAIL", "Invalid year: " + yearTextField.getText());
            showError("Shyiramo umwaka mwiza (urugero: " + LocalDate.now().getYear() + ")");
            return;
        }

        // Validate date
        LocalDate date = datePicker.getValue();
        if (date == null) {
            loggingService.logUserAction("CONTRIBUTION_VALIDATION_FAIL", "No date selected");
            showError("Shyiramo itariki yo gutanga");
            return;
        }

        Contribution contributionToSave = (currentContribution != null) ? currentContribution : new Contribution();
        contributionToSave.setFaithful(selectedFaithful);
        contributionToSave.setAmount(amount);
        contributionToSave.setDate(date);
        contributionToSave.setYear(contributionYear);
        contributionToSave.setNotes(notesTextArea.getText().trim());

        try {
            if (contributionToSave.getId() == null) {
                contributionService.save(contributionToSave);
                loggingService.logUserAction("CONTRIBUTION_ADD_SUCCESS", 
                    "Added contribution: " + amount + " RWF for " + selectedFaithful.getName() + 
                    " (Year: " + contributionYear + ")");
                showSuccess("Ituro rya " + selectedFaithful.getName() + " rw'umwaka " + contributionYear + " ryanditswe neza");
                clearFields();
            } else {
                contributionService.update(contributionToSave);
                loggingService.logUserAction("CONTRIBUTION_UPDATE_SUCCESS", 
                    "Updated contribution ID " + contributionToSave.getId() + 
                    " to " + amount + " RWF for " + selectedFaithful.getName());
                showSuccess("Ituro rya " + selectedFaithful.getName() + " rw'umwaka " + contributionYear + " ryanditswe neza");
            }

            if (refreshCallback != null) {
                loggingService.logUserAction("CONTRIBUTION_REFRESH", "Triggering parent view refresh");
                refreshCallback.run();
            }

        } catch (IllegalArgumentException e) {
            loggingService.logError("CONTRIBUTION_VALIDATION_ERROR", e);
            showError(e.getMessage());
        } catch (Exception e) {
            loggingService.logError("CONTRIBUTION_SAVE_ERROR", e);
            showError("Ikosa ritunguranye: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFields() {
        loggingService.logUserAction("CONTRIBUTION_FORM_CLEAR", "Clearing form fields");
        amountTextField.setText("");
        yearTextField.setText(String.valueOf(LocalDate.now().getYear()));
        datePicker.setValue(LocalDate.now());
        notesTextArea.setText("");
        currentContribution = null;
        formTitleLabel.setText("TANGA ITURO RYAWE");
    }

    private void showError(String message) {
        loggingService.logUserAction("CONTRIBUTION_FORM_ERROR", message);
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    private void showSuccess(String message) {
        loggingService.logUserAction("CONTRIBUTION_FORM_SUCCESS", message);
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
        loggingService.logUserAction("CONTRIBUTION_FORM_CLOSE", "Closing contribution form");
        clearFields();
        if (refreshCallback != null) {
            refreshCallback.run();
        }
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}