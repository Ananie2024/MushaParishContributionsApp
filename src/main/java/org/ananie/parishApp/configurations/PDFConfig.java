package org.ananie.parishApp.configurations;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor; // Good practice for beans
import lombok.AllArgsConstructor; // Good practice for beans

@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
public class PDFConfig {

    private String logoPath;
    private String parishName;
    private String defaultOutputDirectory;
    private boolean autoOpenAfterGeneration;
    private String fontFamily; 

    // Church/Parish specific information
    private String churchAddress;
    private String churchPhone;
    private String churchEmail;

    // PDF Layout settings
    private float pageMarginTop;
    private float pageMarginBottom;
    private float pageMarginLeft;
    private float pageMarginRight;

    // Font sizes
    private int titleFontSize;
    private int subtitleFontSize;
    private int headerFontSize;
    private int labelFontSize;
    private int valueFontSize;
    private int tableFontSize;

    // Colors (now directly as iText BaseColor)
    private BaseColor headerColor;
    private BaseColor alternateRowColor;
    private BaseColor titleColor;

    
   public Font getTitleFont() {
         return FontFactory.getFont(fontFamily, titleFontSize, Font.BOLD, titleColor);
     }
}