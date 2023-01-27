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
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.net.URL;
import java.util.*;

public class PolarPlot implements Initializable {

    public Canvas canvas;

    private SimpleObjectProperty<Color> backgroundColor;
    private SimpleObjectProperty<Color> foregroundColor;

    private final Map<UUID, SpacecraftTrack> trackMap = new LinkedHashMap<>();
    private final Map<UUID, SpacecraftTrackPoint> positionMap = new LinkedHashMap<>();
    private final Map<UUID, Color> colorMap = new HashMap<>();
    private final Map<PlotPosition, Pair<Color, String>> textMap = new EnumMap<>(PlotPosition.class);

    private ISpacecraftDrawStrategy spacecraftDrawStrategy = null;

    private ChangeListener<Object> refresher;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.refresher = (a,b,c) -> refresh();

        this.backgroundColor = new SimpleObjectProperty<>(Color.WHITE);
        this.foregroundColor = new SimpleObjectProperty<>(Color.BLACK);

        this.backgroundColor.addListener(this.refresher);
        this.foregroundColor.addListener(this.refresher);

        // Tooltip for coordinates
        Tooltip tooltip = new Tooltip();
        Tooltip.install(canvas, tooltip);
        tooltip.setHideDelay(Duration.millis(5000));
        tooltip.setShowDelay(Duration.millis(200));
        canvas.setOnMouseMoved(e -> {
            Point2D elAzCoordinates = PolarPlotPainter.toPolarPoint(e.getX(), e.getY(), canvas.getWidth(), canvas.getHeight());
            if(elAzCoordinates.getX() > 0) {
                tooltip.setText(String.format("EL: %.3f - AZ: %.3f", elAzCoordinates.getX(), elAzCoordinates.getY()));
            } else {
                tooltip.hide();
            }
        });
        canvas.setOnMouseExited(e -> tooltip.hide());
        // Menu for image copy
        canvas.setOnContextMenuRequested(e -> {
            ContextMenu m = new ContextMenu();
            final MenuItem copyItem = new MenuItem("Copy image to clipboard");
            copyItem.setOnAction(event -> {
                WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                image = canvas.snapshot(null, image);
                ClipboardContent content = new ClipboardContent();
                content.putImage(image);
                Clipboard.getSystemClipboard().setContent(content);
            });
            m.getItems().add(copyItem);
            m.show(canvas.getScene().getWindow(), e.getScreenX(), e.getScreenY());
        });

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
        PolarPlotPainter painter = new PolarPlotPainter(gc, canvas.getWidth(), canvas.getHeight());
        drawBackground(painter);
        drawPlot(painter);
        drawPass(painter);
        drawSpacecraftLocation(painter);
        drawAngleText(painter);
    }

    private void drawAngleText(PolarPlotPainter painter) {
        for(Map.Entry<PlotPosition, Pair<Color, String>> entry : this.textMap.entrySet()) {
            painter.drawAngleText(entry.getValue().getKey(), entry.getKey(), entry.getValue().getValue());
        }
    }

    public void updateSize(double size) {
        canvas.setHeight(size);
        canvas.setWidth(size);

        refresh();
    }

    public void setSpacecraftDrawStrategy(ISpacecraftDrawStrategy spacecraftDrawStrategy) {
        this.spacecraftDrawStrategy = spacecraftDrawStrategy;
    }

    private void drawSpacecraftLocation(PolarPlotPainter painter) {
        for(Map.Entry<UUID, SpacecraftTrackPoint> entry : this.positionMap.entrySet()) {
            Color color = this.colorMap.get(entry.getKey());
            Point2D location = entry.getValue().getPoint();
            if (this.spacecraftDrawStrategy != null) {
                Point2D p1 = location != null ? PolarPlotPainter.toScreenPoint(location.getX(), location.getY(), canvas.getWidth(), canvas.getHeight()) : null;
                if(p1 != null) {
                    this.spacecraftDrawStrategy.draw(painter.getGraphicsContext(), color, p1, entry.getValue().getName());
                }
            } else {
                painter.drawSpacecraftLocation(color, location);
            }
        }
    }

    private void drawPass(PolarPlotPainter painter) {
        for(Map.Entry<UUID, SpacecraftTrack> entry : this.trackMap.entrySet()) {
            SpacecraftTrack st = entry.getValue();
            Color color = this.colorMap.get(entry.getKey());
            if (st != null) {
                painter.drawTrack(color, st.getTrack());
            }
        }
    }

    private void drawPlot(PolarPlotPainter painter) {
        painter.drawPlot(foregroundColor.get());
    }

    private void drawBackground(PolarPlotPainter painter) {
        painter.drawBackground(backgroundColor.get());
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

    public interface ISpacecraftDrawStrategy {
        void draw(GraphicsContext gc, Color color, Point2D location, String orbitName);
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
