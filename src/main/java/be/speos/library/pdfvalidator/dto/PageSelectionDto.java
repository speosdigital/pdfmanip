package be.speos.library.pdfvalidator.dto;

public class PageSelectionDto {
    private String optionName;
    private String pageType;
    private Integer singlePage;
    private Integer rangeFrom;
    private Integer rangeTo;
    private String customPages;

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public Integer getSinglePage() {
        return singlePage;
    }

    public void setSinglePage(Integer singlePage) {
        this.singlePage = singlePage;
    }

    public Integer getRangeFrom() {
        return rangeFrom;
    }

    public void setRangeFrom(Integer rangeFrom) {
        this.rangeFrom = rangeFrom;
    }

    public Integer getRangeTo() {
        return rangeTo;
    }

    public void setRangeTo(Integer rangeTo) {
        this.rangeTo = rangeTo;
    }

    public String getCustomPages() {
        return customPages;
    }

    public void setCustomPages(String customPages) {
        this.customPages = customPages;
    }
}
