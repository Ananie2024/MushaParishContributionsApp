package org.ananie.mushaParish.controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import org.ananie.mushaParish.model.BEC;
import org.ananie.mushaParish.model.FaithfulContributionRow;
import org.ananie.mushaParish.model.SubParish;
import org.ananie.mushaParish.services.BECService;
import org.ananie.mushaParish.services.ContributionService;
import org.ananie.mushaParish.services.LoggingService;
import org.ananie.mushaParish.services.SubParishService;
import org.ananie.mushaParish.utilities.ContributionReportPDFUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class ContributionReportController implements Initializable {
    private final LoggingService loggingService;
    private final ContributionService contributionService;
    private final SubParishService subParishService;
    private final BECService becService;
    private final ContributionReportPDFUtility contributionReportPDFUtility;

    @Autowired
    public ContributionReportController(ContributionService contributionService,
                                      SubParishService subParishService,
                                      BECService becService, 
                                      ContributionReportPDFUtility contributionReportPDFUtility,
                                      LoggingService loggingService) {
        this.contributionService = contributionService;
        this.subParishService = subParishService;
        this.becService = becService;
        this.contributionReportPDFUtility = contributionReportPDFUtility;
        this.loggingService = loggingService;
        loggingService.logUserAction("Controller Initialization", "ContributionReportController created");
    }

    @FXML private ComboBox<SubParish> subParishComboBox;
    @FXML private ComboBox<BEC> becComboBox;
    @FXML private TableView<FaithfulContributionRow> contributionTable;
    @FXML private Button clearFilterButton;
    @FXML private Button pdfButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loggingService.logUserAction("UI Initialization", "Contribution report UI initializing");
        initializeData();
    }

    private void initializeData() {
        try {
            becComboBox.setDisable(true);
            subParishComboBox.setItems(FXCollections.observableArrayList(subParishService.findAllOrderedByName()));
            subParishComboBox.setConverter(new StringConverter<SubParish>() {
                @Override
                public String toString(SubParish object) {
                    return object == null ? null : object.getName();
                }

                @Override
                public SubParish fromString(String string) {
                    return null;
                }
            });

            subParishComboBox.setOnAction(e -> {
                SubParish selected = subParishComboBox.getValue();
                if (selected != null) {
                    loggingService.logUserAction("SubParish Selection", "Selected sub-parish: " + selected.getName());
                    becComboBox.setDisable(false);
                    becComboBox.setItems(FXCollections.observableArrayList(becService.findBySubParish(selected)));
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
                    becComboBox.getSelectionModel().clearSelection();
                } else {
                    becComboBox.setDisable(true);
                    becComboBox.getItems().clear();
                }
                refreshTable();
            });

            becComboBox.setOnAction(e -> {
                if (becComboBox.getValue() != null) {
                    loggingService.logUserAction("BEC Selection", "Selected BEC: " + becComboBox.getValue().getName());
                }
                refreshTable();
            });

            clearFilterButton.setOnAction(ev -> clearFilters());
            pdfButton.setOnAction(ev -> generatePdf());
            refreshTable();
            
            loggingService.logUserAction("Data Initialization", "Contribution report data initialized successfully");
        } catch (Exception e) {
            loggingService.logError("initializeData", e);
            e.printStackTrace();
        }
    }

    private void generatePdf() {
        try {
            SubParish selectedSubParish = subParishComboBox.getValue();
            BEC selectedBEC = becComboBox.getValue();

            String reportGroupText;
            if (selectedBEC != null) {
                reportGroupText = selectedBEC.getName();
            } else if (selectedSubParish != null) {
                reportGroupText = selectedSubParish.getName();
            } else {
                reportGroupText = "Paruwase MUSHA";
            }

            String reportTitle = "Imbonerahamwe y'amaturo ya: " + reportGroupText;
            
            long startTime = System.currentTimeMillis();
            contributionReportPDFUtility.exportReportToPDF(contributionTable, pdfButton.getScene().getWindow(), reportTitle);
            long duration = System.currentTimeMillis() - startTime;
            
            loggingService.logPDFGeneration("Contribution Report", reportTitle, true);
            loggingService.logPerformance("PDF Generation", duration);
            loggingService.logUserAction("PDF Generation", "Generated PDF report for: " + reportGroupText);
        } catch (Exception e) {
            loggingService.logError("generatePdf", e);
            loggingService.logPDFGeneration("Contribution Report", "Failed generation", false);
            e.printStackTrace();
            }
    }

    private void clearFilters() {
        loggingService.logUserAction("Filter Clear", "Cleared all filters");
        subParishComboBox.getSelectionModel().clearSelection();
        becComboBox.getSelectionModel().clearSelection();
        refreshTable();
    }

    private void refreshTable() {
        try {
            contributionTable.getColumns().clear();

            SubParish selectedSubParish = subParishComboBox.getValue();
            BEC selectedBEC = becComboBox.getValue();

            List<Integer> years = contributionService.getAvailableYears();
            List<FaithfulContributionRow> rows = contributionService.getFilteredContributionMatrix(selectedSubParish, selectedBEC);

            contributionTable.setItems(FXCollections.observableArrayList(rows));

            TableColumn<FaithfulContributionRow, String> nameCol = new TableColumn<>("Abakristu");
            nameCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getName()));
            contributionTable.getColumns().add(nameCol);

            for (Integer year : years) {
                TableColumn<FaithfulContributionRow, BigDecimal> yearCol = new TableColumn<>(String.valueOf(year));
                
                yearCol.setCellValueFactory(cell -> 
                    new SimpleObjectProperty<>(cell.getValue().getYearlyContributions().getOrDefault(year, new BigDecimal("0000.00")))
                );

                yearCol.setCellFactory(column -> new TableCell<>() {
                    @Override
                    protected void updateItem(BigDecimal item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                            setStyle("");
                        } else {
                            setText(String.format("%,.2f", item));
                            if (item.compareTo(BigDecimal.ZERO) == 0) {
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            } else {
                                setText(item.setScale(0,RoundingMode.DOWN).toPlainString());
                                setStyle("");
                            }
                        }
                    }
                });

                contributionTable.getColumns().add(yearCol);
                contributionTable.setPlaceholder(new Label("Nta bakristu bagaragara"));
            }

            // Add totals column at the end
            TableColumn<FaithfulContributionRow, BigDecimal> totalCol = new TableColumn<>("Total");
            totalCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalContribution()));
            totalCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : String.format("%,.2f", item));
                }
            });
            totalCol.setPrefWidth(100);
            totalCol.setStyle(" -fx-background-color: skyblue; -fx-font-weight: bold; -fx-text-fill: darkviolet; -fx-alignment: CENTER-RIGHT;  -fx-border-color: darkviolet;" );
            contributionTable.getColumns().add(totalCol);
            
            loggingService.logUserAction("Table Refresh", "Refreshed contribution table with " + rows.size() + " records");
        } catch (Exception e) {
            loggingService.logError("refreshTable", e);
            e.printStackTrace();
        }
    }
}