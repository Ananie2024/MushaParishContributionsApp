package org.ananie.parishApp.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.ananie.mushaParish.services.BECService;
import org.ananie.mushaParish.services.ContributionService;
import org.ananie.mushaParish.services.FaithfulService;
import org.ananie.mushaParish.services.LoggingService;
import org.ananie.mushaParish.services.SubParishService;
import org.ananie.mushaParish.utilities.FaithfulPDFUtility;
import org.ananie.mushaParish.utilities.ViewPaths;
import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.model.SubParish;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class FaithfulContributionsManagerController {

    // Filter controls
    @FXML private ComboBox<SubParish> filterSubParishComboBox;
    @FXML private ComboBox<BEC> filterBecComboBox;
    @FXML private TextField searchFaithfulTextField;
    @FXML private Button clearFiltersButton;
    @FXML private Button homeButton;
    @FXML private Button showSummaryButton; 
    @FXML private Button overallSummaryButton;
    @FXML private VBox rootPane;
    
    // Faithfuls Table
    @FXML private TableView<Faithful> faithfulsTable;
    @FXML private TableColumn<Faithful, String> faithfulNameCol;
    @FXML private TableColumn<Faithful, String> faithfulBecCol;
    @FXML private TableColumn<Faithful, String> faithfulSubParishCol;

    // Faithful Details
    @FXML private Label detailNameLabel;
    @FXML private Label detailContactLabel;
    @FXML private Label detailAddressLabel;
    @FXML private Label detailBecLabel;
    @FXML private Label detailSubParishLabel;
    @FXML private Label detailBaptismYearLabel;
    @FXML private Label detailOccupationLabel;

    // Faithful actions buttons
    @FXML private Button addNewFaithfulBtn;
    @FXML private Button editFaithfulBtn;
    @FXML private Button deleteFaithfulBtn;

    // Contributions Table
    @FXML private TableView<Contribution> contributionsTable;
    @FXML private TableColumn<Contribution, Integer> contributionYearCol;
    @FXML private TableColumn<Contribution, BigDecimal> contributionAmountCol;
    @FXML private TableColumn<Contribution, LocalDate> contributionDateCol;
   
    // Contribution actions buttons
    @FXML private Button addContributionForSelectedBtn;
    @FXML private Button editContributionBtn;
    @FXML private Button deleteContributionBtn;
    @FXML private Button generatePDFBtn1;
    
    private final FaithfulService faithfulService;
    private final ContributionService contributionService;
    private final SubParishService subParishService;
    private final BECService becService;
    private final FaithfulPDFUtility faithfulPDFUtility;
    private final LoggingService loggingService;
    private final ApplicationContext applicationContext; // For loading other controllers

    private ObservableList<Faithful> faithfulsData;
    private ObservableList<Contribution> contributionsData;

    private Faithful selectedFaithfulInTable; // Holds the currently selected faithful
	private HomePageController homePageController;
	
    @Autowired // Constructor injection
    public FaithfulContributionsManagerController(FaithfulService faithfulService,
                                                ContributionService contributionService,
                                                SubParishService subParishService,
                                                BECService becService,FaithfulPDFUtility faithfulPDFUtility,
                                                LoggingService loggingService,ApplicationContext applicationContext) {
        this.faithfulService = faithfulService;
        this.contributionService = contributionService;
        this.subParishService = subParishService;
        this.becService = becService;
        this.faithfulPDFUtility = faithfulPDFUtility;
		this.loggingService = loggingService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
    	// log the opening of the view 
    	loggingService.logUserAction("VIEW_OPENED", "Faithful Contributions Manager opened");
    	// initialize the ObservableLists that will be used by Tables
    	faithfulsData = FXCollections.observableArrayList();
    	contributionsData = FXCollections.observableArrayList();
        // --- Initialize Tables and Columns ---
        setupFaithfulsTable();
        setupContributionsTable();

        // --- Populate Filter ComboBoxes ---
        populateFilterSubParishComboBox();
        populateAllFaithfuls(); // Load all faithfuls initially

        // --- Set up Listeners ---
        filterSubParishComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            populateFilterBecComboBox(newVal); // Populate BECs based on selected SubParish
            filterFaithfuls(); // Re-filter faithfuls table
        });
        filterBecComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterFaithfuls());
        searchFaithfulTextField.textProperty().addListener((obs, oldVal, newVal) -> filterFaithfuls());
        clearFiltersButton.setOnAction(event -> clearFilters());

        // Listener for Faithfuls Table selection
        faithfulsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedFaithfulInTable = newVal;
            if (newVal != null) {
            	loggingService.logUserAction("FAITHFUL_SELECTED","Selected faithful: " + newVal.getName());
            	
                displayFaithfulDetails(newVal);
                refreshContributionsTable(newVal); // Load contributions for selected faithful
                enableFaithfulActionButtons(true);
                addContributionForSelectedBtn.setDisable(false); // Enable add contribution for selected
            } else {
                clearFaithfulDetails();
                contributionsTable.getItems().clear(); // Clear contributions table
                enableFaithfulActionButtons(false);
                addContributionForSelectedBtn.setDisable(true);
            }
        });

        // Listener for Contributions Table selection (for Edit/Delete Contribution)
        contributionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSelected = newVal != null;
            if(isSelected) {
            loggingService.logUserAction("CONTRIBUTION_SELECTED", 
                    "Selected contribution: " + newVal.getAmount() + " for year " + newVal.getYear());
            }
            editContributionBtn.setDisable(!isSelected);
            deleteContributionBtn.setDisable(!isSelected);
        });

        // --- Set up Buttons Actions ---
        addNewFaithfulBtn.setOnAction(event -> openAddEditFaithfulForm(null)); // Open in add mode
        editFaithfulBtn.setOnAction(event -> {
            Faithful selected = faithfulsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openAddEditFaithfulForm(selected); // Open in edit mode
            } else {
                showAlert(AlertType.WARNING, "Nta mukristu wahisemo", "Hitamo umukristu kuri liste");
            }
        });
        deleteFaithfulBtn.setOnAction(event -> deleteSelectedFaithful());

        addContributionForSelectedBtn.setOnAction(event -> {
            if (selectedFaithfulInTable != null) {
                openAddContributionForm(selectedFaithfulInTable, null); // Open in add mode
            } else {
                showAlert(AlertType.WARNING, "Nta mukristu wahisemo", "Hitamo umukristu ushaka kwandikira ituro kuri liste");
            }
        });
        editContributionBtn.setOnAction(event -> {
            Contribution selectedContribution = contributionsTable.getSelectionModel().getSelectedItem();
            if (selectedContribution != null && selectedFaithfulInTable != null) {
                openAddContributionForm(selectedFaithfulInTable, selectedContribution); // Open in edit mode
            } else {
                showAlert(AlertType.WARNING, "Nta mukristu wahisemo", "Hitamo umukristu ushaka kwandikira ituro kuri liste");
            }
        });
        deleteContributionBtn.setOnAction(event -> deleteSelectedContribution());
        
        // Home and Summary button handlers
        if(homeButton != null) {
            homeButton.setOnAction(e -> {
            	loggingService.logUserAction("NAVIGATION", "Navigated to home page");
            	handleGoHome();}); // return to the home page
        }
        if(showSummaryButton != null) {
            showSummaryButton.setOnAction(e -> {
            	loggingService.logUserAction("REPORT_VIEW", "Opened contribution summary");
            	openContributionSummary();});
        }
        if(overallSummaryButton !=null) {
        	overallSummaryButton.setOnAction(e -> {
        	    loggingService.logUserAction("REPORT_VIEW", "Opened overall summary");
        		handleOverallSummaryButtonAction();});      
        }
        if (generatePDFBtn1 != null) {
            generatePDFBtn1.setOnAction(event -> {
            	loggingService.logUserAction("PDF_EXPORT", "Attempted PDF export");
            	handlePDFExportWithLogo();});
        }
        
        // Initially disable action buttons until a selection is made
        enableFaithfulActionButtons(false);
        addContributionForSelectedBtn.setDisable(true);
        editContributionBtn.setDisable(true);
        deleteContributionBtn.setDisable(true);
    }
    
    public Parent getRootNode() {
        return rootPane; // Returns the FXML root element
    }
    
    public void setHomePageController(HomePageController controller) {
        this.homePageController = controller;
    }
    
    // Handler for the Home button
    private void handleGoHome() {
    	if (homePageController != null) {
            //  Call the method to restore the initial homepage content
            homePageController.restoreInitialHomePageContent();
        }
    }
    
    // Handler for the Summary button - opens contribution summary dialog
    private void openContributionSummary() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ViewPaths.CONTRIBUTION_SUMMARY));
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("IBITERANYO BY'AMATURO");
            stage.setResizable(true);
            Scene scene = new Scene(root);
            // add styling
            scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
        	loggingService.logError("OPERATION (openContributionSummary) FAILED", e);
            showAlert(AlertType.ERROR, "Ikosa", "Ntabwo byashoboye gufunguka: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Handler for the overallSummary Button
    @FXML
    private void handleOverallSummaryButtonAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPaths.CONTRIBUTION_REPORT));
            loader.setControllerFactory(applicationContext::getBean);           
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
            Stage stage = new Stage();
            stage.setTitle("IMBONERAHAMWE Y'AMATURO");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
        	loggingService.logError("OPERATION (openOverallSummary) FAILED", e);
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Ikosa", "Ntabwo byashoboye gufunguka: " + e.getMessage());
        }
    }


    // --- Table Setup Methods ---
    private void setupFaithfulsTable() {
        faithfulNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        //Display BEC name
        faithfulBecCol.setCellValueFactory(cellData -> {
            Faithful faithful = cellData.getValue();
            if (faithful == null) return new SimpleStringProperty("N/A");
            
            try {
                BEC bec = faithful.getBec();
                return new SimpleStringProperty(bec != null ? bec.getName() : "N/A");
            } catch (ObjectNotFoundException e) {
            	loggingService.logError("OPERATION (setupFaithfulsTable) FAILED", e);
                return new SimpleStringProperty("N/A");
            }
        });
        
        // Display Sub-Parish name (through BEC)
        faithfulSubParishCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBec() != null && cellData.getValue().getBec().getSubParish() != null ?
                                     cellData.getValue().getBec().getSubParish().getName() : "N/A"));

        faithfulsTable.setItems(faithfulsData);
        faithfulsTable.setPlaceholder(new Label("Nta bakristu bagaragara"));
    }

    private void setupContributionsTable() {
        contributionYearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        contributionAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        contributionDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        contributionsTable.setItems(contributionsData);
        contributionsTable.setPlaceholder(new Label("Nta maturo agaragara"));
    }

    // --- Data Population & Filtering Methods ---
    public void populateFilterSubParishComboBox() {
        List<SubParish> subParishes = subParishService.findAllOrderedByName();
        filterSubParishComboBox.setItems(FXCollections.observableArrayList(subParishes));
        filterSubParishComboBox.setConverter(new StringConverter<SubParish>() {

			@Override
			public String toString(SubParish object) {
				return object == null ? null: object.getName();
			}

			@Override
			public SubParish fromString(String string) {
				return null;
			}
        });
        filterSubParishComboBox.getSelectionModel().clearSelection();
        filterBecComboBox.getSelectionModel().clearSelection();
        filterBecComboBox.setDisable(true);
    }

    public void populateFilterBecComboBox(SubParish subParish) {
        filterBecComboBox.getItems().clear();
        filterBecComboBox.setDisable(true); // Disable until a SubParish is selected
        if (subParish != null) {
            List<BEC> becs = becService.findBySubParish(subParish);
            filterBecComboBox.setItems(FXCollections.observableArrayList(becs));
            filterBecComboBox.setConverter( new StringConverter<BEC> () {

				@Override
				public String toString(BEC object) {
					return object == null ? null:object.getName();
				}

				@Override
				public BEC fromString(String string) {
					return null;
				}
            });
            filterBecComboBox.setDisable(false); // Enable once populated
            filterBecComboBox.getSelectionModel().clearSelection();
        }
    }

    public void populateAllFaithfuls() {
        List<Faithful> allFaithfuls = faithfulService.findAll();
        faithfulsData.setAll(allFaithfuls);
    }

    private void filterFaithfuls() {
    	// Add null check to prevent crash
        if (filterSubParishComboBox == null && filterBecComboBox == null) {
            System.err.println("WARNING: filterSubParishComboBox is null - FXML not loaded properly");
            // Load all faithfuls as fallback
            populateAllFaithfuls();
            return; }
                
        SubParish selectedSubParish = filterSubParishComboBox.getSelectionModel().getSelectedItem();
        BEC selectedBec = filterBecComboBox.getSelectionModel().getSelectedItem();
        String searchText = searchFaithfulTextField.getText();
        
        loggingService.logUserAction("FILTER_APPLIED", 
        	    "Filtered faithfuls - SubParish: " + (selectedSubParish != null ? selectedSubParish.getName() : "None") + 
        	    ", BEC: " + (selectedBec != null ? selectedBec.getName() : "None") + 
        	    ", Search: " + (searchText != null ? searchText : "None"));
        
        List<Faithful> filteredList;

        if (selectedSubParish != null && selectedBec != null) {
            filteredList = faithfulService.findByBec(selectedBec);
        } else if (selectedSubParish != null) {
            filteredList = faithfulService.findBySubParish(selectedSubParish);
        } else {
            filteredList = faithfulService.findAll(); // No sub-parish filter
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredList = filteredList.stream()
                .filter(f -> f.getName().toLowerCase().contains(lowerCaseSearchText) ||
                             (f.getContactNumber() != null && f.getContactNumber().toLowerCase().contains(lowerCaseSearchText)) ||
                             (f.getAddress() != null && f.getAddress().toLowerCase().contains(lowerCaseSearchText)) ||
                             (f.getOccupation() != null && f.getOccupation().toLowerCase().contains(lowerCaseSearchText)))
                .toList();
        }
        faithfulsData.setAll(filteredList);
    }

    private void clearFilters() {
        filterSubParishComboBox.getSelectionModel().clearSelection();
        filterBecComboBox.getSelectionModel().clearSelection();
        searchFaithfulTextField.clear();
        loggingService.logUserAction("FILTER_CLEARED", "Cleared all filters");
        populateAllFaithfuls(); // Reload all faithfuls
    }

    // --- Faithful Details and Actions ---
    private void displayFaithfulDetails(Faithful faithful) {
        detailNameLabel.setText(faithful.getName());
        detailContactLabel.setText(faithful.getContactNumber() != null ? faithful.getContactNumber() : "N/A");
        detailAddressLabel.setText(faithful.getAddress() != null ? faithful.getAddress() : "N/A");
        detailBecLabel.setText(faithful.getBec() != null ? faithful.getBec().getName() : "N/A");
        detailSubParishLabel.setText(faithful.getBec() != null && faithful.getBec().getSubParish() != null ?
                                      faithful.getBec().getSubParish().getName() : "N/A");
        detailBaptismYearLabel.setText(faithful.getBaptismYear() != null ? faithful.getBaptismYear() : "N/A");
        detailOccupationLabel.setText(faithful.getOccupation() != null ? faithful.getOccupation() : "N/A");
    }

    private void clearFaithfulDetails() {
        detailNameLabel.setText("");
        detailContactLabel.setText("");
        detailAddressLabel.setText("");
        detailBecLabel.setText("");
        detailSubParishLabel.setText("");
        detailBaptismYearLabel.setText("");
        detailOccupationLabel.setText("");
    }

    private void enableFaithfulActionButtons(boolean enable) {
        editFaithfulBtn.setDisable(!enable);
        deleteFaithfulBtn.setDisable(!enable);
    }

    // This method is public so other controllers can call it to refresh the table
    public void refreshFaithfulsTable() {
        filterFaithfuls(); // Re-apply current filters to refresh
        faithfulsTable.getSelectionModel().clearSelection(); // Clear selection after refresh
    }
   
    private void deleteSelectedFaithful() {
        Faithful selected = faithfulsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "HITAMO", "Please! Hitamo witonze uwo ushaka gusiba.");
            return;
        }

        // Log the selected faithful's ID for debugging
        loggingService.logUserAction("DELETE_DEBUG", "Selected faithful ID: " + selected.getId() + ", Name: " + selected.getName());

        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Emeza gusiba");
        confirmAlert.setHeaderText("Ushaka gusiba " + selected.getName() + "?");
        confirmAlert.setContentText("Uzi neza ko biribusibe n'ahanditse amaturo yari yaratanze yose?");

        ButtonType deleteButton = new ButtonType("Siba uyu muntu");
        ButtonType cancelButton = new ButtonType("Bireke");
        confirmAlert.getButtonTypes().setAll(deleteButton, cancelButton);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == deleteButton) {
            try {
                loggingService.logUserAction("FAITHFUL_DELETE", "Starting delete for faithful ID: " + selected.getId());
                
                // Debug: Log table size before deletion
                int sizeBeforeDelete = faithfulsTable.getItems().size();
                loggingService.logUserAction("DELETE_DEBUG", "Table size before delete: " + sizeBeforeDelete);
                // First delete all contributions for this faithful
                contributionService.delete(selected.getId());
                
                // Call the delete service
                faithfulService.deleteFaithful(selected.getId());
                
                loggingService.logUserAction("FAITHFUL_DELETE", "faithfulService.delete() completed for ID: " + selected.getId());
                
                refreshFaithfulsTable();
                // Debug: Check if faithful still exists in database
                try {
                    Optional<Faithful> checkDeleted = faithfulService.findById(selected.getId());
                    if (checkDeleted != null) {
                        loggingService.logUserAction("DELETE_WARNING", "Faithful still exists in database after delete! ID: " + selected.getId());
                    } else {
                        loggingService.logUserAction("DELETE_SUCCESS", "Faithful successfully removed from database. ID: " + selected.getId());
                    }
                } catch (Exception checkEx) {
                    loggingService.logUserAction("DELETE_SUCCESS", "Faithful not found in database (good - means deleted). ID: " + selected.getId());
                }
                
                // Refresh the table
                loggingService.logUserAction("DELETE_DEBUG", "Starting table refresh...");
                refreshFaithfulsTable();
                
                // Debug: Log table size after refresh
                int sizeAfterRefresh = faithfulsTable.getItems().size();
                loggingService.logUserAction("DELETE_DEBUG", "Table size after refresh: " + sizeAfterRefresh);
                
                // Check if the deleted item is still in the table
                boolean stillInTable = faithfulsTable.getItems().stream()
                    .anyMatch(f -> f.getId().equals(selected.getId()));
                
                if (stillInTable) {
                    loggingService.logUserAction("DELETE_ERROR", "Faithful still appears in table after refresh! ID: " + selected.getId());
                } else {
                    loggingService.logUserAction("DELETE_SUCCESS", "Faithful removed from table successfully. ID: " + selected.getId());
                }
                
                showAlert(AlertType.INFORMATION, "Byakunze", selected.getName() + " yasibwe burundu");
                
            } catch (Exception e) {
                loggingService.logError("OPERATION (deleteSelectedFaithful) FAILED", e);
                showAlert(AlertType.ERROR, "BYANZE", "Gusiba uyu muntu ntibikunze: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            loggingService.logUserAction("FAITHFUL_DELETE_CANCELLED", "User cancelled deletion of faithful: " + selected.getName());
        }
    }

    // --- Contribution Actions ---
    // This method is public so other controllers can call it to refresh the contributions table
    public void refreshContributionsTable(Faithful faithful) {
        if (faithful != null) {
            List<Contribution> contributions = contributionService.findByFaithful(faithful);
            contributionsData.setAll(contributions);
        } else {
            contributionsData.clear();
        }
    }

    private void deleteSelectedContribution() {
        Contribution selectedContribution = contributionsTable.getSelectionModel().getSelectedItem();
        if (selectedContribution == null) {
            showAlert(AlertType.WARNING, "NTA TURO WAHISEMO", "Please! Hitamo ituro ushaka gusiba.");
            return;
        }

        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Emeza gusiba");
        confirmAlert.setHeaderText("ushaka gusiba ituro?");
        confirmAlert.setContentText(" Ushaka gusiba iri turo ringana n' " + selectedContribution.getAmount() + " RWF ryatanzwe " + selectedContribution.getFaithful().getName() + "?");
        
        ButtonType openButton = new ButtonType("Siba ituro");
        ButtonType okButton = new ButtonType("Bireke");
        confirmAlert.getButtonTypes().setAll(openButton, okButton);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == openButton ) {
            try {
            	loggingService.logUserAction("CONTRIBUTION_DELETE", 
            		    "Attempted to delete contribution: " + selectedContribution.getAmount() + " for " + selectedContribution.getFaithful().getName());
                contributionService.delete(selectedContribution.getId());
                showAlert(AlertType.INFORMATION, "Byakunze", "Ituro ryasibwe neza");
                refreshContributionsTable(selectedFaithfulInTable); // Refresh only contributions for current faithful
            } catch (Exception e) {
            	loggingService.logError("OPERATION (deleteSelectedContribution) FAILED", e);
                showAlert(AlertType.ERROR, "BYANZE", "Gusiba ituro byanze: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // --- Helper for opening new forms (Add/Edit Faithful, Add/Edit Contribution) ---
    private void openAddEditFaithfulForm(Faithful faithfulToEdit) {
        try {
        	if (faithfulToEdit == null) {
        	    loggingService.logUserAction("FAITHFUL_ADD", "Opened add faithful form");
        	} else {
        	    loggingService.logUserAction("FAITHFUL_EDIT", 
        	        "Opened edit form for faithful: " + faithfulToEdit.getName());
        	}
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ViewPaths.ADD_OR_EDIT_FAITHFUL));
            fxmlLoader.setControllerFactory(applicationContext::getBean); // Let Spring inject

            Parent root = fxmlLoader.load();
            AddOrEditFaithfulController controller = fxmlLoader.getController();

            // Pass data and this controller as a callback reference
            controller.initData(faithfulToEdit == null ? "add" : "edit", faithfulToEdit, this::refreshFaithfulsTable);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL); // Makes it a pop-up that blocks main window
            stage.setTitle(faithfulToEdit == null ? "Ongeraho umukristu mushya" : "Hindura umwirondoro");
            Scene scene = new Scene(root);
            // add styling
            scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
            stage.setScene(scene);
            stage.showAndWait(); // Wait for the pop-up to be closed
            // After pop-up is closed, refresh the table (initData already handles this on save)
            // But if user cancels, we still need to refresh in case something else changed.
            // controller.initData already calls refreshFaithfulsTable on save/cancel via its initData callback
        } catch (IOException e) {
        	loggingService.logError("OPERATION (openAddEditFaithfulForm) FAILED", e);
            showAlert(AlertType.ERROR, "AKABAZO MU KUBONA FOMU", "Fomu yo kuzuzaho umwirondoro ntibonetse " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openAddContributionForm(Faithful faithful, Contribution contributionToEdit) {
        try {
        	if (contributionToEdit == null) {
        	    loggingService.logUserAction("CONTRIBUTION_ADD", 
        	        "Opened add contribution form for: " + faithful.getName());
        	} else {
        	    loggingService.logUserAction("CONTRIBUTION_EDIT", 
        	        "Opened edit form for contribution: " + contributionToEdit.getAmount() + " for " + faithful.getName());}
            
        	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ViewPaths.ADD_CONTRIBUTION));
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            Parent root = fxmlLoader.load();
            AddContributionController controller = fxmlLoader.getController();

            // Pass data and this controller as a callback reference
            controller.initData(faithful, contributionToEdit, () -> refreshContributionsTable(faithful));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(contributionToEdit == null ? "Ituro rishya" : "");
            Scene scene = new Scene(root);
            // add styling
            scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
            stage.setScene(scene);
            stage.showAndWait(); // Wait for the pop-up to be closed
            // refreshContributionsTable will be called by AddContributionController on save/cancel
        } catch (IOException e) {
        	loggingService.logError("OPERATION (openAddContributionForm) FAILED", e);
            showAlert(AlertType.ERROR, "AKABAZO MU KUZANA FOMU", " Fomu buzuzaho ituro ntiraza: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * PDF export with custom logo 
     */
    private void handlePDFExportWithLogo() {
        if (selectedFaithfulInTable == null) {
            showAlert(Alert.AlertType.WARNING, "Ikosa", "Hitamo umukristu kuri liste mbere yo gukora ifishi ya PDF.");
            return;
        }
        List<Contribution> currentContributions = contributionsTable.getItems();
        Stage stage = (Stage) generatePDFBtn1.getScene().getWindow();
        
        faithfulPDFUtility.exportWithConfirmation(selectedFaithfulInTable, currentContributions, stage);
        loggingService.logUserAction("PDF_EXPORT_WITH_LOGO", 
        	    "Exported PDF with logo for faithful: " + selectedFaithfulInTable.getName());
    }
    // --- General Alert Helper ---
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}