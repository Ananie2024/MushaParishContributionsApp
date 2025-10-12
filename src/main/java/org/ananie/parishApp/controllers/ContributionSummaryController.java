package org.ananie.parishApp.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.ananie.parishApp.model.BEC;
import org.ananie.parishApp.model.SubParish;
import org.ananie.parishApp.services.BECService;
import org.ananie.parishApp.services.ContributionService;
import org.ananie.parishApp.services.LoggingService;
import org.ananie.parishApp.services.SubParishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ContributionSummaryController {
    
    @FXML private ComboBox<Integer> yearFilterComboBox;
    @FXML private Button refreshSummaryButton;
    @FXML private Button showAllYearsButton;
    @FXML private Button closeButton;
    
    @FXML private VBox summaryContentVBox;
    @FXML private Label overallTotalTitleLabel;
    @FXML private Label overallTotalAmountLabel;
    @FXML private VBox subParishSummaryVBox;
    @FXML private VBox becSummaryVBox;
    
    @FXML private ComboBox<SubParish> subParishFilterComboBox;
    @FXML private Button clearSubParishFilterButton;
    @FXML private VBox dynamicSummaryVBox;
    @FXML private Label summaryTitleLabel;
    
    private final ContributionService contributionService;
    private final SubParishService subParishService;
    private final LoggingService loggingService;
    @Autowired
    public ContributionSummaryController(ContributionService contributionService,
                                       SubParishService subParishService,
                                       BECService becService, LoggingService loggingService) {
        this.contributionService = contributionService;
        this.subParishService = subParishService;
		this.loggingService = loggingService;
    }
    
    @FXML
    public void initialize() {
    	loggingService.logUserAction("SUMMARY_VIEW", "Contribution summary view initialized");
        setupYearComboBox();
        setupSubParishComboBox();
        
        refreshSummaryButton.setOnAction(event -> {
        	loggingService.logUserAction("SUMMARY_REFRESH", "Refreshing summary data");
        	refreshSummary();});
        
        showAllYearsButton.setOnAction(event -> {
        	loggingService.logUserAction("SUMMARY_SHOW_ALL", "Showing all years summary");
        	showAllYearsSummary();});
        
        clearSubParishFilterButton.setOnAction(event -> {
        	loggingService.logUserAction("CLEAR_FILTER", "Clearing the subParish filter comboBox");
        	clearSubParishFilter();});
        closeButton.setOnAction(event -> {
        	loggingService.logUserAction("NAVIGATION", "Closing the overall summary view");
        	closeDialog();});
        yearFilterComboBox.setOnAction(event -> refreshCurrentView());
        
        // Load current year summary by default
        yearFilterComboBox.setValue(LocalDate.now().getYear());
        refreshSummary();
    }
    
    private void setupYearComboBox() {
        // Get available years from contributions
        List<Integer> availableYears = contributionService.getAvailableYears();
        yearFilterComboBox.setItems(FXCollections.observableArrayList(availableYears));
        
        yearFilterComboBox.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer year) {
                return year == null ? "" : year.toString();
            }
            
            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });
    }
    
    private void refreshSummary() {
        Integer selectedYear = yearFilterComboBox.getValue();
        loggingService.logUserAction("SUMMARY_FILTER", 
                "Refreshing summary with year filter: " + (selectedYear != null ? selectedYear : "All years"));
        if (selectedYear != null) {
            loadSummaryForYear(selectedYear);
        } else {
            showAllYearsSummary();
        }
    }
    
    private void showAllYearsSummary() {
    	loggingService.logUserAction("SUMMARY_SHOW_ALL", "Displaying summary for all years");
        yearFilterComboBox.setValue(null);
        loadSummaryForYear(null); // null means all years
    }
    
   
    
    private HBox createSummaryRow(String label, String amount, String style) {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setStyle(style);
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        Label amountLabel = new Label(amount);
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        
        // Push amount to the right
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
        
        row.getChildren().addAll(nameLabel, amountLabel);
        return row;
    }
    
    private String formatCurrency(BigDecimal amount) {
     try {	
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
        return formatter.format(amount) + " RWF";
    } catch(Exception e) {
    	loggingService.logError("CURRENCY_FORMAT_ERROR", e);
    	return " RWF ???";
     }
    }
    
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    private void setupSubParishComboBox() {
    try {	
        List<SubParish> subParishes = subParishService.findAllOrderedByName();
        loggingService.logUserAction("FILTER_SETUP", "Loaded " + subParishes.size() + " sub-parishes for filter");
        subParishFilterComboBox.setItems(FXCollections.observableArrayList(subParishes));        
        subParishFilterComboBox.setConverter(new StringConverter<SubParish>() {
            @Override
            public String toString(SubParish subParish) {
                return subParish == null ? "" : subParish.getName();
            }
            
            @Override
            public SubParish fromString(String string) {
                return subParishFilterComboBox.getItems().stream()
                        .filter(sp -> sp.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        
        // Add listener for SubParish selection
        subParishFilterComboBox.setOnAction(event -> {
            SubParish selectedSubParish = subParishFilterComboBox.getValue();
            if (selectedSubParish != null) {
            	loggingService.logUserAction("FILTER_APPLY", 
                        "Selected sub-parish from filter: " + selectedSubParish.getName());
                showBecSummaryForSubParish(selectedSubParish);
            } else {
            	showSubParishSummary();
            }
        });
    } catch (Exception ex) {
    	loggingService.logError("FILTER_SETUP_ERROR", ex);
    	ex.printStackTrace();
       }
    }

    // Add this method to clear SubParish filter:
    private void clearSubParishFilter() {
        subParishFilterComboBox.setValue(null);
        showSubParishSummary();
    }

    // Add this method to show BEC summary for selected SubParish:
    private void showBecSummaryForSubParish(SubParish subParish) {
     try {	
        Integer selectedYear = yearFilterComboBox.getValue();
        loggingService.logUserAction("SUMMARY_DRILLDOWN", 
                "Showing BEC summary for " + subParish.getName() + 
                " (" + (selectedYear != null ? selectedYear : "all years") + ")");
        
        // Clear previous  content
        dynamicSummaryVBox.getChildren().clear();
        
        // Update title
        String titleText = "IGITERANYO CY'ITURO RYA MPUZA ZA " + subParish.getName().toUpperCase();
        if (selectedYear != null) {
            titleText += " (" + selectedYear + ")";
        } else {
            titleText += " (IMYAKA YOSE)";
        }
        summaryTitleLabel.setText(titleText);
        dynamicSummaryVBox.getChildren().add(summaryTitleLabel);
        // Get BEC totals for the selected SubParish
        Map<BEC, BigDecimal> becTotals = contributionService.getTotalsByBecInSubParish(subParish, selectedYear);
        loggingService.logUserAction("SUMMARY_DRILLDOWN_DATA", "Found " + becTotals.size() + " BECs with contributions");
        if (!becTotals.isEmpty()) {
            for (Map.Entry<BEC, BigDecimal> entry : becTotals.entrySet()) {
                BEC bec = entry.getKey();
                BigDecimal total = entry.getValue();
                
                HBox becRow = createSummaryRow(
                    bec.getName(), 
                    formatCurrency(total),
                    "-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-radius: 3;"
                );
                dynamicSummaryVBox.getChildren().add(becRow);
               
            }
        } else {
        	String noDataMessage = "Nta maturo aboneka kuri ino santarali";
            if (selectedYear != null) {
                noDataMessage += " mu mwaka wa " + selectedYear;
            }
            Label noDataLabel = new Label(noDataMessage);
            noDataLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            dynamicSummaryVBox.getChildren().add(noDataLabel);
        }
      } catch (Exception e) {
          loggingService.logError("SUMMARY_DRILLDOWN_ERROR", e);
          e.printStackTrace();
      }
     }
   // method to show SubParish summary:
    	private void showSubParishSummary() {
    	  try {
    		Integer selectedYear = yearFilterComboBox.getValue();
    	    loggingService.logUserAction("SUMMARY_DISPLAY","Displaying sub-parish summary for " + (selectedYear != null ? selectedYear : "all years"));
    	    
    	    // Clear previous content
    	    dynamicSummaryVBox.getChildren().clear();
    	    
    	    // Update title with year information
    	    String titleText = "IGITERANYO CY'ITURO RYA SANTARALI:";
    	    if (selectedYear != null) {
    	        titleText += " (" + selectedYear + ")";
    	    } else {
    	        titleText += " (IMYAKA YOSE)";
    	    }
    	    summaryTitleLabel.setText(titleText);
    	    dynamicSummaryVBox.getChildren().add(summaryTitleLabel);
    	    
    	    // Get totals by SubParish for the selected year
    	    Map<SubParish, BigDecimal> subParishTotals = contributionService.getTotalsBySubParish(selectedYear);
    	    loggingService.logUserAction("SUMMARY_DATA", "Found " + subParishTotals.size() + " sub-parishes with contributions");
    	    if (!subParishTotals.isEmpty()) {
    	        for (Map.Entry<SubParish, BigDecimal> entry : subParishTotals.entrySet()) {
    	            SubParish subParish = entry.getKey();
    	            BigDecimal total = entry.getValue();
    	            
    	            HBox subParishRow = createSummaryRow(
    	                subParish.getName(), 
    	                formatCurrency(total),
    	                "-fx-background-color: #e9ecef; -fx-padding: 10; -fx-border-radius: 3;"
    	            );
    	            
    	            dynamicSummaryVBox.getChildren().add(subParishRow);
    	        }
    	    } else {
    	        String noDataMessage = "Nta maturo aboneka";
    	        if (selectedYear != null) {
    	            noDataMessage += " mu mwaka wa " + selectedYear;
    	        }
    	        Label noDataLabel = new Label(noDataMessage);
    	        noDataLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
    	        dynamicSummaryVBox.getChildren().add(noDataLabel);
    	    } 
    	    
    	  }catch(Exception e) {
    	    	loggingService.logError("SUMMARY_DISPLAY_ERROR", e);
    	    	e.printStackTrace();
    	    }
    	}

   private void loadSummaryForYear(Integer year) {
        // Clear previous content
    	try {subParishFilterComboBox.setValue(null);
    	loggingService.logUserAction("SUMMARY_LOAD", "Loading summary data for year: " + (year != null ? year : "All years"));
        
        // Update title based on filter
        if (year != null) {
            overallTotalTitleLabel.setText("ITURO RYA PARUWASE YOSE UMWAKA WA " + year +" NI ");
        } else {
            overallTotalTitleLabel.setText("ITURO RYOSE RYA PARUWASE KUGEZA UBU NI ");
        }
        
        // Get overall total
        BigDecimal overallTotal = contributionService.getTotalContributions(year);
        loggingService.logUserAction("SUMMARY_TOTAL", "Calculated overall total: " + formatCurrency(overallTotal));
        overallTotalAmountLabel.setText(formatCurrency(overallTotal));
        
        showSubParishSummary();
         } catch(Exception ex ) {
        	 loggingService.logError("SUMMARY_LOAD_ERROR", ex);
        	 ex.printStackTrace();         }
        }
    private void refreshCurrentView() {
        // 1. Update overall total for new year
        Integer selectedYear = yearFilterComboBox.getValue();
        BigDecimal overallTotal = contributionService.getTotalContributions(selectedYear);
        overallTotalAmountLabel.setText(formatCurrency(overallTotal));
        
        // 2. Update overall title
        if (selectedYear != null) {
            overallTotalTitleLabel.setText("ITURO RYA PARUWASE YOSE MU MWAKA WA " + selectedYear + " NI");
        } else {
            overallTotalTitleLabel.setText("ITURO RYOSE RYA PARUWASI KUGEZA UBU NI ");
        }
        
        // 3. Smart refresh based on current state
        SubParish selectedSubParish = subParishFilterComboBox.getValue();
        if (selectedSubParish != null) {
            // User is viewing BEC details - refresh BEC view with new year
            showBecSummaryForSubParish(selectedSubParish);
        } else {
            // User is viewing SubParish summary - refresh SubParish view with new year
            showSubParishSummary();
        }
    }

}