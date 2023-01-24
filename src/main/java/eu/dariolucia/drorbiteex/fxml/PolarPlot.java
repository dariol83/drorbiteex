/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;

import java.net.URL;
import java.util.*;

// TODO: move into separate package, make it more customisable:
//  - tooltip with coordinates on mouse over,
//  - height/width easy definition/stretching
public class PolarPlot implements Initializable {

    private static final double MAX_RADIUS_FACTOR = 0.85;

    public Canvas canvas;

    private SimpleObjectProperty<Color> backgroundColor;
    private SimpleObjectProperty<Color> foregroundColor;

    private final Map<UUID, SpacecraftTrack> trackMap = new LinkedHashMap<>();
    private final Map<UUID, SpacecraftTrackPoint> positionMap = new LinkedHashMap<>();
    private final Map<UUID, Color> colorMap = new HashMap<>();
    private final Map<PlotPosition, Pair<Color, String>> textMap = new EnumMap<>(PlotPosition.class);
    private ChangeListener<Object> refresher;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.refresher = (a,b,c) -> refresh();

        this.backgroundColor = new SimpleObjectProperty<>(Color.WHITE);
        this.foregroundColor = new SimpleObjectProperty<>(Color.BLACK);

        this.backgroundColor.addListener(this.refresher);
        this.foregroundColor.addListener(this.refresher);

