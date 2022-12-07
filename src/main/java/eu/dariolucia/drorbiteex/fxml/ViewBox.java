/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

import javafx.geometry.Point2D;

public class ViewBox {
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    public ViewBox(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void update(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void update(ViewBox extendedViewport) {
        this.startX = extendedViewport.startX;
        this.startY = extendedViewport.startY;
        this.endX = extendedViewport.endX;
        this.endY = extendedViewport.endY;
    }

    public double getStartX() {
        return startX;
    }

    public void setStartX(double startX) {
        this.startX = startX;
    }

    public double getStartY() {
        return startY;
    }

    public void setStartY(double startY) {
        this.startY = startY;
    }

    public double getEndX() {
        return endX;
    }

    public void setEndX(double endX) {
        this.endX = endX;
    }

    public double getEndY() {
        return endY;
    }

    public void setEndY(double endY) {
        this.endY = endY;
    }

    public double getWidth() {
        return Math.abs(this.endX - this.startX);
    }

    public double getHeight() {
        return Math.abs(this.endY - this.startY);
    }

    public void move(double dx, double dy) {
        this.startX += dx;
        this.endX += dx;
        this.startY += dy;
        this.endY += dy;
    }

    public boolean contains(double px, double py) {
        return px >= startX && px <= endX && py >= startY && py <= endY;
    }

    @Override
    public String toString() {
        return "ViewBox{" +
                "startX=" + startX +
                ", startY=" + startY +
                ", endX=" + endX +
                ", endY=" + endY +
                '}';
    }

    public double getRatio() {
        return getWidth()/getHeight();
    }

}
