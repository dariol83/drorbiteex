package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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

    public void setSpacecraftTrack(VisibilityWindow track) {
        if(track != null) {
            this.spacecraftTrack.set(new SpacecraftTrack(track));
        } else {
            this.spacecraftTrack.set(null);
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
            Point2D p1 = toXY(location.getX(), location.getY());
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
            TrackPoint maxElPoint = null;
            gc.setStroke(trackColor.get());
            gc.setFill(trackColor.get());
            gc.setLineWidth(1.5);
            List<TrackPoint> track = st.getTrack();
            for(int i = 0; i < track.size() - 1; ++i) {
                // Draw line from point to point
                TrackPoint tp1 = track.get(i);
                TrackPoint tp2 = track.get(i + 1);
                Point2D p1 = toXY(tp1.getAzimuth(), tp1.getElevation());
                Point2D p2 = toXY(tp2.getAzimuth(), tp2.getElevation());
                if(p1.getY() < 0 || p2.getY() < 0) {
                    continue;
                }
                gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                if(i == 0) {
                    gc.fillRect(p1.getX() - 2, p1.getY() - 2, 4, 4);
                } else if(i == track.size() - 2) {
                    gc.strokeLine(p2.getX() - 3, p2.getY() - 3, p2.getX() + 3, p2.getY() + 3);
                    gc.strokeLine(p2.getX() - 3, p2.getY() + 3, p2.getX() + 3, p2.getY() - 3);
                }
                if(maxElPoint == null) {
                    maxElPoint = tp1;
                } else {
                    if(maxElPoint.getElevation() < tp2.getElevation()) {
                        maxElPoint = tp2;
                    }
                }
            }
            // Text
            gc.setStroke(getForegroundColor());
            // BL: entry
            gc.strokeText("Start\nAZ " + doublePrint(track.get(0).getAzimuth(), 4) + "\nEL " + doublePrint(track.get(0).getElevation(), 4), 2, canvas.getHeight() - 40);
            // BR: exit
            gc.strokeText("End\nAZ " + doublePrint(track.get(track.size() - 1).getAzimuth(), 4) + "\nEL " + doublePrint(track.get(track.size() - 1).getElevation(), 4), canvas.getWidth() - 70, canvas.getHeight() - 40);
            // TR: max elevation
            if(maxElPoint != null) {
                gc.strokeText("Max EL\nAZ " + doublePrint(maxElPoint.getAzimuth(), 4) + "\nEL " + doublePrint(maxElPoint.getElevation(), 4), canvas.getWidth() - 70, 15);
            }
        }
    }

    private String doublePrint(double value, int places) {
        return String.format("%." + places + "f", value);
    }

    private Point2D toXY(double az, double el) {
        // Elevation: radius length: 0 = radius; 90 = 0
        double radius = (canvas.getWidth()/2 * MAX_RADIUS_FACTOR) * (90.0 - el)/90.0;
        double y = canvas.getHeight()/2 - radius * Math.cos(Math.toRadians(az));
        double x = canvas.getWidth()/2 + radius * Math.sin(Math.toRadians(az));
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

    public void setNewSpacecraftPosition(GroundStation groundStation, Orbit orbit, TrackPoint currentLocation) {
        SpacecraftTrack currentTrack = spacecraftTrack.get();
        if(currentTrack != null &&
            currentTrack.getWindow().getStation().equals(groundStation) &&
            currentTrack.getWindow().getOrbit().equals(orbit)) {
            // If the current location is null, the SC went out of visibility
            if(currentLocation == null) {
                this.spacecraftPosition.set(null);
            } else {
                if (currentTrack.contains(currentLocation.getTime())) {
                    // set spacecraft position
                    Point2D currentScPos = new Point2D(currentLocation.getAzimuth(), currentLocation.getElevation());
                    System.out.println("Spacecraft " + orbit.getName() + ", position AZ/EL" + currentScPos);
                    this.spacecraftPosition.set(currentScPos);
                }
            }
        }
    }

    public void updateCurrentData(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows) {
        SpacecraftTrack currentTrack = spacecraftTrack.get();
        if(currentTrack != null &&
                currentTrack.getWindow().getStation().equals(groundStation) &&
                currentTrack.getWindow().getOrbit().equals(orbit)) {
            // There is a visibility window selected, replace it
            for(VisibilityWindow vw : visibilityWindows) {
                if(vw.getOrbitNumber() == currentTrack.getWindow().getOrbitNumber()) {
                    // Found
                    spacecraftTrack.set(new SpacecraftTrack(vw));
                    return;
                }
            }
            // At this stage, it means that the visibility window disappeared -> clear plot
            clear();
        }
    }

    public static class SpacecraftTrack {

        private final VisibilityWindow window;

        public SpacecraftTrack(VisibilityWindow window) {
            this.window = window;
        }

        public VisibilityWindow getWindow() {
            return window;
        }

        public String getName() {
            return window.getOrbit().getName();
        }

        public List<TrackPoint> getTrack() {
            return window.getGroundTrack();
        }

        public boolean contains(Date now) {
            TrackPoint spt1 = getTrack().get(0);
            TrackPoint spt2 = getTrack().get(getTrack().size() - 1);
            return now.after(spt1.getTime()) && now.before(spt2.getTime());
        }
    }
}
