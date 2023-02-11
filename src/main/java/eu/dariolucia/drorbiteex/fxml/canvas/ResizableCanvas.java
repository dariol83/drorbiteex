package eu.dariolucia.drorbiteex.fxml.canvas;

import javafx.scene.canvas.Canvas;

public class ResizableCanvas extends Canvas {
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double maxHeight(double width) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double maxWidth(double height) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double minWidth(double height) {
        return 1.0;
    }

    @Override
    public double minHeight(double width) {
        return 1.0;
    }

    @Override
    public void resize(double width, double height) {
        double size = Math.min(width, height);
        setWidth(size);
        setHeight(size);
    }
}