        refresh();
    }

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }

    public Color getForegroundColor() {
        return foregroundColor.get();
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor.set(foregroundColor);
    }

    public void setSpacecraftTrack(VisibilityWindow track) {
        if(track != null) {
            SpacecraftTrack st = new SpacecraftTrack(track);
            this.trackMap.put(track.getOrbit().getId(), st);
            this.colorMap.put(track.getOrbit().getId(), Color.valueOf(track.getOrbit().getColor()));

            refresh();
        }
    }

    public void setSpacecraftPosition(UUID id, String name, Point2D position, Color color) {
        if(position != null) {
            SpacecraftTrackPoint stp = new SpacecraftTrackPoint(name, position);
            this.positionMap.put(id, stp);
            if(color != null) {
                this.colorMap.put(id, color);
            }

            refresh();
        }
    }

    public void setColor(UUID id, Color color) {
        if(color != null) {
            this.colorMap.put(id, color);

            refresh();
        }
    }

    public void setText(PlotPosition position, String text, Color color) {
        this.textMap.put(position, new Pair<>(color, text));

        refresh();
    }

    public void clear() {
        this.trackMap.clear();
        this.positionMap.clear();
        this.colorMap.clear();
        this.textMap.clear();

        refresh();
    }

    private void refresh() {
        GraphicsContext gc = this.canvas.getGraphicsContext2D();
        drawBackground(gc);
        drawPlot(gc);
        drawPass(gc);
        drawSpacecraftLocation(gc);
        drawAngleText(gc);
    }

    private void drawAngleText(GraphicsContext gc) {
        Font previous = gc.getFont();
        gc.setFont(new Font(previous.getName(), previous.getSize() - 2));
        for(Map.Entry<PlotPosition, Pair<Color, String>> entry : this.textMap.entrySet()) {
            gc.setStroke(entry.getValue().getKey());
            switch (entry.getKey()) {
                case TOP_LEFT:
                    gc.strokeText(entry.getValue().getValue(),2, 15);
                    break;
                case TOP_RIGHT:
                    gc.strokeText(entry.getValue().getValue(), canvas.getWidth() - 100, 15);
                    break;
                case BOTTOM_LEFT:
                    gc.strokeText(entry.getValue().getValue(), 2, canvas.getHeight() - 35);
                    break;
                case BOTTOM_RIGHT:
                    gc.strokeText(entry.getValue().getValue(), canvas.getWidth() - 100, canvas.getHeight() - 35);
            }
        }
        gc.setFont(previous);
    }

    public void updateSize(double size) {
        canvas.setHeight(size);
        canvas.setWidth(size);

        refresh();
    }

    private void drawSpacecraftLocation(GraphicsContext gc) {
        for(Map.Entry<UUID, SpacecraftTrackPoint> entry : this.positionMap.entrySet()) {
            Point2D location = entry.getValue().getPoint();
            Color color = this.colorMap.get(entry.getKey());
            gc.setStroke(color);
            gc.setFill(color);
            gc.setLineWidth(1.5);
            Font previous = gc.getFont();
            gc.setFont(new Font(previous.getName(), previous.getSize() - 2));
            if (location != null) {
                Point2D p1 = toXY(location.getX(), location.getY());
                gc.fillOval(p1.getX() - 7, p1.getY() - 7, 14, 14);
                gc.setFill(Color.WHITE);
                gc.fillOval(p1.getX() - 3, p1.getY() - 3, 6, 6);
                gc.setFill(color);
            }
            gc.setFont(previous);
        }
    }

    private void drawPass(GraphicsContext gc) {
        for(Map.Entry<UUID, SpacecraftTrack> entry : this.trackMap.entrySet()) {
            SpacecraftTrack st = entry.getValue();
            if (st != null) {
                TrackPoint maxElPoint = null;
                Color color = this.colorMap.get(entry.getKey());
                gc.setStroke(color);
                gc.setFill(color);
                gc.setLineWidth(2);
                List<TrackPoint> track = st.getTrack();
                for (int i = 0; i < track.size() - 1; ++i) {
                    // Draw line from point to point
                    TrackPoint tp1 = track.get(i);
                    TrackPoint tp2 = track.get(i + 1);
                    Point2D p1 = toXY(tp1.getAzimuth(), tp1.getElevation());
                    Point2D p2 = toXY(tp2.getAzimuth(), tp2.getElevation());
                    if (p1.getY() < 0 || p2.getY() < 0) {
                        continue;
                    }
                    gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    if (i == 0) {
                        gc.fillRect(p1.getX() - 2, p1.getY() - 2, 4, 4);
                    } else if (i == track.size() - 2) {
                        gc.strokeLine(p2.getX() - 3, p2.getY() - 3, p2.getX() + 3, p2.getY() + 3);
                        gc.strokeLine(p2.getX() - 3, p2.getY() + 3, p2.getX() + 3, p2.getY() - 3);
                    }
                    if (maxElPoint == null) {
                        maxElPoint = tp1;
                    } else {
                        if (maxElPoint.getElevation() < tp2.getElevation()) {
                            maxElPoint = tp2;
                        }
                    }
                }
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

    public void setNewSpacecraftPosition(GroundStation groundStation, Orbit orbit, TrackPoint currentLocation) {
        SpacecraftTrack currentTrack = this.trackMap.get(orbit.getId());
        if(currentTrack != null &&
            currentTrack.getWindow().getStation().equals(groundStation) &&
            currentTrack.getWindow().getOrbit().equals(orbit)) {
            // If the current location is null, the SC went out of visibility
            if(currentLocation == null) {
                this.positionMap.remove(orbit.getId());
                refresh();
            } else {
                if (currentTrack.contains(currentLocation.getTime())) {
                    // set spacecraft position
                    Point2D currentScPos = new Point2D(currentLocation.getAzimuth(), currentLocation.getElevation());
                    setSpacecraftPosition(orbit.getId(), orbit.getName(), currentScPos, Color.valueOf(orbit.getColor()));
                }
            }
        }
    }

    public VisibilityWindow updateCurrentData(GroundStation groundStation, Orbit orbit, List<VisibilityWindow> visibilityWindows) {
        SpacecraftTrack currentTrack = this.trackMap.get(orbit.getId());
        if(currentTrack != null &&
                currentTrack.getWindow().getStation().equals(groundStation) &&
                currentTrack.getWindow().getOrbit().equals(orbit)) {
            // There is a visibility window selected, replace it
            if(visibilityWindows != null) {
                for (VisibilityWindow vw : visibilityWindows) {
                    if (vw.getOrbitNumber() == currentTrack.getWindow().getOrbitNumber()) {
                        // Found
                        setSpacecraftTrack(vw);
                        return vw;
                    }
                }
            }
            // At this stage, it means that the visibility window disappeared -> clear the track
            clearSpacecraftTrack(currentTrack.getWindow().getOrbit().getId());
            clearSpacecraftTrackPosition(currentTrack.getWindow().getOrbit().getId());
        }
        return null;
    }

    public void clearSpacecraftTrackPosition(UUID id) {
        if(this.positionMap.remove(id) != null) {
            refresh();
        }
    }

    public void clearSpacecraftTrack(UUID id) {
        if(this.trackMap.remove(id) != null) {
            refresh();
        }
    }

    public static class SpacecraftTrackPoint {

        private final String name;

        private Point2D point;

        public SpacecraftTrackPoint(String name) {
            this(name, null);
        }

        public SpacecraftTrackPoint(String name, Point2D point) {
            this.name = name;
            this.point = point;
        }

        public String getName() {
            return name;
        }

        public Point2D getPoint() {
            return point;
        }

        public void setPoint(Point2D point) {
            this.point = point;
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

    public enum PlotPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
