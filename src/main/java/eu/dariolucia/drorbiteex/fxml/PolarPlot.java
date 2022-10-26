package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.data.VisibilityWindow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.orekit.propagation.SpacecraftState;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class PolarPlot implements Initializable {

    private static final double MAX_RADIUS_FACTOR = 0.85;

    public Canvas canvas;

    private SimpleObjectProperty<Color> backgroundColor;
    private SimpleObjectProperty<Color> foregroundColor;
    private SimpleObjectProperty<Color> trackColor;
    private SimpleObjectProperty<Color> spacecraftColor;
    private SimpleObjectProperty<SpacecraftTrack> spacecraftTrack;
    private SimpleObjectProperty<Point2D> spacecraftPosition;
    private ChangeListener<Object> refresher;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.refresher = (a,b,c) -> refresh();

        this.backgroundColor = new SimpleObjectProperty<>(Color.WHITE);
        this.foregroundColor = new SimpleObjectProperty<>(Color.BLACK);
        this.trackColor = new SimpleObjectProperty<>(Color.RED);
        this.spacecraftColor = new SimpleObjectProperty<>(Color.RED);
        this.spacecraftTrack = new SimpleObjectProperty<>(null);
        this.spacecraftPosition = new SimpleObjectProperty<>(null);

        this.backgroundColor.addListener(this.refresher);
        this.foregroundColor.addListener(this.refresher);
        this.trackColor.addListener(this.refresher);
        this.spacecraftColor.addListener(this.refresher);
        this.spacecraftTrack.addListener(this.refresher);
        this.spacecraftPosition.addListener(this.refresher);

        refresh();
    }

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public SimpleObjectProperty<Color> backgroundColorProperty() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }

    public Color getForegroundColor() {
        return foregroundColor.get();
    }

    public SimpleObjectProperty<Color> foregroundColorProperty() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor.set(foregroundColor);
    }

    public Color getTrackColor() {
        return trackColor.get();
    }

    public SimpleObjectProperty<Color> trackColorProperty() {
        return trackColor;
    }

    public void setTrackColor(Color trackColor) {
        this.trackColor.set(trackColor);
    }

    public Color getSpacecraftColor() {
        return spacecraftColor.get();
    }

    public SimpleObjectProperty<Color> spacecraftColorProperty() {
        return spacecraftColor;
    }

    public void setSpacecraftColor(Color spacecraftColor) {
        this.spacecraftColor.set(spacecraftColor);
    }

    public void setSpacecraftTrack(String spacecraft, List<VisibilityWindow.SpacecraftTrackPoint> track) {
        if(track != null) {
            this.spacecraftTrack.set(new SpacecraftTrack(spacecraft, track.toArray(new VisibilityWindow.SpacecraftTrackPoint[0])));
        } else {
            this.spacecraftTrack.set(new SpacecraftTrack(spacecraft, new VisibilityWindow.SpacecraftTrackPoint[0]));
        }
        this.spacecraftPosition.set(null);
    }

    private void refresh() {
        GraphicsContext gc = this.canvas.getGraphicsContext2D();
        drawBackground(gc);
        drawPlot(gc);
        drawPass(gc);
        drawSpacecraftLocation(gc);
    }

    private void drawSpacecraftLocation(GraphicsContext gc) {
        Point2D location = this.spacecraftPosition.get();
        SpacecraftTrack st = this.spacecraftTrack.get();
        gc.setStroke(trackColor.get());
        gc.setFill(trackColor.get());
        gc.setLineWidth(1.5);
        if(location != null && st != null) {
            Point2D p1 = toXY(location);
            gc.fillOval(p1.getX() - 5, p1.getY() - 5, 10, 10);
            gc.setFill(Color.WHITE);
            gc.fillOval(p1.getX() - 3, p1.getY() - 3, 6, 6);
            gc.setFill(trackColor.get());
            // TL: entry
            gc.strokeText(st.getName() + "\nAZ " + doublePrint(location.getX(), 4) + "\nEL " + doublePrint(location.getY(), 4), 2, 15);
        } else if(st != null) {
            // TL: entry
            gc.strokeText(st.getName(), 2, 15);
        }
    }

    private void drawPass(GraphicsContext gc) {
        SpacecraftTrack st = this.spacecraftTrack.get();
        if(st != null) {
            VisibilityWindow.SpacecraftTrackPoint maxElPoint = null;
            gc.setStroke(trackColor.get());
            gc.setFill(trackColor.get());
            gc.setLineWidth(1.5);
            VisibilityWindow.SpacecraftTrackPoint[] track = st.getTrack();
            for(int i = 0; i < track.length - 1; ++i) {
                // Draw line from point to point
                Point2D p1 = toXY(track[i].getAzimuthElevation());
                Point2D p2 = toXY(track[i + 1].getAzimuthElevation());
                if(p1.getY() < 0 || p2.getY() < 0) {
                    continue;
                }
                gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                if(i == 0) {
                    gc.fillRect(p1.getX() - 2, p1.getY() - 2, 4, 4);
                } else if(i == track.length - 2) {
                    gc.strokeLine(p2.getX() - 3, p2.getY() - 3, p2.getX() + 3, p2.getY() + 3);
                    gc.strokeLine(p2.getX() - 3, p2.getY() + 3, p2.getX() + 3, p2.getY() - 3);
                }
                if(maxElPoint == null) {
                    maxElPoint = track[i];
                } else {
                    if(maxElPoint.getAzimuthElevation().getY() < track[i+1].getAzimuthElevation().getY()) {
                        maxElPoint = track[i+1];
                    }
                }
            }
            // Text
            gc.setStroke(getForegroundColor());
            // BL: entry
            gc.strokeText("Start\nAZ " + doublePrint(track[0].getAzimuthElevation().getX(), 4) + "\nEL " + doublePrint(track[0].getAzimuthElevation().getY(), 4), 2, canvas.getHeight() - 40);
            // BR: exit
            gc.strokeText("End\nAZ " + doublePrint(track[track.length - 1].getAzimuthElevation().getX(), 4) + "\nEL " + doublePrint(track[track.length - 1].getAzimuthElevation().getY(), 4), canvas.getWidth() - 70, canvas.getHeight() - 40);
            // TR: max elevation
            if(maxElPoint != null) {
                gc.strokeText("Max EL\nAZ " + doublePrint(maxElPoint.getAzimuthElevation().getX(), 4) + "\nEL " + doublePrint(maxElPoint.getAzimuthElevation().getY(), 4), canvas.getWidth() - 70, 15);
            }
        }
    }

    private String doublePrint(double value, int places) {
        return String.format("%." + places + "f", value);
    }

    private Point2D toXY(Point2D azEl) {
        // Elevation: radius length: 0 = radius; 90 = 0
        double radius = (canvas.getWidth()/2 * MAX_RADIUS_FACTOR) * (90.0 - azEl.getY())/90.0;
        double y = canvas.getHeight()/2 - radius * Math.cos(Math.toRadians(azEl.getX()));
        double x = canvas.getWidth()/2 + radius * Math.sin(Math.toRadians(azEl.getX()));
        return new Point2D(x, y);
    }

    private void drawPlot(GraphicsContext gc) {
        gc.setStroke(foregroundColor.get());
        gc.setFill(foregroundColor.get());
        Point2D xyRadius = new Point2D(canvas.getWidth()/2, canvas.getHeight()/2);
        // 90-75-50-25-0
        gc.setLineWidth(2.0);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR,
                        xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR,
                        canvas.getWidth() * MAX_RADIUS_FACTOR,
                        canvas.getHeight() * MAX_RADIUS_FACTOR);
        gc.setLineWidth(1.0);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.75,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.75,
                canvas.getWidth() * MAX_RADIUS_FACTOR * 0.75,
                canvas.getHeight() * MAX_RADIUS_FACTOR * 0.75);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.5,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.5,
                canvas.getWidth() * MAX_RADIUS_FACTOR * 0.5,
                canvas.getHeight() * MAX_RADIUS_FACTOR * 0.5);
        gc.strokeOval(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR * 0.25,
                xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR * 0.25,
                canvas.getWidth() * MAX_RADIUS_FACTOR * 0.25,
                canvas.getHeight() * MAX_RADIUS_FACTOR * 0.25);
        gc.fillOval(xyRadius.getX() - 1,
                xyRadius.getY() - 1,
                2,
                2);
        gc.strokeLine(xyRadius.getX(), xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR,
                xyRadius.getX(), canvas.getHeight() - (xyRadius.getY() - xyRadius.getY() * MAX_RADIUS_FACTOR));
        gc.strokeLine(xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR, xyRadius.getY(),
                canvas.getWidth() - (xyRadius.getX() - xyRadius.getX() * MAX_RADIUS_FACTOR), xyRadius.getY());
        // Draw numbers
        gc.strokeText("0", xyRadius.getX() - 2, 0 + gc.getFont().getSize());
        gc.strokeText("90", xyRadius.getX() * 2 - 25, xyRadius.getY() + 3);
        gc.strokeText("180", xyRadius.getX() - 10, xyRadius.getY() * 2 - 5);
        gc.strokeText("270", 3, xyRadius.getY() + 3);
    }

    private void drawBackground(GraphicsContext gc) {
        gc.setFill(backgroundColor.get());
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void setDisable(boolean b) {
        this.canvas.setDisable(b);
    }

    public void clear() {
        this.spacecraftTrack.set(null);
        this.spacecraftPosition.set(null);
    }

    public void newSpacecraftPosition(String name, SpacecraftState currentLocation, Date now) {
        SpacecraftTrack currentTrack = spacecraftTrack.get();
        if(currentTrack != null && currentTrack.getName().equals(name)) {
            if(currentTrack.contains(now)) {
                // set spacecraft position
                Point2D currentScPos = currentTrack.getTrack()[0].getVisibilityWindow().convertToAzimuthElevation(currentLocation);
                System.out.println("Spacecraft " + name + ", position AZ/EL" + currentScPos);
                this.spacecraftPosition.set(currentScPos);
            }
        }
    }

    public static class SpacecraftTrack {
        private final String name;
        private final VisibilityWindow.SpacecraftTrackPoint[] track;

        public SpacecraftTrack(String name, VisibilityWindow.SpacecraftTrackPoint[] track) {
            this.name = name;
            this.track = track;
        }

        public String getName() {
            return name;
        }

        public VisibilityWindow.SpacecraftTrackPoint[] getTrack() {
            return track;
        }

        public boolean contains(Date now) {
            VisibilityWindow.SpacecraftTrackPoint spt1 = track[0];
            VisibilityWindow.SpacecraftTrackPoint spt2 = track[track.length - 1];
            return now.after(spt1.getTime()) && now.before(spt2.getTime());
        }
    }
}
