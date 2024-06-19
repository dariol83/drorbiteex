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

package eu.dariolucia.drorbiteex.model.orbit;

import javafx.beans.property.SimpleBooleanProperty;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CelestrakTleData {

    private static final String CELESTRAK_PATH = "https://celestrak.org/NORAD/elements/gp.php?GROUP=<group>&FORMAT=tle";

    public static final String[] CELESTRAK_GROUPS = new String[] {"last-30-days", "weather", "dmc", "sarsar", "noaa", "resource", "gps-ops", "galileo", "geo", "cubesat", "active"};

    private final String name;
    private final String group;
    private final String tle;
    private SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public CelestrakTleData(String name, String group, String tle) {
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

    public static List<CelestrakTleData> retrieveSpacecraftList(String group) {
        try {
            URL url = new URL(CELESTRAK_PATH.replace("<group>", group));
            URLConnection conn = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            List<CelestrakTleData> list = extractData(group, br);
            br.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<CelestrakTleData> extractData(String group, BufferedReader br) throws IOException {
        List<CelestrakTleData> list = new LinkedList<>();
        String read;
        String satelliteId = null;
        String tle1 = null;
        String tle2 = null;
        int state = 0;
        while((read = br.readLine()) != null) {
            if(read.isBlank()) {
                continue;
            }
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
                    list.add(new CelestrakTleData(satelliteId, group, tle1 + "\n" + tle2));
                    break;
            }
        }
        return list;
    }

    public static List<CelestrakTleData> processCelestrakFile(String group, String file) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            List<CelestrakTleData> list = extractData(group, br);
            br.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String retrieveUpdatedTle(String group, String name) {
        List<CelestrakTleData> sats = retrieveSpacecraftList(group);
        if(sats != null) {
            return sats.stream().filter(o -> o.getName().equals(name)).map(CelestrakTleData::getTle).findFirst().orElse(null);
        } else {
            return null;
        }
    }
}
