package eu.dariolucia.drorbiteex.model.station;

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

    private List<GroundStation> groundStations;

    @XmlElement
    public List<GroundStation> getGroundStations() {
        return groundStations;
    }

    public void setGroundStations(List<GroundStation> groundStations) {
        this.groundStations = groundStations;
    }
}
