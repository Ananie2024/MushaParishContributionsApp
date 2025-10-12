package org.ananie.parishApp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URL;

import org.ananie.parishApp.model.Faithful;
import org.ananie.parishApp.model.SubParish;
import org.ananie.parishApp.services.LoggingService;
import org.ananie.parishApp.utilities.ViewPaths; 

@Component
public class HomePageController {

    @FXML private Label topLabel;
    @FXML private Label bottomLabel;
    @FXML private Button addFaithfulBtn;
    @FXML private Button manageFaithfulsBtn;
    @FXML private StackPane contentArea;
    @FXML private Button addSubParishBtn;
    @FXML private Button addBECBtn;
    @FXML private Button logoutButton;
    
    // This instance will be set AFTER FaithfulContributionsManager.fxml is loaded
    private FaithfulContributionsManagerController faithfulManagerControllerInstance;
    //Field to store the original content of the StackPane
    private Parent initialContentArea;
    private final LoggingService loggingService;
    private final ApplicationContext applicationContext;
    // Constructor injection for ApplicationContext
    public HomePageController(ApplicationContext applicationContext, LoggingService loggingService) {
        this.loggingService = loggingService;
		this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        // Set up button actions to load different views into the contentArea
        loggingService.logUserAction("APPLICATION_STARTED", "Home page initialized");
        
        addFaithfulBtn.setOnAction(event -> {
            loggingService.logUserAction("NAVIGATION", "Opened Add Faithful form");
            loadView(ViewPaths.ADD_OR_EDIT_FAITHFUL, "add", null);
        });
        
        manageFaithfulsBtn.setOnAction(event -> {
            loggingService.logUserAction("NAVIGATION", "Opened Faithful Management view");
            loadView(ViewPaths.FAITHFULCONTRIBUTIONSMANAGER, null, null);
        });
        
        addSubParishBtn.setOnAction(event -> {
            loggingService.logUserAction("NAVIGATION", "Opened Add Sub-Parish dialog");
            openAddSubParishDialog();
        });
        
        addBECBtn.setOnAction(event -> {
            loggingService.logUserAction("NAVIGATION", "Opened Add BEC dialog");
            openAddBECDialog(null);
        });
        
        logoutButton.setOnAction(e -> {
        	loggingService.logUserAction("NAVIGATION", "the user loggged out");
        	try {
				logout();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        });
       
        //Capture the initial content of the contentArea AFTER FXML has loaded it
        if (!contentArea.getChildren().isEmpty()) {
            initialContentArea = (Parent) contentArea.getChildren().get(0);
        }
        
         }
    private void logout() throws IOException {
FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPaths.LOGIN));
		
		loader.setControllerFactory(applicationContext::getBean);
		
		Parent root= loader.load();
		// close the homestage on the logout
		Stage HomePageStage = (Stage) logoutButton.getScene().getWindow();
		HomePageStage.close();
		
