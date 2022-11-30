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

package eu.dariolucia.drorbiteex.model.oem;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.frames.Frame;

import java.util.Date;

public class OemGenerationRequest {
    private final String code;
    private final String name;
    private final Date startTime;
    private final Date endTime;
    private final int periodSeconds;
    private final String file;

    private final Frame frame;

    private final FileFormat format;

    private final String folder;

    private final String nameGenerator;

    private final String postProcessor;

    private final Orbit orbit;

    public OemGenerationRequest(Orbit orbit, String code, String name, Date startTime, Date endTime, int periodSeconds, String file, Frame frame, FileFormat format, String folder, String nameGenerator, String postProcessor) {
        this.orbit = orbit;
        this.code = code;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodSeconds = periodSeconds;
        this.file = file;
        this.frame = frame;
        this.format = format;
        this.folder = folder;
        this.nameGenerator = nameGenerator;
        this.postProcessor = postProcessor;
    }

    public Orbit getOrbit() {
        return orbit;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getPeriodSeconds() {
        return periodSeconds;
    }

    public String getFile() {
        return file;
    }

    public FileFormat getFormat() {
        return format;
    }

    public Frame getFrame() {
        return frame;
    }

    public String getFolder() {
        return folder;
    }

    public String getNameGenerator() {
        return nameGenerator;
    }

    public String getPostProcessor() {
        return postProcessor;
    }
}
