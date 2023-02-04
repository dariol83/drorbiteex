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

import eu.dariolucia.drorbiteex.model.collinearity.SkyCoverageAnalysisRequest;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.ResourceBundle;

public class SkyCoverageReportDialog implements Initializable {

    public TextField elevationText;
    public TextField azimuthText;
    public Label periodLabel;
    private SkyCoverageAnalysisRequest request;

    public ImageView image;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Menu for image copy
        image.setOnContextMenuRequested(e -> {
            ContextMenu m = new ContextMenu();
            final MenuItem copyItem = new MenuItem("Copy image to clipboard");
            copyItem.setOnAction(event -> {
                WritableImage theImage = image.snapshot(null, null);
                ClipboardContent content = new ClipboardContent();
                content.putImage(theImage);
                Clipboard.getSystemClipboard().setContent(content);
            });
            m.getItems().add(copyItem);
            m.show(image.getScene().getWindow(), e.getScreenX(), e.getScreenY());
        });
    }

    public static void openDialog(Window owner, SkyCoverageAnalysisRequest request, Canvas plot) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Sky coverage result for " + request.getGroundStation().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = SkyCoverageReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/SkyCoverageReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            SkyCoverageReportDialog controller = loader.getController();
            controller.initialise(request, plot);

            d.getDialogPane().setContent(root);
            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialise(SkyCoverageAnalysisRequest request, Canvas plot) {
        this.request = request;
        this.image.setPreserveRatio(true);
        this.image.setFitWidth(-1);
        this.image.setFitHeight(-1);
        this.image.setImage(plot.snapshot(null, null));

        this.periodLabel.setText(TimeUtils.formatDate(request.getStartTime()) + " - " + TimeUtils.formatDate(request.getEndTime()));

        this.image.setOnMouseMoved(this::mouseMoved);
    }

    private void mouseMoved(MouseEvent e) {
        Point2D elAzCoordinates = PolarPlotPainter.toPolarPoint(e.getX(), e.getY(), image.getImage().getWidth(), image.getImage().getHeight());
        if(elAzCoordinates != null && elAzCoordinates.getX() > 0) {
            this.elevationText.setText(String.format("%.4f", elAzCoordinates.getX()));
            this.azimuthText.setText(String.format("%.4f", elAzCoordinates.getY()));
        } else {
            this.elevationText.setText("");
            this.azimuthText.setText("");
        }
    }
}
