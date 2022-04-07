package be.speos.library.pdfvalidator.util;

import be.speos.library.pdfvalidator.dto.OverlayDetail;
import be.speos.library.pdfvalidator.enums.OverlayListType;
import be.speos.library.pdfvalidator.dto.PageSelectionDto;
import be.speos.library.pdfvalidator.enums.PageType;
import be.speos.library.pdfvalidator.exception.PDFValidatorPasswordException;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.crypto.BadPasswordException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FileManipulator {

    private static final Logger log = LoggerFactory.getLogger(FileManipulator.class);
    private DimensionChecker pdfDimensionChecker;
    private String overlayPath;
    private static FileManipulator fileManipulator;

    public static FileManipulator getInstance(DimensionChecker dimensionChecker, String overlayPath) {

        if (fileManipulator == null) {
            return new FileManipulator(dimensionChecker, overlayPath);
        }
        return fileManipulator;
    }

    private FileManipulator(DimensionChecker dimensionChecker, String overlayPath) {
        this.pdfDimensionChecker = dimensionChecker;
        this.overlayPath = overlayPath;
    }

    public byte[] manipulateFile(Path originalPath, Path destinationPath, Map<OverlayListType, OverlayDetail> overlayDetailMap) throws PDFValidatorPasswordException {
        byte[] bytes = new byte[0];
        try {

            bytes = applyPdfOverlay(originalPath, destinationPath, overlayDetailMap);

        } catch (BadPasswordException ex) {
            throw new PDFValidatorPasswordException(ex);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return bytes;
    }


    public byte[] manipulateFileWithOverlappingValidation(Path filePath, String sessionPath, String fileName) {
        byte[] bytes = new byte[0];

        try (PdfReader reader = new PdfReader(filePath.toString())) {
            PdfDocument document = new PdfDocument(reader);
            PdfDocumentContentParser contentParser = new PdfDocumentContentParser(document);
            Map<Integer, Set<Rectangle>> rectangleMap = new HashMap<>();
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                OverlappingTextSearchingStrategy strategy = contentParser.processContent(i, new OverlappingTextSearchingStrategy());
                Set<Rectangle> overlappingRectangle = strategy.foundOverlappingText();
                rectangleMap.put(i, overlappingRectangle);
            }
            bytes = applyOverlapping(sessionPath, fileName, rectangleMap);
        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                bytes = Files.readAllBytes(filePath);
            } catch (IOException em) {
                log.error(em.getMessage());
            }
        }
        return bytes;
    }

    private byte[] applyOverlapping(String sessionPath, String fileName, Map<Integer, Set<Rectangle>> recMap) throws IOException {
        File directory = createNewOverlappingDirectory(sessionPath);
        Path sourcePath = Paths.get(sessionPath, "overlay", fileName);
        Path destinationPath = Paths.get(directory.getAbsolutePath(), fileName);

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(sourcePath.toString()), new PdfWriter(destinationPath.toString()))) {
            Document document = new Document(pdfDoc);
            for (Map.Entry<Integer, Set<Rectangle>> entry : recMap.entrySet()) {
                PdfCanvas pdfCanvas = new PdfCanvas(pdfDoc.getPage(entry.getKey()));
                for (Rectangle rectangle : entry.getValue()) {
                    pdfCanvas.rectangle(rectangle);
                    pdfCanvas.setFillColor(DeviceRgb.GREEN);
                    pdfCanvas.setStrokeColor(DeviceRgb.GREEN);

                    Paragraph paragraph = new Paragraph("!");
                    paragraph.setFontColor(DeviceRgb.BLUE);
                    paragraph.setFontSize(20);
                    paragraph.setFixedPosition(entry.getKey(), rectangle.getX(), rectangle.getY(), 20);
                    document.add(paragraph);
                }
                pdfCanvas.stroke();
            }
            document.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return Files.readAllBytes(destinationPath);
    }


    private byte[] applyPdfOverlay(Path originalPath, Path destinationPath, Map<OverlayListType, OverlayDetail> overlayDetailMap) throws IOException, BadPasswordException {

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(originalPath.toAbsolutePath().toString()), new PdfWriter(destinationPath.toAbsolutePath().toString()))) {

            //first page overlay apply
            applyFirstPageOverlay(overlayDetailMap.get(OverlayListType.FIRST_PAGE).getOverlays(), pdfDoc);

            if (overlayDetailMap.containsKey(OverlayListType.PERFORATION_LINE) && overlayDetailMap.get(OverlayListType.PERFORATION_LINE).getAddCustomOverlay()) {
                applyCustomOverlayList(overlayDetailMap.get(OverlayListType.PERFORATION_LINE).getOverlays(), overlayDetailMap.get(OverlayListType.PERFORATION_LINE).getPageInfo(), pdfDoc);
            }

            if (overlayDetailMap.containsKey(OverlayListType.PAYMENT) && overlayDetailMap.get(OverlayListType.PAYMENT).getAddCustomOverlay()) {
                applyCustomOverlayList(overlayDetailMap.get(OverlayListType.PAYMENT).getOverlays(), overlayDetailMap.get(OverlayListType.PAYMENT).getPageInfo(), pdfDoc);
            }
            //all pages after the first page overlay apply
            applyAllPagesOverlay(overlayDetailMap.get(OverlayListType.ALL_PAGE).getOverlays(), pdfDoc);
        }


        return Files.readAllBytes(destinationPath);
    }


    private static File createNewOverlappingDirectory(String sessionPath) {
        Path userOverlappingPath = Paths.get(sessionPath, "overlapping");
        File directory = new File(userOverlappingPath.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }
        return directory;
    }

    private void applyFirstPageOverlay(List<String> firstPageOverlayList, PdfDocument pdfDoc) throws IOException {
        Rectangle rectangle = pdfDoc.getFirstPage().getPageSizeWithRotation();
        PdfCanvas canvas;
        if (pdfDimensionChecker.checkWidth(rectangle.getWidth()) && pdfDimensionChecker.checkHeight(rectangle.getHeight())) {
            canvas = new PdfCanvas(pdfDoc.getFirstPage().setIgnorePageRotationForContent(true));
        } else {
            canvas = new PdfCanvas(pdfDoc.getFirstPage());
        }
        applyOverlayFiles(firstPageOverlayList, canvas, pdfDoc);
    }

    private void applyAllPagesOverlay(List<String> secondPageOverlayList, PdfDocument pdfDoc) throws IOException {
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            Rectangle rectangle = pdfDoc.getPage(i).getPageSizeWithRotation();
            PdfCanvas canvas;
            if (pdfDimensionChecker.checkWidth(rectangle.getWidth()) && pdfDimensionChecker.checkHeight(rectangle.getHeight())) {
                canvas = new PdfCanvas(pdfDoc.getPage(i).setIgnorePageRotationForContent(true));
            } else {
                canvas = new PdfCanvas(pdfDoc.getPage(i));
            }
            applyOverlayFiles(secondPageOverlayList, canvas, pdfDoc);
        }
    }

    private void applyCustomOverlayList(List<String> perforationOverlayList, PageSelectionDto perforationLineData, PdfDocument pdfDoc) throws IOException {
        if (perforationLineData.getPageType().equals(PageType.SINGLE.getPageTypeName())) {
            PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(perforationLineData.getSinglePage()).newContentStreamAfter(),
                    pdfDoc.getPage(perforationLineData.getSinglePage()).getResources(), pdfDoc);
            applyOverlayFiles(perforationOverlayList, canvas, pdfDoc);

        } else if (perforationLineData.getPageType().equals(PageType.RANGE.getPageTypeName())) {
            for (int i = perforationLineData.getRangeFrom(); i <= perforationLineData.getRangeTo(); i++) {
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(i).newContentStreamAfter(), pdfDoc.getPage(i).getResources(), pdfDoc);
                applyOverlayFiles(perforationOverlayList, canvas, pdfDoc);
            }
        } else if (perforationLineData.getPageType().equals(PageType.CUSTOM.getPageTypeName())) {
            String customPages = perforationLineData.getCustomPages();
            List<String> strCustomPageList = new ArrayList<>(Arrays.asList(customPages.split(",")));
            List<Integer> customPageList = strCustomPageList.stream()
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            for (Integer page : customPageList) {
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(page).newContentStreamAfter(), pdfDoc.getPage(page).getResources(), pdfDoc);
                applyOverlayFiles(perforationOverlayList, canvas, pdfDoc);
            }
        }
    }

    private void applyOverlayFiles(List<String> overlayList, PdfCanvas canvas, PdfDocument pdfDoc) throws IOException {
        for (String file : overlayList) {
            Path path = Paths.get(overlayPath, file);
            try (PdfDocument srcDoc = new PdfDocument(new PdfReader(path.toString()))) {
                PdfFormXObject page = srcDoc.getFirstPage().copyAsFormXObject(pdfDoc);
                canvas.addXObject(page, 0, 0);
            }
        }
    }

}