		Stage stage = new Stage();
		stage.setTitle("Parish Management");
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
		stage.setScene(scene);
		stage.show();
		
		
		
	}

	public void restoreInitialHomePageContent() {
    	loggingService.logUserAction("NAVIGATION", "Restoring initial home page content");
    	
        if (initialContentArea != null) {
            contentArea.getChildren().setAll(initialContentArea);
        } else {
            // This case should ideally not happen if homepage.fxml is correctly structured
            // and the initial content is present.
        	loggingService.logError("CONTENT_ERROR", 
                    new RuntimeException("Initial content for HomePageController was not captured"));
            System.err.println("Warning: Initial content for HomePageController was not captured.");
            contentArea.getChildren().clear(); // Fallback to clear
        }
    }
    /**
     * Loads an FXML file into the contentArea and handles controller injection via Spring.
     * @param fxmlPath The path to the FXML file (from ViewPaths constants)
     * @param mode Optional: a string indicating a mode for the loaded controller (e.g., "add", "edit")
     * @param data Optional: an object (e.g., a Faithful instance) to pass to the loaded controller
     */
    private void loadView(String fxmlPath, String mode, Object data) {
        try {
        	 loggingService.logUserAction("VIEW_LOAD", 
        	            "Loading view: " + fxmlPath + 
        	            (mode != null ? " (Mode: " + mode + ")" : "") +
        	            (data != null ? " (With data)" : ""));
        	      
            FXMLLoader fxmlLoader = new FXMLLoader();            
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null) {
                    System.err.println("FXML file not found: " + fxmlPath);
                    // Optionally show error to user
                    return;
                }
            }
            fxmlLoader.setLocation(fxmlUrl);

            // Set controller factory to allow Spring to create and inject dependencies into controllers
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            Parent view = fxmlLoader.load();
            Object controller = fxmlLoader.getController();

            // Special handling for controllers that need initialization data
            if (controller instanceof FaithfulContributionsManagerController managerController ) {
                this.faithfulManagerControllerInstance =  managerController;
                managerController.setHomePageController(this);
                  }
            
            if (controller instanceof AddOrEditFaithfulController addOrEditController) {
            	Runnable refreshManagerCallback = () -> {
            		 loggingService.logUserAction("REFRESH","Refreshing faithful manager view after edit");
                    // Check if the FaithfulContributionsManager view is currently loaded
                    // or if its controller instance has been initialized by an FXMLLoader load
                    if (faithfulManagerControllerInstance == null || !contentArea.getChildren().contains(faithfulManagerControllerInstance.getRootNode())) {
                        // If it's not loaded or the current 'view' in contentArea isn't it, load it now
                        // This re-executes loadView to ensure faithfulManagerControllerInstance is populated
                        // and its initialize() method runs.
                    	loggingService.logUserAction("NAVIGATION", "Loading faithful manager view for refresh");
                        loadView(ViewPaths.FAITHFULCONTRIBUTIONSMANAGER, null, null);
                    }
                     // Now that we're sure faithfulManagerControllerInstance is loaded and initialized, refresh it
                        if (faithfulManagerControllerInstance != null) {
                            faithfulManagerControllerInstance.refreshFaithfulsTable();
                        }
                    };
                addOrEditController.initData(mode, (Faithful) data, refreshManagerCallback);
            	
            }  
          
          contentArea.getChildren().setAll(view); // Replace current content
        } catch (IOException e) {
        	loggingService.logError("VIEW_LOAD_FAILED", e);
            System.err.println("Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
            // Show an error alert to the user
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Application Error");
            alert.setContentText("Could not load view: " + fxmlPath + "\n" + e.getMessage());
            alert.showAndWait();
        } catch (ClassCastException e) {
        	loggingService.logError("VIEW_CAST_ERROR", e);
            System.err.println("Type casting error for controller or data: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Development Error");
            alert.setContentText("A programming error occurred when trying to set up the view. Please check logs. " + e.getMessage());
            alert.showAndWait();
        }
    }
 
 private void openAddSubParishDialog() {
     try {
    	 
         FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ViewPaths.ADD_SUBPARISH));
         fxmlLoader.setControllerFactory(applicationContext::getBean);

         Parent root = fxmlLoader.load();
         AddSubParishController controller = fxmlLoader.getController();

         // Initialize for add mode
         controller.initData(null, () -> {
             // Refresh callback - update your ComboBoxes or tables 
        	 if(faithfulManagerControllerInstance!=null) {
        	 faithfulManagerControllerInstance.populateFilterSubParishComboBox(); 
        	 }
         });

         Stage stage = new Stage();
         stage.initModality(Modality.APPLICATION_MODAL);
         stage.setTitle("Santarali Nshya");
         Scene scene = new Scene(root);
         // add styling
         scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
         stage.setScene(scene);
         stage.setResizable(false);
         
         loggingService.logUserAction("DIALOG_OPEN", "Opening Add Sub-Parish dialog");
         stage.showAndWait();
         loggingService.logUserAction("DIALOG_CLOSE", "Add Sub-Parish dialog closed");

     } catch (IOException e) {
    	 loggingService.logError("DIALOG_OPEN_FAILED", e);
    	 System.err.println( e.getMessage());
         e.printStackTrace();
     }
 }

 // Method to open Add BEC dialog
 private void openAddBECDialog(SubParish preselectedSubParish) {
     try {
         FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ViewPaths.ADD_BEC));
         fxmlLoader.setControllerFactory(applicationContext::getBean);

         Parent root = fxmlLoader.load();
         AddBECController controller = fxmlLoader.getController();

         // Initialize for add mode with optional preselected SubParish
         controller.initData(null, preselectedSubParish, () -> {
             // Refresh callback - update your ComboBoxes or tables
        	 if(faithfulManagerControllerInstance!=null) {
        	 faithfulManagerControllerInstance.populateFilterBecComboBox(preselectedSubParish); 
        	 }
         });

         Stage stage = new Stage();
         stage.initModality(Modality.APPLICATION_MODAL);
         stage.setTitle("Impuza Nshya");
         Scene scene = new Scene(root);
         // add styling
         scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
         stage.setScene(scene);
         stage.setResizable(false);
         loggingService.logUserAction("DIALOG_OPEN", 
                 "Opening Add BEC dialog" + 
                 (preselectedSubParish != null ? " (Pre-selected SubParish)" : ""));
         stage.showAndWait();
         loggingService.logUserAction("DIALOG_CLOSE", "Add BEC dialog closed");
        
     } catch (IOException e) {
    	 loggingService.logError("DIALOG_OPEN_FAILED", e);
         System.err.println( e.getMessage());
         e.printStackTrace();
     }
 }

}