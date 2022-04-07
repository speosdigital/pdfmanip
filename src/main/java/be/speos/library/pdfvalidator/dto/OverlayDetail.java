package be.speos.library.pdfvalidator.dto;

import java.util.List;

public class OverlayDetail {

    private List<String> overlays;
    private boolean addCustomOverlay;
    private PageSelectionDto pageInfo;

    public OverlayDetail(List<String> overlays, boolean addCustomOverlay) {
        this.overlays = overlays;
        this.addCustomOverlay = addCustomOverlay;
    }

    public OverlayDetail(List<String> overlays, boolean addCustomOverlay, PageSelectionDto pageInfo) {
        this.overlays = overlays;
        this.addCustomOverlay = addCustomOverlay;
        this.pageInfo = pageInfo;
    }

    public List<String> getOverlays() {
        return overlays;
    }

    public boolean getAddCustomOverlay() {
        return addCustomOverlay;
    }

    public PageSelectionDto getPageInfo() {
        return pageInfo;
    }
}
