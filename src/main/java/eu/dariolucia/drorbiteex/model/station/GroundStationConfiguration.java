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

package eu.dariolucia.drorbiteex.model.station;

import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "ground-station-config", namespace = "http://dariolucia.eu/drorbiteex/groundstation")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroundStationConfiguration {

    public static GroundStationConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(GroundStationConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (GroundStationConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public static void save(GroundStationConfiguration d, OutputStream out) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(GroundStationConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(d, out);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    private List<GroundStation> groundStations = new LinkedList<>();

    private GroundStationParameterConfiguration configuration = new GroundStationParameterConfiguration();

    @XmlElement(name = "configuration")
    public GroundStationParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(GroundStationParameterConfiguration configuration) {
        this.configuration = configuration;
    }


    @XmlElement(name = "ground-station")
    public List<GroundStation> getGroundStations() {
        return groundStations;
    }

    public void setGroundStations(List<GroundStation> groundStations) {
        this.groundStations = groundStations;
    }
}
