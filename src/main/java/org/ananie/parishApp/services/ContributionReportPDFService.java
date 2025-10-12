package org.ananie.parishApp.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.ananie.parishApp.model.FaithfulContributionRow;
import org.ananie.parishApp.utilities.ViewPaths;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.scene.control.TableColumn;

@Service
public class ContributionReportPDFService {

    // Define fonts with support for unicode characters (for Kinyarwanda)
	 private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
    private static final Font TABLE_DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
    // Colors
    private static final BaseColor HEADER_COLOR = new BaseColor(52, 58, 64); // Dark gray
    private static final BaseColor ALTERNATE_ROW_COLOR = new BaseColor(248, 249, 250); // Light gray
    
    String logoPath = ViewPaths.LOGO;

    /**
     * Generates a PDF for the contribution report table.
     * @param columns The list of TableColumn objects from the JavaFX TableView.
     * @param rows The list of data objects (FaithfulContributionRow) from the JavaFX TableView.
     * @param reportTitle The title for the report (e.g., "IMBONERAHAMWE Y'AMATURO YA PARUWASE MUSHA").
     * @param filePath The path where the PDF will be saved.
     * @param logoPath The path to the parish logo.
     * @throws DocumentException
     * @throws IOException
     */
    public void generateContributionReportPDF(List<TableColumn<FaithfulContributionRow, ?>> columns,
                                            List<FaithfulContributionRow> rows,
                                            String reportTitle,
                                            String filePath
                                           ) throws DocumentException, IOException {

      try (FileOutputStream fos = new FileOutputStream(filePath)){ Document document = new Document(PageSize.A4, 40, 40, 60, 80); 
        PdfWriter writer = PdfWriter.getInstance(document, fos);

       
		// Add header and footer event handler - reuse your existing one
        FaithfulPDFService.FaithfulPDFPageEvent pageEvent = new FaithfulPDFService.FaithfulPDFPageEvent(logoPath);
        writer.setPageEvent(pageEvent);

        document.open();

        addDocumentTitle(document,reportTitle);
        addContributionTableToPDF(document, columns, rows);
        addCredentials(document);
        document.close();}
    }

    private void addDocumentTitle(Document document, String reportTitle) throws DocumentException {
        Paragraph title = new Paragraph(reportTitle, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        String dateText = "Byakozwe ku wa: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph dateP = new Paragraph(dateText, TABLE_DATA_FONT); // Using TABLE_DATA_FONT for date
        dateP.setAlignment(Element.ALIGN_RIGHT);
        dateP.setSpacingAfter(10);
        document.add(dateP);
    }

    
    private void addContributionTableToPDF(Document document,
                                          List<TableColumn<FaithfulContributionRow, ?>> columns,
                                          List<FaithfulContributionRow> rows) throws DocumentException {

        if (rows == null || rows.isEmpty()) {
            Paragraph noData = new Paragraph("Nta maturo yagaragara kuri iyi raporo.", TABLE_DATA_FONT);
            noData.setSpacingAfter(20);
            document.add(noData);
            return;
        }

        // Create a table with the correct number of columns
        PdfPTable pdfTable = new PdfPTable(columns.size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(10f);
        pdfTable.setSpacingAfter(10f);

        // Calculate column widths dynamically based on content (approximation)
        float[] columnWidths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            // Assign relative widths. Name column wider, total column wider, years narrower.
            String columnText = columns.get(i).getText();
            if ("Abakristu".equals(columnText)) {
                columnWidths[i] = 3.0f; // Wider for names
            } else if ("Total".equals(columnText)) {
                columnWidths[i] = 2.0f; // Wider for total
            } else {
                columnWidths[i] = 1.0f; // Standard for years
            }
        }
        pdfTable.setWidths(columnWidths);

        // Add table headers
        for (TableColumn<FaithfulContributionRow, ?> col : columns) {
            PdfPCell header = new PdfPCell(new Phrase(col.getText(), TABLE_HEADER_FONT));
            header.setBackgroundColor(HEADER_COLOR);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPadding(8);
            pdfTable.addCell(header);
        }

        // Add table data
        for (int i = 0; i < rows.size(); i++) {
            FaithfulContributionRow row = rows.get(i);
            boolean isAlternateRow = i % 2 == 1;

            for (TableColumn<FaithfulContributionRow, ?> col : columns) {
                Object cellValue = getCellValue(row, col);
                String cellText;

                // Apply formatting based on column type, similar to JavaFX TableCell
                if (cellValue instanceof BigDecimal) {
                    BigDecimal amount = (BigDecimal) cellValue;
                    if (amount.compareTo(BigDecimal.ZERO) == 0 && !col.getText().equals("Total")) {
                        cellText = ""; // Empty string for zero contributions in year columns
                    } else {
                        cellText = String.format("%,.0f", amount.setScale(0, RoundingMode.DOWN));
                    }
                } else if (cellValue != null) {
                    cellText = cellValue.toString();
                } else {
                    cellText = "";
                }

                PdfPCell dataCell = new PdfPCell(new Phrase(cellText, TABLE_DATA_FONT));
                dataCell.setPadding(6);
                dataCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                // Align 'Abakristu' column left, numeric columns right
                if ("Abakristu".equals(col.getText())) {
                    dataCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                } else {
                    dataCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }

                if (isAlternateRow) {
                    dataCell.setBackgroundColor(ALTERNATE_ROW_COLOR);
                }

                // Special styling for "Total" column
                if ("Total".equals(col.getText())) {
                    dataCell.setBackgroundColor(new BaseColor(173, 216, 230)); // Skyblue
                    dataCell.setBorderColor(new BaseColor(75, 0, 130)); // Dark violet
                    dataCell.setBorderWidth(0.5f);
                    dataCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    dataCell.setPhrase(new Phrase(cellText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new BaseColor(75, 0, 130)))); // Dark violet bold font
                }
                pdfTable.addCell(dataCell);
            }
        }
        document.add(pdfTable);
    }
    
    private void addCredentials(Document document) throws DocumentException {
    	// add space before them
    	 document.add(Chunk.NEWLINE);
    	 document.add(Chunk.NEWLINE);
    	 
    	 Paragraph signatureParagraph = new Paragraph();
    	 
    	 Chunk signatureLabelChunk = new Chunk("UMUKONO NA KASHE: ", LABEL_FONT);
    	 signatureParagraph.add(signatureLabelChunk);
    	 document.add(signatureParagraph);
    }

    /**
     * Helper to get cell value from FaithfulContributionRow based on TableColumn's cellValueFactory.
     * This is a generic way to extract values from TableView data without relying on specific field names.
     * It mimics how JavaFX gets the value.
     */
    private<T> Object getCellValue(FaithfulContributionRow row, TableColumn<FaithfulContributionRow, T> column) {
        if (column.getCellValueFactory() != null) {
            return column.getCellValueFactory().call(new TableColumn.CellDataFeatures<FaithfulContributionRow, T>(null, column, row)).getValue();
        }
        return null;
    }
}