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

package eu.dariolucia.drorbiteex.fxml.progress;

import eu.dariolucia.drorbiteex.fxml.ExportScheduleDialog;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ProgressDialog implements Initializable, IProgressMonitor {
    public Label messageText;
    public ProgressBar progress;
    public Label ectText;
    private volatile InterruptibleCallable task;

    private volatile boolean wasInterrupted = false;
    private volatile Stage stage;

    private volatile Date startTime;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing
    }

    public void onCancelButtonAction(ActionEvent actionEvent) {
        this.wasInterrupted = true;
        if(task != null) {
            task.cancel();
            stage.close(); // unblock the showAndWait call
        }
    }

    private <V> Future<V> runTask(InterruptibleCallable<V> task, Stage stage) {
        this.stage = stage;
        this.task = task;
        task.monitor(this);
        ExecutorService service = Executors.newSingleThreadExecutor((r) -> {
            Thread t = new Thread(r, "Progress Task");
            t.setDaemon(true);
            return t;
        });
        this.startTime = new Date();
        Future<V> future = service.submit(task);
        service.shutdown();
        return future;
    }

    public static <V> V openProgress(Window owner, String name, InterruptibleCallable<V> task) {
        try {
            // Create the popup
            Stage d = new Stage();
            d.setTitle(name);
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.initStyle(StageStyle.UTILITY);

            URL dataSelectionDialogFxmlUrl = ExportScheduleDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/progress/ProgressDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            d.setScene(new Scene(root));

            ProgressDialog controller = loader.getController();
            Future<V> future = controller.runTask(task, d);
            d.setOnCloseRequest(e -> {
                controller.onCancelButtonAction(null);
                e.consume();
            });
            d.showAndWait();

            V value = future.get();
            if(controller.wasInterrupted) {
                return null;
            } else {
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void progress(String name, long current, long total, String message) {
        if(this.wasInterrupted) {
            return;
        }
        Date timestamp = new Date();
        Platform.runLater(() -> {
            this.messageText.setText(message);
            this.progress.setProgress(current/(double) total);
            if(current > 0) {
                // Compute ECT
                long secs = computeECT(timestamp, current, total);
                long hours = secs / 3600;
                long minutes = (secs % 3600) / 60;
                long seconds = secs % 60;
                StringBuilder sb = new StringBuilder("ECT: ");
                if (hours > 0) {
                    sb.append(hours).append(" hours, ");
                    sb.append(minutes).append(" minutes, ");
                    sb.append(seconds).append(" seconds");
                } else if (minutes > 0) {
                    sb.append(minutes).append(" minutes, ");
                    sb.append(seconds).append(" seconds");
                } else if (seconds > 5) {
                    sb.append(seconds).append(" seconds");
                } else {
                    sb.append("almost done");
                }
                this.ectText.setText(sb.toString());

                if(current == total) {
                    stage.close(); // Unblock showAndWait
                }
            }
        });
    }

    private long computeECT(Date timestamp, long current, long total) {
        // Compute the difference between start time and timestamp
        long msDiff = timestamp.getTime() - startTime.getTime();
        // if in msDiff we progressed current, we need still (msDiff / current) * (total - current) ms
        double estimated = ((double) msDiff/ (double) current) * (total - current);
        return Math.round(estimated) / 1000;
    }

    public interface InterruptibleCallable<V> extends Callable<V> {
        void monitor(IProgressMonitor monitor);

        void cancel();
    }
}
