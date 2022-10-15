package eu.dariolucia.drorbiteex.data;

import javafx.beans.property.SimpleBooleanProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

public class CelestrakSatellite {

    private static final String CELESTRAK_PATH = "https://celestrak.org/NORAD/elements/gp.php?GROUP=<group>&FORMAT=tle";

    private final String name;
    private final String group;
    private final String tle;
    private SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public CelestrakSatellite(String name, String group, String tle) {
        this.name = name;
        this.group = group;
        this.tle = tle;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getTle() {
        return tle;
    }

    public SimpleBooleanProperty selectedProperty() {
        return this.selectedProperty;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static List<CelestrakSatellite> retrieveSpacecraftList(String group) {
        try {
            List<CelestrakSatellite> list = new LinkedList<>();
            URL url = new URL(CELESTRAK_PATH.replace("<group>", group));
            URLConnection conn = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String read;
            String satelliteId = null;
            String tle1 = null;
            String tle2 = null;
            int state = 0;
            while((read = br.readLine()) != null) {
                switch (state) {
                    case 0:
                        satelliteId = read.trim();
                        state = 1;
                        break;
                    case 1:
                        tle1 = read.trim();
                        state = 2;
                        break;
                    case 2:
                        tle2 = read.trim();
                        state = 0;
                        list.add(new CelestrakSatellite(satelliteId, group, tle1 + "\n" + tle2));
                        break;
                }
            }
            br.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String retrieveUpdatedTle(String group, String name) {
        // TODO: connect to https://celestrak.org/NORAD/elements/gp.php?GROUP=<group>&FORMAT=tle
        // TODO: parse the result and look for satellite name == name
        return null;
    }
}
