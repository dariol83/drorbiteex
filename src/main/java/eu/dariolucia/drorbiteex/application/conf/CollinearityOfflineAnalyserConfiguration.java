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

package eu.dariolucia.drorbiteex.application.conf;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class CollinearityOfflineAnalyserConfiguration {

    public static final String GROUND_STATION_NAME_KEY = "ground.station.name";
    public static final String GROUND_STATION_LAT_KEY = "ground.station.lat";
    public static final String GROUND_STATION_LON_KEY = "ground.station.lon";
    public static final String GROUND_STATION_ALT_KEY = "ground.station.altitude";
    public static final String REF_TLE_FILE_PATH_KEY = "reference.tle.file.path";
    public static final String TARGET_TLE_FILE_PATH_KEY = "target.tle.file.path";
    public static final String OUTPUT_FOLDER_PATH_KEY = "output.folder.path";
    public static final String NAME_EXCLUSIONS_PREFIX = "name.exclusion.";
    public static final String MAX_HEIGHT_KEY = "max.height.exclusion";
    public static final String MIN_HEIGHT_KEY = "min.height.exclusion";
    public static final String MAX_ANGULAR_SEPARATION_KEY = "max.angular.separation";
    public static final String SAMPLING_KEY = "sampling";
    public static final String INTERVAL_KEY = "interval";
    public static final String CORES_KEY = "cores";

    private String gsName;
    private double gsLat;
    private double gsLon;
    private double gsAltitude;
    private String referenceTleOrbitFilePath;
    private String targetTleOrbitFilePath;
    private String outputFolder;
    private List<String> nameExclusions = null;
    private int maxHeight = Integer.MAX_VALUE;
    private int minHeight = 0;
    private double maxAngularSeparation = 10.0;
    private int sampling = 1;
    private int intervalPeriod = 20;
    private int cores = Math.min(1, Runtime.getRuntime().availableProcessors());

    public CollinearityOfflineAnalyserConfiguration(Properties props) {
        for(Object key : props.keySet()) {
            String theKey = key.toString();
            switch (theKey) {
                case GROUND_STATION_NAME_KEY:
                    gsName = props.getProperty(theKey);
                    break;
                case GROUND_STATION_LAT_KEY:
                    gsLat = Double.parseDouble(props.getProperty(theKey));
                    break;
                case GROUND_STATION_LON_KEY:
                    gsLon = Double.parseDouble(props.getProperty(theKey));
                    break;
                case GROUND_STATION_ALT_KEY:
                    gsAltitude = Double.parseDouble(props.getProperty(theKey));
                    break;
                case REF_TLE_FILE_PATH_KEY:
                    referenceTleOrbitFilePath = props.getProperty(theKey);
                    break;
                case TARGET_TLE_FILE_PATH_KEY:
                    targetTleOrbitFilePath = props.getProperty(theKey);
                    break;
                case OUTPUT_FOLDER_PATH_KEY:
                    outputFolder = props.getProperty(theKey);
                    break;
                case MAX_HEIGHT_KEY:
                    maxHeight = Integer.parseInt(props.getProperty(theKey));
                    break;
                case MIN_HEIGHT_KEY:
                    minHeight = Integer.parseInt(props.getProperty(theKey));
                    break;
                case MAX_ANGULAR_SEPARATION_KEY:
                    maxAngularSeparation = Double.parseDouble(props.getProperty(theKey));
                    break;
                case SAMPLING_KEY:
                    sampling = Integer.parseInt(props.getProperty(theKey));
                    break;
                case INTERVAL_KEY:
                    intervalPeriod = Integer.parseInt(props.getProperty(theKey));
                    break;
                case CORES_KEY:
                    cores = Integer.parseInt(props.getProperty(theKey));
                    break;
                default: {
                    if(theKey.startsWith(NAME_EXCLUSIONS_PREFIX)) {
                        String toExclude = props.getProperty(theKey);
                        if(nameExclusions == null) {
                            nameExclusions = new LinkedList<>();
                        }
                        nameExclusions.add(toExclude);
                    }
                }
                break;
            }
        }
    }

    public String getGsName() {
        return gsName;
    }

    public void setGsName(String gsName) {
        this.gsName = gsName;
    }

    public double getGsLat() {
        return gsLat;
    }

    public void setGsLat(double gsLat) {
        this.gsLat = gsLat;
    }

    public double getGsLon() {
        return gsLon;
    }

    public void setGsLon(double gsLon) {
        this.gsLon = gsLon;
    }

    public double getGsAltitude() {
        return gsAltitude;
    }

    public void setGsAltitude(double gsAltitude) {
        this.gsAltitude = gsAltitude;
    }

    public String getReferenceTleOrbitFilePath() {
        return referenceTleOrbitFilePath;
    }

    public void setReferenceTleOrbitFilePath(String referenceTleOrbitFilePath) {
        this.referenceTleOrbitFilePath = referenceTleOrbitFilePath;
    }

    public String getTargetTleOrbitFilePath() {
        return targetTleOrbitFilePath;
    }

    public void setTargetTleOrbitFilePath(String targetTleOrbitFilePath) {
        this.targetTleOrbitFilePath = targetTleOrbitFilePath;
    }

    public List<String> getNameExclusions() {
        return nameExclusions;
    }

    public void setNameExclusions(List<String> nameExclusions) {
        this.nameExclusions = nameExclusions;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public double getMaxAngularSeparation() {
        return maxAngularSeparation;
    }

    public void setMaxAngularSeparation(double maxAngularSeparation) {
        this.maxAngularSeparation = maxAngularSeparation;
    }

    public int getSampling() {
        return sampling;
    }

    public void setSampling(int sampling) {
        this.sampling = sampling;
    }

    public int getIntervalPeriod() {
        return intervalPeriod;
    }

    public void setIntervalPeriod(int intervalPeriod) {
        this.intervalPeriod = intervalPeriod;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Override
    public String toString() {
        return  "gsName=" + gsName + "\n" +
                "gsLat=" + gsLat + " deg\n" +
                "gsLon=" + gsLon + " deg\n" +
                "gsAltitude=" + gsAltitude + " m\n" +
                "referenceTleOrbitFilePath=" + referenceTleOrbitFilePath + "\n" +
                "targetTleOrbitFilePath=" + targetTleOrbitFilePath + "\n" +
                "outputFolder=" + outputFolder + "\n" +
                "nameExclusions=" + nameExclusions + "\n" +
                "maxHeight=" + maxHeight + " km\n" +
                "minHeight=" + minHeight + " km\n" +
                "maxAngularSeparation=" + maxAngularSeparation + " deg\n" +
                "sampling=" + sampling + " s\n" +
                "intervalPeriod=" + intervalPeriod + " s\n" +
                "cores=" + cores;
    }
}
