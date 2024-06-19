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

package eu.dariolucia.drorbiteex.application;

import eu.dariolucia.drorbiteex.application.conf.CollinearityOfflineAnalyserConfiguration;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalyser;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityAnalysisRequest;
import eu.dariolucia.drorbiteex.model.collinearity.CollinearityEvent;
import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitParameterConfiguration;
import eu.dariolucia.drorbiteex.model.orbit.TleOrbitModel;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.GroundStationMask;
import eu.dariolucia.drorbiteex.model.station.GroundStationParameterConfiguration;
import eu.dariolucia.drorbiteex.model.util.TimeUtils;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class CollinearityOfflineAnalyser {

    private static final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + "drorbiteex";
    private static final String CONFIG_FOLDER_LOCATION_KEY = "drorbiteex.config";
    private static final String OREKIT_FOLDER_NAME = "orekit-data";

    private final CollinearityOfflineAnalyserConfiguration configuration;
    private final List<Instant> instants;

    public CollinearityOfflineAnalyser(CollinearityOfflineAnalyserConfiguration configuration, List<Instant> instants) {
        this.configuration = configuration;
        this.instants = instants;
    }

    private void start() throws IOException {
        System.out.println("===============================================================================================");
        System.out.println("Dr. Orbiteex - Collinearity Analysis");
        System.out.println("===============================================================================================");
        System.out.println("Configuration: ");
        System.out.println(configuration);
        // Create the list of target orbit models
        List<CelestrakTleData> targetTleFileContents = CelestrakTleData.processCelestrakFile("---", configuration.getTargetTleOrbitFilePath());
        // Run analysis
        for(Instant i : instants) {
            runCollinearity(i, new ArrayList<>(targetTleFileContents));
        }
    }

    private void runCollinearity(Instant time, List<CelestrakTleData> targetTleFileContents) throws IOException {
        long startCompTime = System.currentTimeMillis();
        System.out.println("===============================================================================================");
        System.out.println("Running collinearity analysis on time " + time);
        Date startTime = new Date(time.minusSeconds(configuration.getIntervalPeriod()/2).toEpochMilli());
        Date endTime = new Date(time.plusSeconds(configuration.getIntervalPeriod()/2).toEpochMilli());
        // Create the ground station model
        GroundStation station = new GroundStation(UUID.randomUUID(), configuration.getGsName(), configuration.getGsName(), "---", "---", "---", true,
                configuration.getGsLat(), configuration.getGsLon(), configuration.getGsAltitude(),
                new GroundStationMask());
        station.setConfiguration(new GroundStationParameterConfiguration());

        // Create the reference orbit model
        List<CelestrakTleData> referenceTleFileContents = CelestrakTleData.processCelestrakFile("---", configuration.getReferenceTleOrbitFilePath());
        String referenceOrbitName = referenceTleFileContents.get(0).getName();
        TleOrbitModel referenceTleModel = new TleOrbitModel(referenceTleFileContents.get(0).getTle());
        Orbit referenceOrbit = new Orbit(UUID.randomUUID(), String.valueOf(referenceTleModel.getTleObject().getSatelliteNumber()), referenceOrbitName, "---", true, referenceTleModel);
        referenceOrbit.setOrbitConfiguration(new OrbitParameterConfiguration());

        // Prepare the file to write
        File toWrite = new File(configuration.getOutputFolder() + File.separator +
                "CollinearityAnalysis_" + sanitize(referenceOrbitName) + "_" + sanitize(configuration.getGsName()) + "_"
                + sanitize(TimeUtils.formatDate(time).replaceAll(" ", "_")) + "_"
                + sanitize(TimeUtils.formatDate(new Date()).replaceAll(" ", "_")) + ".csv");
        toWrite.createNewFile();
        PrintStream writer = new PrintStream(new FileOutputStream(toWrite));

        // Write header
        writer.println(CollinearityEvent.getCsvHeader());

        // Prepare data for statistics
        int totalBatches = (int) Math.ceil(targetTleFileContents.size() / (double) configuration.getCores());
        int lastPercentagePrint = 0;

        int batch = 1;
        // Depending on the number of cores, build and submit <cores> orbits for collinearity computation
        while(!targetTleFileContents.isEmpty()) {
            // Extract Math.min(targetTleFileContents.size(), configuration.getCores())
            int nbToProcess = Math.min(targetTleFileContents.size(), configuration.getCores());
            List<CelestrakTleData> toProcess = new ArrayList<>(targetTleFileContents.subList(0, nbToProcess));
            targetTleFileContents.subList(0, nbToProcess).clear();
            // Build corresponding Orbit objects
            List<Orbit> targetOrbits = new ArrayList<>(nbToProcess);
            for(CelestrakTleData tleData : toProcess) {
                String orbitName = tleData.getName();
                try {
                    TleOrbitModel tleModel = new TleOrbitModel(tleData.getTle());
                    Orbit orbit = new Orbit(UUID.randomUUID(), String.valueOf(tleModel.getTleObject().getSatelliteNumber()), orbitName, "---", true, tleModel);
                    orbit.setOrbitConfiguration(new OrbitParameterConfiguration());
                    targetOrbits.add(orbit);
                } catch (Exception e) {
                    System.err.println("Error when loading TLE for orbit " + orbitName);
                    System.err.println(tleData.getTle());
                    e.printStackTrace();
                }
            }
            // Build a CollinearityAnalysisRequest
            CollinearityAnalysisRequest request = new CollinearityAnalysisRequest(
                    startTime,
                    endTime,
                    station,
                    referenceOrbit,
                    configuration.getMaxAngularSeparation(),
                    configuration.getSampling(),
                    configuration.getCores(),
                    configuration.getNameExclusions(),
                    configuration.getMinHeight(),
                    configuration.getMaxHeight(),
                    null,
                    targetOrbits
            );
            // Run the analysis
            List<CollinearityEvent> result = CollinearityAnalyser.analyse(request, null);
            if(result != null) {
                // Put the data in the file
                result.forEach(e -> writer.println(e.toCSV()));
            } else {
                System.err.println("Error when processing collinearity for time " + time + ": null result");
            }
            //
            ++batch;
            double percentage = (double) batch / totalBatches;
            // The number is between 0 and 1. Multiply the number by 10, convert to integer, multiply by 10 again and check
            int newPercentage = ((int) (percentage * 10)) * 10;
            if(newPercentage > lastPercentagePrint && newPercentage != 100) {
                System.out.print(newPercentage + "% ... ");
                System.out.flush();
                lastPercentagePrint = newPercentage;
            }
        }
        System.out.println("100%");
        // Export the CSV file
        writer.close();
        System.out.println("Collinearity analysis completed for time " + time + ":\n exported file " + toWrite.getAbsolutePath());
        long durationSecs = (System.currentTimeMillis() - startCompTime)/1000;
        // Print duration in minutes:seconds
        System.out.println("Processing time: " + (durationSecs/60) + " minute(s) " + (durationSecs % 60) + " second(s)");
    }

    private String sanitize(String s) {
        return s.replaceAll(" ", "")
                .replaceAll("-","")
                .replaceAll(":","")
                .replaceAll("\\[","")
                .replaceAll("\\(","")
                .replaceAll("\\)","")
                .replaceAll("\\.","")
                .replaceAll("]","");
    }

    public static void main(String[] args) throws IOException {
        // Usage: CollinearityOfflineAnalyser <configuration file> <timestamp files>
        // Argument check
        if(args.length != 2) {
            System.err.println("Usage: CollinearityOfflineAnalyser <configuration file> <timestamp files>");
            System.exit(1);
        }

        // Load the configuration
        String configurationFile = args[0];
        Properties props = new Properties();
        props.load(new FileInputStream(configurationFile));
        CollinearityOfflineAnalyserConfiguration configuration = new CollinearityOfflineAnalyserConfiguration(props);

        // Load the instants
        List<Instant> instants = loadInstants(args[1]);

        // Load Orekit data
        // Load old configuration if available
        String configLocation = System.getProperty(CONFIG_FOLDER_LOCATION_KEY);
        File orekitData;
        if(configLocation != null && !configLocation.isBlank()) {
            orekitData = new File(configLocation + File.separator + OREKIT_FOLDER_NAME);
        } else {
            orekitData = new File(DEFAULT_CONFIG_FOLDER + File.separator + OREKIT_FOLDER_NAME);
        }
        // Orekit initialisation
        try {
            DataProvidersManager orekitManager = DataContext.getDefault().getDataProvidersManager();
            orekitManager.addProvider(new DirectoryCrawler(orekitData));
        } catch (Exception e) {
            // You have to quit
            System.err.println("Orekit initialisation data not found. Steps to fix the problem:\n" +
                    "1) download https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip\n" +
                    "2) extract the archive and rename the resulting extracted folder to 'orekit-data'\n" +
                    "3) either copy the 'orekit-data' folder\n" +
                    "\t3a) inside " + DEFAULT_CONFIG_FOLDER + " or \n" +
                    "\t3b) inside another folder of your choice and " +
                    "start Dr. Orbiteex JVM with the system property -D" + CONFIG_FOLDER_LOCATION_KEY + "=<path to your folder>");
            e.printStackTrace();
            System.exit(-1);
        }

        // Feed updated information for orbit determination to Orekit. If it fails, keep going
        try {
            MarshallSolarActivityFutureEstimation msafe = new MarshallSolarActivityFutureEstimation(
                    MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                    MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            DataProvidersManager orekitManager = DataContext.getDefault().getDataProvidersManager();
            orekitManager.feed(msafe.getSupportedNames(), msafe); // Feeding the F10.7 bulletins to Orekit's data manager
        } catch (Exception e) {
            System.err.println("Cannot feed F10.7 bulletins to Orekit's data manager");
            e.printStackTrace();
        }

        // Start the analysis
        CollinearityOfflineAnalyser analyser = new CollinearityOfflineAnalyser(configuration, instants);
        analyser.start();
    }

    private static List<Instant> loadInstants(String file) throws IOException {
        List<Instant> toReturn = new LinkedList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String read = null;
        while((read = br.readLine()) != null) {
            if(read.isBlank()) {
                continue;
            }
            if(read.trim().startsWith("#")) {
                continue;
            }
            Instant t = Instant.parse(read.trim());
            toReturn.add(t);
        }
        br.close();
        return toReturn;
    }
}
