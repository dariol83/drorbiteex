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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BackgroundThread {

    private static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor((e) -> {
        Thread t = new Thread(e);
        t.setDaemon(true);
        t.setName("Dr Orbiteex - Model Manager Thread");
        return t;
    });

    public static ExecutorService getExecutor() {
        return THREAD_EXECUTOR;
    }

    public static void runLater(Runnable r) {
        THREAD_EXECUTOR.submit(r);
    }

    public static void shutdown() throws InterruptedException {
        THREAD_EXECUTOR.shutdown();
        THREAD_EXECUTOR.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }
}
