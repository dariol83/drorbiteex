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

import eu.dariolucia.drorbiteex.fxml.progress.IMonitorableCallable;
import eu.dariolucia.drorbiteex.fxml.progress.ProgressDialog;
import eu.dariolucia.drorbiteex.model.collinearity.ErrorPoint;
import eu.dariolucia.drorbiteex.model.determination.NumericalOrbitDeterminationRequest;
import eu.dariolucia.drorbiteex.model.determination.NumericalOrbitDeterminationResult;
import eu.dariolucia.drorbiteex.model.oem.DefaultGenerator;
import eu.dariolucia.drorbiteex.model.oem.DefaultPostProcessor;
import eu.dariolucia.drorbiteex.model.oem.OemExporterProcess;
import eu.dariolucia.drorbiteex.model.oem.OemGenerationRequest;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.ResourceBundle;

public class NumericalOrbitDeterminationReportDialog implements Initializable {

    public Label textLabel;
    public VBox chartParent;
    public Button exportButton;
    private OrbitPane orbitPane;
    private Orbit orbit;

    public DatePicker startDatePicker;
    public TextField startTimeText;
    public DatePicker endDatePicker;
    public TextField endTimeText;
    public TextField periodText;

    private final BooleanProperty validData = new SimpleBooleanProperty(false);

    private String error;

    private ChartManager residualChartManager;
    private Propagator propagator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        startTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        endDatePicker.valueProperty().addListener((prop, oldVal, newVal) -> validate());
        endTimeText.textProperty().addListener((prop, oldVal, newVal) -> validate());
        periodText.setText(String.valueOf(60));

        exportButton.disableProperty().bind(validData.not());

        validate();
    }

    public static void openDialog(Window owner, NumericalOrbitDeterminationResult result, NumericalOrbitDeterminationRequest request, OrbitPane orbitPane) {
        try {
            // Create the popup
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("TLE orbit determination result for orbit " + request.getOrbit().getName());
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            URL dataSelectionDialogFxmlUrl = NumericalOrbitDeterminationReportDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/NumericalOrbitDeterminationReportDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            NumericalOrbitDeterminationReportDialog controller = loader.getController();
            controller.configure(result, request, orbitPane);

            d.getDialogPane().setContent(root);
            d.getDialogPane().getStylesheets().addAll(root.getStylesheets());

            d.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validate() {
        try {
            if(startDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("Start date field is blank");
            }
            if(startTimeText.getText().isBlank()) {
                throw new IllegalStateException("Start time field is blank");
            }
            if(endDatePicker.valueProperty().isNull().get()) {
                throw new IllegalStateException("End date field is blank");
            }
            if(endTimeText.getText().isBlank()) {
                throw new IllegalStateException("End time field is blank");
            }

            DialogUtils.getDate(startDatePicker, startTimeText);
            DialogUtils.getDate(endDatePicker, endTimeText);

            Integer.parseInt(periodText.getText());

            error = null;
            validData.setValue(true);
        } catch (Exception e) {
            error = e.getMessage();
            validData.setValue(false);
        }
    }

    private void configure(NumericalOrbitDeterminationResult result, NumericalOrbitDeterminationRequest request, OrbitPane orbitPane) {
        this.textLabel.setText("Numerical orbit determination for Orbit " + request.getOrbit().getName());
        this.propagator = result.getEstimatedPropagator();
        this.orbit = request.getOrbit();
        this.orbitPane = orbitPane;

        Date initialDate = TimeUtils.toDate(this.propagator.getInitialState().getDate());
        this.startDatePicker.setValue(DialogUtils.toDateText(initialDate));
        this.startTimeText.setText(DialogUtils.toTimeText(initialDate));
        this.endDatePicker.setValue(DialogUtils.toDateText(new Date(initialDate.getTime() + 14 * 24 * 3600000L)));
        this.endTimeText.setText(DialogUtils.toTimeText(new Date(initialDate.getTime() + 14 * 24 * 3600000L)));

        // Create the chart managers based on the dataset labels
        this.residualChartManager = new ChartManager("Residual Error", chartParent, new NumberToDateAxisFormatter());

        // Get min-max time range
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        // You have one value for the error
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Residual Error");
        for (ErrorPoint tp : result.getResiduals()) {
            series.getData().add(new XYChart.Data<>(tp.getTime().toEpochMilli(), tp.getErrorAt(2)));
            if(tp.getTime().toEpochMilli() < minTime) {
                minTime = tp.getTime().toEpochMilli();
            }
            if(tp.getTime().toEpochMilli() > maxTime) {
                maxTime = tp.getTime().toEpochMilli();
            }
        }
        this.residualChartManager.add(series);
        this.residualChartManager.updateChartRange(minTime, maxTime);
        this.residualChartManager.setLegendVisible(false);

        validate();
    }

    public void onExportOemAction(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save OEM File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files","*.*"));
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("OEM file","*.xml", "*.oem", "*.txt"));

        File selected = fc.showSaveDialog(textLabel.getScene().getWindow());
        if(selected != null) {
            String filePath = selected.getAbsolutePath();
            try {
                Date start = DialogUtils.getDate(startDatePicker, startTimeText);
                Date end = DialogUtils.getDate(endDatePicker, endTimeText);
                exportOemOrbit(propagator, filePath, start, end);
            } catch (ParseException e) {
                e.printStackTrace();
                DialogUtils.alert("OEM Export", "Cannot parse date",
                        "Error: " + e.getMessage());
            }
        }
    }

    public void exportOemOrbit(Propagator p, String file, Date start, Date end) {
        String taskName = "OEM Export";
        OemGenerationRequest oemGenerationRequest = new OemGenerationRequest(p,
                orbit.getCode(), orbit.getName(),
                start, end, Integer.parseInt(periodText.getText()), file,
                FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                FileFormat.XML,
                null,
                null,
                null);
        IMonitorableCallable<String> task = monitor -> {
            ITaskProgressMonitor monitorBridge = new ITaskProgressMonitor() {
                @Override
                public void progress(long current, long total, String message) {
                    monitor.progress(taskName, current, total, message);
                }

                @Override
                public boolean isCancelled() {
                    return monitor.isCancelled();
                }
            };
            try {
                return new OemExporterProcess().exportOem(oemGenerationRequest, monitorBridge);
            } catch (Exception e) {
                // e.printStackTrace();
                throw e;
            }
        };
        ProgressDialog.Result<String> taskResult = ProgressDialog.openProgress(textLabel.getScene().getWindow(), taskName, task);
        if(taskResult.getStatus() == ProgressDialog.TaskStatus.COMPLETED) {
            DialogUtils.info("OEM Export", "Orbit of " + orbit.getName() + " exported", "OEM file: " + taskResult.getResult());
        } else if(taskResult.getStatus() == ProgressDialog.TaskStatus.CANCELLED) {
            DialogUtils.alert(taskName, "OEM computation for " + orbit.getName(),
                    "Task cancelled by user");
        } else {
            DialogUtils.alert(taskName, "OEM computation for " + orbit.getName(),
                    "Error: " + taskResult.getError().getMessage());
        }
    }
}
