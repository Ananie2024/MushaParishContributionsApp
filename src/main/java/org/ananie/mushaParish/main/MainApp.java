package org.ananie.mushaParish.main;
import org.ananie.mushaParish.services.LoggingService;
import org.ananie.mushaParish.utilities.ViewPaths;
import org.ananie.parishApp.configurations.AppConfig;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MainApp extends Application {

    // Spring's application context
    private ApplicationContext springContext;
    private LoggingService loggingService;
    // This method is called by JavaFX when the application is launched, BEFORE start()
    @Override
    public void init() throws Exception {
    	
        // Initialize plain Spring ApplicationContext
        // We use AnnotationConfigApplicationContext because your AppConfig is annotation-based
        springContext = new AnnotationConfigApplicationContext(AppConfig.class);
        
        loggingService = springContext.getBean(LoggingService.class);
        loggingService.logApplicationStartup();
        loggingService.logSystemResources();
        
        
    }

    // This is the main entry point for JavaFX applications
    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            
            // FXML files are directly in src/main/resources, so the path is like /filename.fxml
            URL fxmlUrl = getClass().getResource(ViewPaths.LOGIN);
            if (fxmlUrl == null) {
                System.err.println("Error: FXML file not found at " + ViewPaths.LOGIN);
                // Optionally show error to user via Alert
                Platform.exit();
                return;
            }
            fxmlLoader.setLocation(fxmlUrl);

            // Set the controller factory to allow Spring to create and manage JavaFX controllers
            // This is crucial for @Autowired to work in your controllers
            fxmlLoader.setControllerFactory(springContext::getBean);

            // Load the FXML file
            Parent root = fxmlLoader.load();

            // Set up the primary stage (your main window)
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(ViewPaths.STYLE).toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
            loggingService.logUserAction("POROGARAMU ITANGIYE GUKORA", "JAVAFX UI iratangiye");

        } catch (Exception e) {
            System.err.println("Failed to load the primary FXML view or start application:");
            e.printStackTrace();
            // Show an error dialog before exiting for better user experience
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Application Startup Error");
                alert.setHeaderText("Failed to Start Application");
                alert.setContentText("An unexpected error occurred during application startup: \n" + e.getMessage());
                alert.showAndWait();
            });
            Platform.exit(); // Ensure application exits cleanly
        }
    }

    // This method is called when the application is closing
    @Override
    public void stop() {
    	if (loggingService != null) {
            loggingService.logApplicationShutdown();
            loggingService.logSystemResources();
            
            // Clean old logs (keep last 30 days)
            loggingService.cleanOldLogs(30);}
    	// Force flush async appenders
    	LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
        
        // Close the Spring application context gracefully
        if (springContext instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) springContext).close();
        }
        Platform.exit(); // Ensure JavaFX platform exits
        System.exit(0);
    }

    // Standard main method to launch the JavaFX application
    public static void main(String[] args) {
    	configureLogback();
        // This is how you launch a JavaFX application
    	System.setProperty("file.encoding","UTF-8");
    	System.out.println("Current working directory: " + System.getProperty("user.dir"));
    	launch(args);
    }

	private static void configureLogback() {
		LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
		try { JoranConfigurator configurator = new JoranConfigurator();
		     configurator.setContext(loggerContext);
		     loggerContext.reset();
		     
		     InputStream configStream = MainApp.class.getResourceAsStream("/logback-spring.xml");
		     if (configStream == null) {
	                System.err.println("❌ logback-spring.xml not found in classpath!");
	                return;
	            }
		     configurator.doConfigure(configStream);
		     System.out.println("✅ Logback configured successfully");
		
		
	} catch(JoranException e) {
		System.out.println("Failed to  configure logback" + e.getMessage());
		e.printStackTrace();
	}
	}
}