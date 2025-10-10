package org.ananie.parishApp.controllers;

import java.io.IOException;

import org.ananie.mushaParish.utilities.ViewPaths;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class LoginController {
	@FXML 
	private Label usernameLabel;
	@FXML
	private Label passwordLabel;
	@FXML
	private Label errorLabel;
	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;
	@FXML 
	private Button loginButton;
	
	private final ApplicationContext applicationContext;
	private final AuthenticationManager authenticationManager;
	
	public LoginController (ApplicationContext applicationContext, AuthenticationManager authenticationManager) {
		this.applicationContext = applicationContext;
		this.authenticationManager = authenticationManager;
	}
	
	@FXML
	public void initialize() {
		loginButton.setOnAction(
			e -> { handleLogin();});
		passwordField.setOnAction(e -> { handleLogin();});
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
		
		Authentication authentication = authenticationManager.authenticate(auth);
		
		SecurityContextHolder.getContext().setAuthentication(authentication);		
		
		
		openHomepage();
		
	} catch(Exception ex) {
		ex.printStackTrace();
	}
	}

	private void openHomepage() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPaths.HOME));
		
		loader.setControllerFactory(applicationContext::getBean);
		
		Parent root= loader.load();
		Stage stage = new Stage();
		stage.setTitle("ParishApp");
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setTitle("Homepage");
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.showAndWait();
		
	}
}