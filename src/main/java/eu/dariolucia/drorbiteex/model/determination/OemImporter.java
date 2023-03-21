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

package eu.dariolucia.drorbiteex.model.determination;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import javafx.geometry.Pos;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.oem.OemMetadata;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemSegment;
import org.orekit.files.ccsds.ndm.tdm.*;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OemImporter {
    public static List<Measurement> load(String absolutePath, Orbit satellite) {
        Oem oemObject = new ParserBuilder().buildOemParser().parseMessage(new DataSource("oem", () -> new FileInputStream(absolutePath)));
        return oemObject.getSegments().stream().filter(seg -> seg.getMetadata().getObjectID().equals(satellite.getCode()))
                .map(seg -> seg.getCoordinates().stream().map(o ->
                                new PositionMeasurement(TimeUtils.toInstant(o.getDate()), o.getPosition(), o.getVelocity(), seg.getFrame()))
                .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
