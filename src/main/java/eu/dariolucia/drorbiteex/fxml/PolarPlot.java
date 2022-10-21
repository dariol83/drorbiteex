package eu.dariolucia.drorbiteex.fxml;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class PolarPlot implements Initializable {

    private static final double MAX_RADIUS_FACTOR = 0.85;

    public Canvas canvas;

    private SimpleObjectProperty<Color> backgroundColor;
    private SimpleObjectProperty<Color> foregroundColor;
    private SimpleObjectProperty<Color> trackColor;
    private SimpleObjectProperty<Color> spacecraftColor;
    private SimpleStringProperty spacecraftName;
    private SimpleObjectProperty<Point2D> spacecraftCurrentElevationAzimuth;
    private SimpleObjectProperty<Point2D[]> spacecraftTrack;
    private ChangeListener<Object> refresher;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.refresher = (a,b,c) -> refresh();

        this.backgroundColor = new SimpleObjectProperty<>(Color.WHITE);
        this.foregroundColor = new SimpleObjectProperty<>(Color.BLACK);
        this.trackColor = new SimpleObjectProperty<>(Color.RED);
        this.spacecraftColor = new SimpleObjectProperty<>(Color.RED);
        this.spacecraftName = new SimpleStringProperty(null);
        this.spacecraftCurrentElevationAzimuth = new SimpleObjectProperty<>(null);
        this.spacecraftTrack = new SimpleObjectProperty<>(null);

        this.backgroundColor.addListener(this.refresher);
        this.foregroundColor.addListener(this.refresher);
        this.trackColor.addListener(this.refresher);
        this.spacecraftColor.addListener(this.refresher);
        this.spacecraftName.addListener(this.refresher);
        this.spacecraftCurrentElevationAzimuth.addListener(this.refresher);
        this.spacecraftTrack.addListener(this.refresher);

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

    public String getSpacecraftName() {
        return spacecraftName.get();
    }

    public SimpleStringProperty spacecraftNameProperty() {
        return spacecraftName;
    }

    public void setSpacecraftName(String spacecraftName) {
        this.spacecraftName.set(spacecraftName);
    }

    public Point2D getSpacecraftCurrentElevationAzimuth() {
        return spacecraftCurrentElevationAzimuth.get();
    }

    public SimpleObjectProperty<Point2D> spacecraftCurrentElevationAzimuthProperty() {
        return spacecraftCurrentElevationAzimuth;
    }

    public void setSpacecraftCurrentElevationAzimuth(Point2D spacecraftCurrentElevationAzimuth) {
        this.spacecraftCurrentElevationAzimuth.set(spacecraftCurrentElevationAzimuth);
    }

    public Point2D[] getSpacecraftTrack() {
        return spacecraftTrack.get();
    }

    public SimpleObjectProperty<Point2D[]> spacecraftTrackProperty() {
        return spacecraftTrack;
    }

    public void setSpacecraftTrack(Point2D[] spacecraftTrack) {
        this.spacecraftTrack.set(spacecraftTrack);
    }

    private void refresh() {
        GraphicsContext gc = this.canvas.getGraphicsContext2D();
        drawBackground(gc);
        drawPlot(gc);
        drawPass(gc);
        drawSpacecraftLocation(gc);
    }

    private void drawSpacecraftLocation(GraphicsContext gc) {

    }

    private void drawPass(GraphicsContext gc) {

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
}
