package be.speos.library.pdfvalidator.util;

public class DimensionChecker {

    private float a4Width;
    private float a4Height;
    private float a4Tolerance;

    public DimensionChecker(float a4Width, float a4Height, float a4Tolerance) {
        this.a4Width = a4Width;
        this.a4Height = a4Height;
        this.a4Tolerance = a4Tolerance;
    }

    public boolean checkWidth(float actualWidth){
        return a4Width + a4Tolerance > actualWidth && a4Width - a4Tolerance < actualWidth;
    }

    public boolean checkHeight(float actualHeight){
        return a4Height + a4Tolerance > actualHeight && a4Height - a4Tolerance < actualHeight;
    }
}
