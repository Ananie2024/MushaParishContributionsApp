package org.ananie.mushaParish.services;

import org.ananie.mushaParish.utilities.ViewPaths;
import org.ananie.parishApp.model.Contribution;
import org.ananie.parishApp.model.Faithful;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FaithfulPDFService {

    // Define fonts with support for unicode characters (for Kinyarwanda)
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
    private static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    
    // Colors
    private static final BaseColor HEADER_COLOR = new BaseColor(52, 58, 64); // Dark gray
    private static final BaseColor BACKGROUND_COLOR = new BaseColor(150,255,165); // light green

    /**
     * Generate PDF for a faithful member's details and contributions
     */
    public void generateFaithfulDetailsPDF(Faithful faithful, List<Contribution> contributions, 
                                         String filePath) throws DocumentException, IOException {
        
        Document document = new Document(PageSize.A4, 40, 40, 60, 80);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        
        // Add header and footer event handler
        FaithfulPDFPageEvent pageEvent = new FaithfulPDFPageEvent(ViewPaths.LOGO);
        writer.setPageEvent(pageEvent);
        
        document.open();
        
        // Add document title
        addDocumentTitle(document);
        
        // Add faithful details section
        addFaithfulDetailsSection(document, faithful);
        
        // Add contributions section
        addContributionsSection(document, contributions);
        
        // Add summary section
        addContributionsSummary(document, contributions);
        // add credentials at the end        
        addCredentials(document);
        
        document.close();
    }

    private void addDocumentTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("IFISHI Y'AMATURO Y'UMUKRISTU", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Add generation date
        String dateText = "Byakozwe ku wa: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph dateP = new Paragraph(dateText, VALUE_FONT);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        dateP.setSpacingAfter(15);
        document.add(dateP);
    }

    private void addFaithfulDetailsSection(Document document, Faithful faithful) throws DocumentException {
        // Section title
        Paragraph sectionTitle = new Paragraph("UMWIRONDORO ", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(5);
        document.add(sectionTitle);
        
        // Create details table
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{30f, 70f});
        detailsTable.setSpacingAfter(20);
        
        
        // Add details rows
        addDetailRow(detailsTable, "Amazina:", faithful.getName());
        addDetailRow(detailsTable, "Telefone:", faithful.getContactNumber() != null ? faithful.getContactNumber() : "Ntayatanzwe");
        addDetailRow(detailsTable, "Aderesi:", faithful.getAddress() != null ? faithful.getAddress() : "Ntayatanzwe");
        addDetailRow(detailsTable, "Impuza:", faithful.getBec().getName());
        addDetailRow(detailsTable, "Santarali:", 
                      faithful.getBec().getSubParish().getName());
        addDetailRow(detailsTable, "Umwaka yabatirijwemo:", 
                    faithful.getBaptismYear() != null ? faithful.getBaptismYear() : "Ntawo");
        addDetailRow(detailsTable, "Icyo akora:", 
                    faithful.getOccupation() != null ? faithful.getOccupation() : "Ntacyatanzwe");
        
        document.add(detailsTable);
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        // Label cell
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8);       
        table.addCell(labelCell);
        
        // Value cell
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);       
        table.addCell(valueCell);
    }

    private void addContributionsSection(Document document, List<Contribution> contributions) throws DocumentException {
        // Section title
        Paragraph sectionTitle = new Paragraph("AMATURO ", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(5);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);
        
        if (contributions == null || contributions.isEmpty()) {
            Paragraph noData = new Paragraph("Nta maturo yagaragara kuri uyu mukristu.", VALUE_FONT);
            noData.setSpacingAfter(20);
            document.add(noData);
            return;
        }
        
        // Create contributions table
        PdfPTable contributionsTable = new PdfPTable(4);
        contributionsTable.setWidthPercentage(100);
        contributionsTable.setWidths(new float[]{15f, 25f, 25f, 35f});
        contributionsTable.setSpacingAfter(20);
        
        // Add headers
        addContributionHeader(contributionsTable, "Inshuro ");
        addContributionHeader(contributionsTable, "Umwaka");
        addContributionHeader(contributionsTable, "Ituro (RWF)");
        addContributionHeader(contributionsTable, "Itariki");
        
        // Add data rows
        for (int i = 0; i < contributions.size(); i++) {
            Contribution contribution = contributions.get(i);
            
            
            addContributionDataCell(contributionsTable, String.valueOf(i + 1));
            addContributionDataCell(contributionsTable, contribution.getYear().toString());
            addContributionDataCell(contributionsTable, formatAmount(contribution.getAmount()));
            addContributionDataCell(contributionsTable, formatDate(contribution.getDate()));
        }
        
        document.add(contributionsTable);
    }

    private void addContributionHeader(PdfPTable table, String text) {
        PdfPCell headerCell = new PdfPCell(new Phrase(text, HEADER_FONT));
        headerCell.setBackgroundColor(HEADER_COLOR);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(10);
        table.addCell(headerCell);
    }

    private void addContributionDataCell(PdfPTable table, String text) {
        PdfPCell dataCell = new PdfPCell(new Phrase(text, TABLE_FONT));
        dataCell.setPadding(8);
        dataCell.setHorizontalAlignment(Element.ALIGN_CENTER);              
        table.addCell(dataCell);
    }

    private void addContributionsSummary(Document document, List<Contribution> contributions) throws DocumentException {
        if (contributions == null || contributions.isEmpty()) {
            return;
        }
        
        // Calculate summary
        BigDecimal totalAmount = contributions.stream()
                .map(Contribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        //create a paragraph with the label and total amount on the same line
        Paragraph summaryParagraph = new Paragraph();
        summaryParagraph.setSpacingBefore(10);
        summaryParagraph.setSpacingAfter(10);
        
        // Add the label
        Chunk labelChunk = new Chunk("AMAFRANGA YOSE YATUYE NI : ", LABEL_FONT);
        summaryParagraph.add(labelChunk);
        
        // Add the total amount
        Chunk amountChunk = new Chunk(formatAmount(totalAmount) + " RWF", VALUE_FONT);
        //amountChunk.setBackground(new BaseColor(255, 255, 0), 2, 2, 2, 2); // Yellow highlight (optional)
        summaryParagraph.add(amountChunk);
        
        document.add(summaryParagraph);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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
     * Helper method to load logo as resource or file
     */
    private static Image loadLogo(String logoPath) throws IOException, DocumentException {
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return null;
        }

        Image logo = null;
        
        try {
            // First try to load as resource
            if (logoPath.startsWith("/")) {
                // Try as resource from classpath
                java.net.URL logoUrl = FaithfulPDFService.class.getResource(logoPath);
                if (logoUrl != null) {
                    logo = Image.getInstance(logoUrl);
                    return logo;
                }
            }
            
            // If resource loading failed, try as file
            File logoFile = new File(logoPath);
            if (logoFile.exists() && logoFile.canRead()) {
                logo = Image.getInstance(logoPath);
                return logo;
            }
            
            // Try common resource paths
            String[] resourcePaths = {
                "/" + logoPath.replaceFirst("^/", ""),
                "/images/" + logoPath.replaceFirst("^.*/", ""),
                "/assets/" + logoPath.replaceFirst("^.*/", "")
            };
            
            for (String resourcePath : resourcePaths) {
                java.net.URL logoUrl = FaithfulPDFService.class.getResource(resourcePath);
                if (logoUrl != null) {
                    logo = Image.getInstance(logoUrl);
                    return logo;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Could not load logo from path: " + logoPath);
            e.printStackTrace();
        }
        
        return null;
    }

    // Inner class for page events (header and footer)
    public static class FaithfulPDFPageEvent extends PdfPageEventHelper {
        private final String logoPath;
        
        public FaithfulPDFPageEvent(String logoPath) {
            this.logoPath = logoPath;
        }
        
        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            try {
            	 addBackgroundColor(writer, document);
                 addHeader(writer, document);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                addFooter(writer, document);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
     // for background color
        private void addBackgroundColor(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            Rectangle rect = new Rectangle(document.getPageSize());
            rect.setBackgroundColor(BACKGROUND_COLOR);
            canvas.rectangle(rect);
            canvas.fill();
        }
        private void addHeader(PdfWriter writer, Document document) throws DocumentException {
            PdfContentByte cb = writer.getDirectContent();
            
            // Add logo if path is provided
            if (logoPath != null && !logoPath.trim().isEmpty()) {
                try {
                    Image logo = loadLogo(logoPath);
                    if (logo != null) {
                        logo.scaleToFit(60, 60);
                        logo.setAbsolutePosition(document.leftMargin(), 
                                               document.top() + document.topMargin() - 70);
                        document.add(logo);
                    } else {
                        // If logo can't be loaded, add placeholder text
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("LOGO", FontFactory.getFont(FontFactory.HELVETICA, 10)),
                                document.leftMargin(), document.top() + document.topMargin() - 30, 0);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading logo in header: " + e.getMessage());
                    // Add placeholder text
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase("LOGO", FontFactory.getFont(FontFactory.HELVETICA, 10)),
                            document.leftMargin(), document.top() + document.topMargin() - 30, 0);
                }
            }
            
            // Add header text
            Phrase headerText = new Phrase("ARKIDIYOSEZI YA KIGALI-PARUWASI MUSHA", 
                                         FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    headerText,
                    (document.right() + document.left()) / 2,
                    document.top() + document.topMargin() - 30, 0);
        }
        
        private void addFooter(PdfWriter writer, Document document) throws DocumentException, IOException {
            PdfContentByte cb = writer.getDirectContent();
            
            // Page number
            Phrase pageNum = new Phrase("Urupapuro " + writer.getPageNumber(), 
                                      FontFactory.getFont(FontFactory.HELVETICA, 9));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    pageNum,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 10, 0);                    
            
        }
    }
}