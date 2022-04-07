package be.speos.library.pdfvalidator.enums;

/**
 * Single - overlay is only applied to one page
 * All - overlay is applied to all pages
 * Range - overlay is applied to a page range.
 * Custom - overlay is applied to selected pages
 */
public enum PageType {
    SINGLE("single"),
    RANGE("range"),
    CUSTOM("custom"),
    ALL("all");

    private final String pageTypeName;

    PageType(String pageTypeName) {
        this.pageTypeName = pageTypeName;
    }

    public String getPageTypeName() {
        return pageTypeName;
    }
}
