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

package eu.dariolucia.drorbiteex.fxml.range;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class RangeSelector implements Initializable {

    private final SimpleLongProperty lowerBound = new SimpleLongProperty(0);

    private final SimpleLongProperty upperBound = new SimpleLongProperty(100);
    
    private final SimpleLongProperty currentLowerBound = new SimpleLongProperty(0);
    
    private final SimpleLongProperty currentUpperBound = new SimpleLongProperty(100);

    private final SimpleObjectProperty<Pair<Long, Long>> range = new SimpleObjectProperty<>(new Pair<>(0L, 100L));
    public Separator minSeparator;
    public Region centerRegion;
    public Separator maxSeparator;
    public ToolBar toolbar;
    public Region leftRegion;
    public Region rightRegion;
    public Label minLabel;
    public Label maxLabel;

    private StringConverter<Number> formatter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lowerBound.addListener((w,o,n) -> updateRange());
        upperBound.addListener((w,o,n) -> updateRange());
        currentLowerBound.addListener((w,o,n) -> updateRange());
        currentUpperBound.addListener((w,o,n) -> updateRange());

        centerRegion.setBackground(new Background(new BackgroundFill(Color.LIME, null, null)));
        leftRegion.setBackground(new Background(new BackgroundFill(Color.DARKSEAGREEN, null, null)));
        rightRegion.setBackground(new Background(new BackgroundFill(Color.DARKSEAGREEN, null, null)));

        Platform.runLater(this::updateRange);
    }

    public void setLabelFormatter(StringConverter<Number> labelFormatter) {
        this.formatter = labelFormatter;
    }

    public long getLowerBound() {
        return lowerBound.get();
    }

    public SimpleLongProperty lowerBoundProperty() {
        return lowerBound;
    }

    public long getUpperBound() {
        return upperBound.get();
    }

    public SimpleLongProperty upperBoundProperty() {
        return upperBound;
    }

    public long getCurrentLowerBound() {
        return currentLowerBound.get();
    }

    public SimpleLongProperty currentLowerBoundProperty() {
        return currentLowerBound;
    }

    public long getCurrentUpperBound() {
        return currentUpperBound.get();
    }

    public SimpleLongProperty currentUpperBoundProperty() {
        return currentUpperBound;
    }

    public Pair<Long, Long> getRange() {
        return range.get();
    }

    public ReadOnlyObjectProperty<Pair<Long, Long>> rangeProperty() {
        return range;
    }

    private void updateRange() {
        double valuePxRes = getValuePxRes();
        // now we can compute the size of all the regions
        // left region: currentLowerBound - lowerBound / valuePxRes
        setSize(leftRegion, (currentLowerBound.get() - lowerBound.get())/valuePxRes);
        // right region: upperBound - currentUpperBound / valuePxRes
        setSize(rightRegion, (upperBound.get() - currentUpperBound.get())/valuePxRes);
        // center region: currentUpperBound - currentLowerBound / valuePxRes
        setSize(centerRegion, (currentUpperBound.get() - currentLowerBound.get())/valuePxRes);
        // set min value
        minLabel.setText(this.formatter != null ? this.formatter.toString(currentLowerBound.get()) : String.valueOf(currentLowerBound.get()));
        // set max value
        maxLabel.setText(this.formatter != null ? this.formatter.toString(currentUpperBound.get()) : String.valueOf(currentUpperBound.get()));
        // update range
        range.set(new Pair<>(currentLowerBound.get(), currentUpperBound.get()));
    }

    private double getValuePxRes() {
        // get the widget width
        double width = toolbar.getWidth() - minSeparator.getWidth()*20;
        // get the difference between upper and lower
        long upperLowerDiff = upperBound.get() - lowerBound.get();
        // compute the value/pixel resolution
        return upperLowerDiff/width;
    }

    private void setSize(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private Separator separatorUnderDragging;
    private long startValue;
    private double startX;

    private long startMinValue;
    private long startMaxValue;

    public void onSeparatorDragPressed(MouseEvent mouseEvent) {
        separatorUnderDragging = (Separator) mouseEvent.getSource();
        startX = mouseEvent.getScreenX();
        startValue = separatorUnderDragging == minSeparator ? currentLowerBound.longValue() : currentUpperBound.longValue();
    }

    public void onSeparatorDragReleased(MouseEvent mouseEvent) {
        separatorUnderDragging = null;
        startX = -1;
        startValue = Long.MAX_VALUE;
    }

    public void onSeparatorDragged(MouseEvent mouseEvent) {
        double theX = mouseEvent.getScreenX();
        // left or right? amount?
        double movement = theX - startX;
        double amount = movement * getValuePxRes();
        long newValue = (long) (startValue + amount);
        // set value
        if(separatorUnderDragging == minSeparator) {
            if(newValue < currentUpperBound.get() && newValue >= lowerBound.get()) {
                currentLowerBound.set(newValue);
            } else if(newValue < lowerBound.get()) {
                currentLowerBound.set(lowerBound.get());
            }
        } else if(separatorUnderDragging == maxSeparator) {
            if(newValue > currentLowerBound.get() && newValue <= upperBound.get()) {
                currentUpperBound.set(newValue);
            } else if(newValue > upperBound.get()) {
                currentUpperBound.set(upperBound.get());
            }
        }
    }

    public void onRegionDragPressed(MouseEvent mouseEvent) {
        startX = mouseEvent.getScreenX();
        startMinValue = currentLowerBound.get();
        startMaxValue = currentUpperBound.get();
    }

    public void onRegionDragReleased(MouseEvent mouseEvent) {
        startX = -1;
        startMinValue = Long.MIN_VALUE;
        startMaxValue = Long.MAX_VALUE;
    }

    public void onRegionDragged(MouseEvent mouseEvent) {
        double theX = mouseEvent.getScreenX();
        // left or right? amount?
        double movement = theX - startX;
        double amount = movement * getValuePxRes();
        // Move the boundaries by the same amount
        long newCurrentMinBound = Math.round(startMinValue + amount);
        long newCurrentMaxBound = Math.round(startMaxValue + amount);
        if(newCurrentMinBound >= lowerBound.get() && newCurrentMaxBound <= upperBound.get()) {
            currentLowerBound.set(newCurrentMinBound);
            currentUpperBound.set(newCurrentMaxBound);
        }
    }

    public void setBounds(long minBound, long maxBound) {
        lowerBound.set(minBound);
        upperBound.set(maxBound);
        currentLowerBound.set(minBound);
        currentUpperBound.set(maxBound);
    }
}
