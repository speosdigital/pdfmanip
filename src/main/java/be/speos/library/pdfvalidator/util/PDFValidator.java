package be.speos.library.pdfvalidator.util;

import com.itextpdf.kernel.crypto.BadPasswordException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PDFValidator {

    private static final Logger log = LoggerFactory.getLogger(PDFValidator.class);
    private static final double ALLOWED_PAGE_WEIGHT = 0.3;

    private PDFValidator(){}

    public static boolean validatePassword(String path, String filename) throws BadPasswordException, IOException {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(path + "/" + filename))) {
            return true;
        }
    }

    public static boolean validateSameSize(String path, String filename, DimensionChecker dimensionChecker) {
        boolean differentSizeFound = false;
        List<String> errorPageList = new ArrayList<>();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(path + "/" + filename))) {

            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                Rectangle rectangle = pdfDoc.getPage(i).getPageSizeWithRotation();
                if (!dimensionChecker.checkWidth(rectangle.getWidth()) || !dimensionChecker.checkHeight(rectangle.getHeight())) {
                    differentSizeFound = true;
                    errorPageList.add(String.valueOf(i));
                }
            }
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }
        return differentSizeFound;
    }

    public static boolean validateEmbeddedFonts(String path, String filename) {
        boolean isEmbedded = true;
        List<String> unembeddedFontList = new ArrayList<>();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(path + "/" + filename))) {
            for (int i = 1; i < pdfDoc.getNumberOfPdfObjects(); i++) {
                PdfObject obj = pdfDoc.getPdfObject(i);
                if (obj != null && obj.isDictionary()) {
                    PdfDictionary pdfDictionary = (PdfDictionary) obj;
                    if (PdfName.Font.equals(pdfDictionary.getAsName(PdfName.Type))) {
                        for (PdfName pdfName : pdfDictionary.keySet()) {
                            checkFont(pdfName, pdfDictionary, unembeddedFontList);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }

        if (!unembeddedFontList.isEmpty()) {
            return false;
        }
        return isEmbedded;
    }


    public static boolean validatePageWeight(String path, String filename) {
        Path filePath = Paths.get(path, filename);

        boolean isValidPageWeight = true;
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(filePath.toString()))) {
            int pageCount = pdfDoc.getNumberOfPages();
            double fileSize = Files.size(filePath) / 1024.0 / 1024.0; //Size in megabytes
            double averageWeight = fileSize / pageCount;
            if (averageWeight > ALLOWED_PAGE_WEIGHT) {
                return false;
            }

        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }
        return isValidPageWeight;
    }

    public static boolean validateOverlapping(String path, String filename) throws FileNotFoundException, IllegalStateException {
        int overlappingCount = 0;

        Path filePath = Paths.get(path, "overlay", filename);
        if (!filePath.toFile().exists()) {
            throw new FileNotFoundException("Could not create the file with manipulations");
        }
        try (PdfReader reader = new PdfReader(filePath.toString())) {
            PdfDocument document = new PdfDocument(reader);
            PdfDocumentContentParser contentParser = new PdfDocumentContentParser(document);
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                OverlappingTextSearchingStrategy strategy = contentParser.processContent(i, new OverlappingTextSearchingStrategy());
                Set<Rectangle> overlappingRectangle = strategy.foundOverlappingText();
                if (!overlappingRectangle.isEmpty()) {
                    overlappingCount = overlappingCount + overlappingRectangle.size();
                }
            }
            document.close();

        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }
        return overlappingCount > 0;
    }


    private static void checkFont(PdfName pdfName, PdfDictionary pdfDictionary, List<String> unembeddedFontList) {
        if (pdfName.equals(PdfName.BaseFont)) {
            String name = pdfDictionary.get(pdfName).toString();
            if (!isEmbeddedSubset(name)) {
                unembeddedFontList.add(name);
            }
        }
    }

    private static boolean isEmbeddedSubset(String name) {
        return name != null && name.length() > 8 && name.charAt(7) == '+';
    }
}
