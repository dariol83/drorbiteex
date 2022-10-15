package eu.dariolucia.drorbiteex.data;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@XmlRootElement(name = "drorbiteex", namespace = "http://dariolucia.eu/drorbiteex")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Configuration {

    public static Configuration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(Configuration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (Configuration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public static void save(Configuration d, OutputStream out) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(Configuration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(d, out);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    private List<GroundStation> groundStations;

    @XmlElement(name = "ground-station")
    public List<GroundStation> getGroundStations() {
        return groundStations;
    }

    public void setGroundStations(List<GroundStation> groundStations) {
        this.groundStations = groundStations;
    }

    private List<AbstractOrbit> orbits;

    @XmlElements({
        @XmlElement(name="tle-orbit",type=TleOrbit.class),
        @XmlElement(name="tle-celestrak-orbit",type=CelestrakTleOrbit.class)
    })
    public List<AbstractOrbit> getOrbits() {
        return orbits;
    }

    public void setOrbits(List<AbstractOrbit> orbits) {
        this.orbits = orbits;
    }
}
