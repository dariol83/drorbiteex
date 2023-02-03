package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.util.List;

public class PolarPlotPainter {

    private static final double MAX_RADIUS_FACTOR = 0.85;

    private final double width;
    private final double height;
    private final GraphicsContext gc;
    public PolarPlotPainter(GraphicsContext gc, double width, double height) {
        this.width = width;
        this.height = height;
        this.gc = gc;
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public void drawTrack(Color strokeColor, List<TrackPoint> track) {
        gc.setStroke(strokeColor);
        gc.setFill(strokeColor);
        gc.setLineWidth(2);
        Point2D lastDrawnPoint = null;
        Point2D firstDrawnPoint = null;
        for (int i = 0; i < track.size() - 1; ++i) {
            // Draw line from point to point
            TrackPoint tp1 = track.get(i);
            TrackPoint tp2 = track.get(i + 1);
            if(tp1.getElevation() < 0 || tp2.getElevation() < 0) {
                continue;
            }
            Point2D p1 = toScreenPoint(tp1.getAzimuth(), tp1.getElevation(), width, height);
            Point2D p2 = toScreenPoint(tp2.getAzimuth(), tp2.getElevation(), width, height);
            if (p1.getY() < 0 || p2.getY() < 0) {
                continue;
            }
            if(i == 0) {
                firstDrawnPoint = p1;
            }
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            lastDrawnPoint = p2;
        }
        if(firstDrawnPoint != null) {
            gc.fillRect(firstDrawnPoint.getX() - 2, firstDrawnPoint.getY() - 2, 4, 4);
        }
        if(lastDrawnPoint != null) {
            gc.strokeLine(lastDrawnPoint.getX() - 3, lastDrawnPoint.getY() - 3, lastDrawnPoint.getX() + 3, lastDrawnPoint.getY() + 3);
            gc.strokeLine(lastDrawnPoint.getX() - 3, lastDrawnPoint.getY() + 3, lastDrawnPoint.getX() + 3, lastDrawnPoint.getY() - 3);
        }
    }

    public void drawBackground(Color backgroundColor) {
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);
    }

    public void drawPlot(Color foregroundColor) {
        gc.setStroke(foregroundColor);
        gc.setFill(foregroundColor);
        Point2D xyRadius = new Point2D(width/2, height/2);
        // 90-75-50-25-0
        gc.setLineWidth(2.0);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR,
                width * MAX_RADIUS_FACTOR,
                height * MAX_RADIUS_FACTOR);
        gc.setLineWidth(1.0);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.75,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.75,
                width * MAX_RADIUS_FACTOR * 0.75,
                height * MAX_RADIUS_FACTOR * 0.75);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.5,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.5,
                width * MAX_RADIUS_FACTOR * 0.5,
                height * MAX_RADIUS_FACTOR * 0.5);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.25,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.25,
                width * MAX_RADIUS_FACTOR * 0.25,
                height * MAX_RADIUS_FACTOR * 0.25);
        gc.fillOval(xyRadius.getX() - 1,
                xyRadius.getY() - 1,
                2,
                2);
        gc.strokeLine(xyRadius.getX(), xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR,
                xyRadius.getX(), height - (xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR));
        gc.strokeLine(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR, xyRadius.getY(),
                width - (xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR), xyRadius.getY());
        // Draw numbers
        gc.strokeText("0", xyRadius.getX() - 2, 0 + gc.getFont().getSize());
        gc.strokeText("90", xyRadius.getX() * 2 - 25, xyRadius.getY() + 3);
        gc.strokeText("180", xyRadius.getX() - 10, xyRadius.getY() * 2 - 5);
        gc.strokeText("270", 3, xyRadius.getY() + 3);
    }

    public void drawAngleText(Color color, PolarPlot.PlotPosition position, String text) {
        Font previous = gc.getFont();
        gc.setFont(new Font(previous.getName(), previous.getSize() - 2));
        gc.setStroke(color);
        switch (position) {
            case TOP_LEFT:
                gc.strokeText(text,2, 15);
                break;
            case TOP_RIGHT:
                gc.strokeText(text, width - 100, 15);
                break;
            case BOTTOM_LEFT:
                gc.strokeText(text, 2, height - 35);
                break;
            case BOTTOM_RIGHT:
                gc.strokeText(text, width - 100, height - 35);
        }
        gc.setFont(previous);
    }

    public void drawSpacecraftLocation(Color color, Point2D location) {
        Point2D p1 = location != null ? toScreenPoint(location.getX(), location.getY(), width, height) : null;
        if(p1 != null) {
            gc.setStroke(color);
            gc.setFill(color);
            gc.setLineWidth(1.5);
            gc.fillOval(p1.getX() - 7, p1.getY() - 7, 14, 14);
            gc.setFill(Color.WHITE);
            gc.fillOval(p1.getX() - 3, p1.getY() - 3, 6, 6);
            gc.setFill(color);
        }
    }

    public static Point2D toScreenPoint(double az, double el, double width, double height) {
        // Elevation: radius length: 0 = radius; 90 = 0
        double radius = (width/2 * MAX_RADIUS_FACTOR) * (90.0 - el)/90.0;
        double y = height/2 - radius * Math.cos(Math.toRadians(az));
        double x = width/2 + radius * Math.sin(Math.toRadians(az));
        return new Point2D(x, y);
    }

    public static Point2D toPolarPoint(double x, double y, double width, double height) {
        // Get distance from the center of the plot to compute the actual radius
        double radius = Math.sqrt(Math.pow(x - width/2, 2) + Math.pow(y - height/2, 2));
        // Compute the angle (x,y) - (width/2, height/2) - (width/2, 0)
        Point2D scPositionPoint = new Point2D(x - width/2,y - height/2).normalize();
        Point2D referencePoint = new Point2D(0,-1);
        double res = referencePoint.dotProduct(scPositionPoint);
        double azimuth = Math.toDegrees(Math.acos(res));
        // Solve acos angle ambiguity
        if(x < width/2) {
            azimuth = 360 - azimuth;
        }
        double elevation = 90 - radius/(MAX_RADIUS_FACTOR * width/2) * 90;
        if(elevation < 0) {
            return null;
        } else {
            return new Point2D(elevation, azimuth);
        }
    }
}
