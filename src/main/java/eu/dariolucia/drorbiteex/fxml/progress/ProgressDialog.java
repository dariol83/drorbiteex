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

import eu.dariolucia.drorbiteex.fxml.CssHolder;
import eu.dariolucia.drorbiteex.fxml.ExportScheduleDialog;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class ProgressDialog implements Initializable, IProgressMonitor {
    public Label messageText;
    public ProgressBar progress;
    public Label ectText;
    private volatile MonitorableTask task;

    private volatile boolean wasInterrupted = false;

    private volatile Exception detectedError;
    private volatile Stage stage;
    private volatile Date startTime;
    private volatile Future<?> futureTask;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing
    }

    public void onCancelButtonAction(ActionEvent actionEvent) {
        if(!this.wasInterrupted) {
            ((Button) actionEvent.getSource()).setDisable(true);
            this.wasInterrupted = true;
            // Someone must really close the stage
            new Thread(() -> {
                Future<?> f = futureTask;
                if (f != null) {
                    try {
                        f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> {
                    if(stage != null) {
                        stage.close();
                    }
                });
            }).start();
        }
    }

    private <V> Future<V> runTask(IMonitorableCallable<V> task, Stage stage, ExecutorService executorService) {
        this.stage = stage;
        this.task = new MonitorableTask<>(task);
        this.task.setMonitor(this);
        this.startTime = new Date();
        boolean shutdownAfterSubmit = executorService == null;
        if(executorService == null) {
            executorService = Executors.newSingleThreadExecutor((r) -> {
                Thread t = new Thread(r, "Progress Task");
                t.setDaemon(true);
                return t;
            });
        }
        futureTask = executorService.submit(this.task);
        if(shutdownAfterSubmit) {
            executorService.shutdown();
        }
        return (Future<V>) futureTask;
    }

    public static <V> Result<V> openProgress(Window owner, String name, IMonitorableCallable<V> task) {
        return openProgress(owner, name, task, null);
    }

    public static <V> Result<V> openProgress(Window owner, String name, IMonitorableCallable<V> task, ExecutorService executorService) {
        try {
            // Create the popup
            Stage d = new Stage();
            d.setTitle(name);
            d.initModality(Modality.APPLICATION_MODAL);
            d.initOwner(owner);
            d.setResizable(false);

            URL dataSelectionDialogFxmlUrl = ExportScheduleDialog.class.getResource("/eu/dariolucia/drorbiteex/fxml/progress/ProgressDialog.fxml");
            FXMLLoader loader = new FXMLLoader(dataSelectionDialogFxmlUrl);
            AnchorPane root = loader.load();
            CssHolder.applyTo(root);
            d.setScene(new Scene(root));

            ProgressDialog controller = loader.getController();
            Future<V> future = controller.runTask(task, d, executorService);
            d.setOnCloseRequest(e -> {
                controller.onCancelButtonAction(null);
                e.consume();
            });
            d.showAndWait();

            V value = future.get();
            if (controller.wasInterrupted) {
                return new Result<>(TaskStatus.CANCELLED, null, null);
            } else if (controller.task != null && controller.task.getDetectedError() != null) {
                return new Result<>(TaskStatus.ERROR, controller.task.getDetectedError(), null);
            } else {
                return new Result<>(TaskStatus.COMPLETED, null, value);
            }
        } catch (CancellationException e) {
            return new Result<>(TaskStatus.CANCELLED, null, null);
        } catch (ExecutionException e) {
            return new Result<>(TaskStatus.ERROR, e.getCause(), null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result<>(TaskStatus.ERROR, e, null);
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
            if(current >= 0) {
                this.progress.setProgress(current/(double) total);
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
                } else if (seconds > 0) {
                    sb.append(seconds).append(" seconds");
                } else {
                    sb.append("almost done");
                }
                this.ectText.setText(sb.toString());

                if(current == total && stage != null) {
                    stage.close(); // Unblock showAndWait
                    stage = null;
                }
            } else if(current == -1) {
                this.progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            }
        });
    }

    @Override
    public boolean isCancelled() {
        return this.wasInterrupted;
    }

    @Override
    public void completed() {
        Platform.runLater(() -> {
            if(stage != null) {
                stage.close(); // Unblock showAndWait
                stage = null;
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

    public static class Result<V> {
        private final TaskStatus status;
        private final Throwable error;
        private final V result;

        public Result(TaskStatus status, Throwable error, V result) {
            this.status = status;
            this.error = error;
            this.result = result;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public Throwable getError() {
            return error;
        }

        public V getResult() {
            return result;
        }
    }

    public enum TaskStatus {
        COMPLETED,
        CANCELLED,
        ERROR
    }
}
