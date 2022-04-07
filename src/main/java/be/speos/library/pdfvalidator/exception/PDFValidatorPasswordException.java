package be.speos.library.pdfvalidator.exception;

import com.itextpdf.kernel.crypto.BadPasswordException;

public class PDFValidatorPasswordException extends RuntimeException {
    public PDFValidatorPasswordException(BadPasswordException ex) {
        super(ex);
    }
}
