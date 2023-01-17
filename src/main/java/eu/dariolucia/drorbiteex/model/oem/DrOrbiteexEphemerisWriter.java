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

package eu.dariolucia.drorbiteex.model.oem;

import org.orekit.files.ccsds.ndm.odm.oem.EphemerisWriter;
import org.orekit.files.ccsds.ndm.odm.oem.OemMetadata;
import org.orekit.files.ccsds.ndm.odm.oem.OemWriter;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.general.EphemerisFile;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;

public class DrOrbiteexEphemerisWriter extends EphemerisWriter {
    /**
     * Super constructor (inherited)
     *
     * @param writer      underlying writer
     * @param header      file header (may be null)
     * @param template    template for metadata
     * @param fileFormat  file format to use
     * @param outputName  output name for error messages
     * @param unitsColumn columns number for aligning units (if negative or zero, units are not output)
     */
    public DrOrbiteexEphemerisWriter(OemWriter writer, Header header, OemMetadata template, FileFormat fileFormat, String outputName, int unitsColumn) {
        super(writer, header, template, fileFormat, outputName, unitsColumn);
    }

    @Override
    public <C extends TimeStampedPVCoordinates, S extends EphemerisFile.EphemerisSegment<C>> void writeSegment(Generator generator, S segment) throws IOException {
        // Open the segment section
        generator.writeRawData("   <segment>");
        super.writeSegment(generator, segment);
        // Close the segment section
        generator.writeRawData("   </segment>\n");
    }
}
