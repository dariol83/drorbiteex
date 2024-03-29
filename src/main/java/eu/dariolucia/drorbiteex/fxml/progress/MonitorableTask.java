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

import java.util.concurrent.Callable;

public class MonitorableTask<V> implements Callable<V> {

    private final IMonitorableCallable<V> task;

    private volatile IProgressMonitor monitor;
    private volatile Exception detectedError;

    public MonitorableTask(IMonitorableCallable<V> task) {
        this.task = task;
    }

    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public Exception getDetectedError() {
        return detectedError;
    }

    @Override
    public V call() throws Exception {
        try {
            return task.run(this.monitor);
        } catch (Exception e) {
            this.detectedError = e;
            throw e;
        } finally {
            if(this.monitor != null) {
                this.monitor.completed();
            }
        }
    }
}
