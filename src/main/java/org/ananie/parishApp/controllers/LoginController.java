package org.ananie.parishApp.controllers;

import java.io.IOException;

import org.ananie.parishApp.security.CustomUserDetails;
import org.ananie.parishApp.services.LoggingService;
import org.ananie.parishApp.services.SecurityUserService; // Assuming this is your custom interface
import org.ananie.parishApp.utilities.ViewPaths;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class LoginController {
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private Label errorLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton togglePasswordButton;
    @FXML private StackPane passwordContainer;

    private final ApplicationContext applicationContext;
    private final AuthenticationManager authenticationManager;
    private final SecurityUserService customUserDetailsService;
    private final LoggingService loggingService;
    private boolean passwordVisible = false;
    
    
    public LoginController (ApplicationContext applicationContext, AuthenticationManager authenticationManager, SecurityUserService customUserDetailsService, LoggingService loggingService) {
        this.applicationContext = applicationContext;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.loggingService = loggingService;
    }
    
    @FXML
    public void initialize() {
        // Feature 1: Assure an initial, clean state for a new login attempt.
        usernameField.clear();
        passwordField.clear();
        passwordTextField.clear();
        loginButton.setDisable(false); // Ensure button is enabled
        errorLabel.setText("");
        errorLabel.setVisible(false);
        
        // Initially hide the visible password field
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);
        // Bind bidirectionally to sync text
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        
        // Set initial button text
        togglePasswordButton.setText("SHOW");
        
        // Handle toggle button click
        togglePasswordButton.setOnAction(event -> togglePasswordVisibility());
        
        
        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        passwordTextField.setOnAction(event -> handleLogin());
    }
    

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if(passwordVisible) {
            // show password
             passwordTextField.setVisible(true);
             passwordTextField.setManaged(true);
             passwordField.setVisible(false);
             passwordField.setManaged(false);
             togglePasswordButton.setText("HIDE");
        } else {
            // hide password
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordButton.setText("SHOW");
        }
            
        ;
    }

    private void handleLogin() {
    try {
        String username = usernameField.getText();
        String password = passwordField.getText();
         if (username == null || username.trim().isEmpty()) {
                errorLabel.setText("Shyiramo amazina yawe");
                errorLabel.setVisible(true);
                return;
            }

            if (password == null || password.trim().isEmpty()) {
                errorLabel.setText("Shyiramo ijambo banga");
                errorLabel.setVisible(true);
                return;
            }
            
           
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username,password);
    
        // authenticate
        Authentication authentication = authenticationManager.authenticate(auth);
        //set the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // and update last login
        customUserDetailsService.updateLastLogin(username);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // NOTE: The button disabling logic is removed here to prevent issues when the login stage reloads.
        // If you absolutely must disable it for UX, re-enable it in a finally block.
        
        loggingService.logUserAction("LOGIN_SUCCESS", 
                    "User logged in: " + username + " (" + userDetails.getFullName() + ")");
        
        errorLabel.setVisible(false);
                
        openHomepage();
        
        // NOTE: Field clearing logic is removed here as it is redundant.
        
    } catch(Exception ex) {
        errorLabel.setText("Username or password is incorrect");
        errorLabel.setVisible(true);
        loggingService.logError("LOGIN ERROR", ex);
        ex.printStackTrace();
    }
    }

    private void openHomepage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPaths.HOME));
        
        loader.setControllerFactory(applicationContext::getBean);
        
        Parent root= loader.load();
        Stage stage = new Stage();
        stage.setTitle("ParishApp");
        
        // Retaining Modality as per your previous design to block the login window
        stage.initModality(Modality.WINDOW_MODAL); 
        stage.setTitle("Homepage");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
        stage.setScene(scene);
        
        // Blocks thread until homepage is closed (i.e., user logs out)
        stage.show();
        
        // Execution resumes here after the user logs out from the homepage.
        
        // Close login window that was blocked
        Stage loginStage = (Stage) loginButton.getScene().getWindow();
        loginStage.close();
        
    }
}