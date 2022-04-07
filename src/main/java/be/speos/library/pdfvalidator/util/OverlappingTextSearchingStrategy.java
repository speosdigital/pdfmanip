package be.speos.library.pdfvalidator.util;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.geom.*;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.util.*;

public class OverlappingTextSearchingStrategy implements IEventListener {
    private static final List<Vector> UNIT_SQUARE_CORNERS = Arrays.asList(new Vector(0, 0, 1), new Vector(1, 0, 1), new Vector(1, 1, 1), new Vector(0, 1, 1));

    private Set<Rectangle> imageRectangles = new HashSet<>();
    private Set<Rectangle> textRectangles = new HashSet<>();
    private Set<Rectangle> pathRectangles = new HashSet<>();

    private Set<Rectangle> interceptedRecs = new HashSet<>();

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (data instanceof ImageRenderInfo) {
            ImageRenderInfo imageData = (ImageRenderInfo) data;
            Matrix ctm = imageData.getImageCtm();
            List<Rectangle> cornerRectangles = new ArrayList<>(UNIT_SQUARE_CORNERS.size());
            for (Vector unitCorner : UNIT_SQUARE_CORNERS) {
                Vector corner = unitCorner.cross(ctm);
                cornerRectangles.add(new Rectangle(corner.get(Vector.I1), corner.get(Vector.I2), 0, 0));
            }
            Rectangle boundingBox = Rectangle.getCommonRectangle(cornerRectangles.toArray(new Rectangle[cornerRectangles.size()]));
            imageRectangles.add(boundingBox);
        } else if (data instanceof TextRenderInfo) {
            TextRenderInfo textData = (TextRenderInfo) data;
            Rectangle ascentRectangle = textData.getAscentLine().getBoundingRectangle();
            Rectangle descentRectangle = textData.getDescentLine().getBoundingRectangle();
            Rectangle boundingBox = Rectangle.getCommonRectangle(ascentRectangle, descentRectangle);
            if ((boundingBox.getHeight() != 0) && (boundingBox.getWidth() != 0)) {
                textRectangles.add(boundingBox.applyMargins(0.1f, 0.1f, 0.1f, 0.1f, false));
            }
        } else if (data instanceof PathRenderInfo) {
            PathRenderInfo pathData = (PathRenderInfo) data;
            pathData.preserveGraphicsState();
            Path path = pathData.getPath();
            for (Subpath sPath : path.getSubpaths()) {
                List<Line> rectLines = new ArrayList<>();
                for (IShape segment : sPath.getSegments()) {
                    if (segment instanceof Line) {
                        Line line = (Line) segment;
                        Rectangle rectangle = new Rectangle((float) line.getBasePoints().get(0).x - 2.5f, (float) line.getBasePoints().get(0).y - 2.5f, (float) (line.getBasePoints().get(1).x - line.getBasePoints().get(0).x), 5);

                        if (pathData.getStrokeColor().getColorValue().length == 3) {
                            DeviceRgb deviceRgb = new DeviceRgb(pathData.getStrokeColor().getColorValue()[0], pathData.getStrokeColor().getColorValue()[1], pathData.getStrokeColor().getColorValue()[2]);
                            if (deviceRgb.equals(DeviceRgb.RED)) {
                                pathRectangles.add(rectangle.applyMargins(0.1f, 0.1f, 0.1f, 0.1f, false));
                                rectLines.add(line);
                            }
                        }
                    }
                }
                if(rectLines.size() == 3){
                    Line firstLine = rectLines.get(0);
                    Line lastLine = rectLines.get(2);

                    Line line = new Line((float)firstLine.getBasePoints().get(0).x,
                            (float)firstLine.getBasePoints().get(0).y, (float)lastLine.getBasePoints().get(1).x, (float)lastLine.getBasePoints().get(1).y);

                    Rectangle rectangle = new Rectangle(
                            (float) line.getBasePoints().get(0).x - 2.5f,
                            (float) line.getBasePoints().get(0).y - 2.5f,
                            5, (float) (line.getBasePoints().get(1).y - line.getBasePoints().get(0).y));
                    pathRectangles.add(rectangle.applyMargins(0.5f, 0.5f, 0.5f, 0.5f, false));
                }
            }
        }
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        return null;
    }


    public Set<Rectangle> foundOverlappingText() {
        List<Rectangle> textRectangleList = new ArrayList<>(textRectangles);

        while (!textRectangleList.isEmpty()) {
            Rectangle testRectangle = textRectangleList.remove(textRectangleList.size() - 1);
            for (Rectangle rectangle : pathRectangles) {
                if (intersect(testRectangle, rectangle)) {
                    interceptedRecs.add(rectangle);
                }
            }
        }

        List<Rectangle> imageRectangleList = new ArrayList<>(imageRectangles);

        while(!imageRectangleList.isEmpty()){
            Rectangle testRectangle = imageRectangleList.remove(imageRectangleList.size() - 1);
            for (Rectangle rectangle : pathRectangles) {
                if (intersect(testRectangle, rectangle)) {
                    interceptedRecs.add(rectangle);
                }
            }
        }

        return interceptedRecs;
    }

    private boolean intersect(Rectangle a, Rectangle b) {
        return intersect(a.getLeft(), a.getRight(), b.getLeft(), b.getRight()) &&
                intersect(a.getBottom(), a.getTop(), b.getBottom(), b.getTop());
    }

    private boolean intersect(float start1, float end1, float start2, float end2) {
        if (start1 < start2)
            return start2 <= end1;
        else
            return start1 <= end2;
    }
}
