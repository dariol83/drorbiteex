package eu.dariolucia.drorbiteex.model.orbit;

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

@XmlRootElement(name = "orbit-config", namespace = "http://dariolucia.eu/drorbiteex/orbit")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class OrbitConfiguration {

    public static OrbitConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(OrbitConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (OrbitConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public static void save(OrbitConfiguration d, OutputStream out) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(OrbitConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(d, out);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    private List<Orbit> orbits;

    @XmlElement
    public List<Orbit> getOrbits() {
        return orbits;
    }

    public void setOrbits(List<Orbit> orbits) {
        this.orbits = orbits;
    }
}
