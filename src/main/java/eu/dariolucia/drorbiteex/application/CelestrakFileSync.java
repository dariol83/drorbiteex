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

import eu.dariolucia.drorbiteex.model.orbit.CelestrakTleData;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CelestrakFileSync {

    private static final String DEFAULT_CONFIG_FOLDER = System.getProperty("user.home") + File.separator + "drorbiteex";
    private static final String CONFIG_FOLDER_LOCATION_KEY = "drorbiteex.config";
    private static final String OREKIT_FOLDER_NAME = "orekit-data";
    public static void main(String[] args) throws IOException {
        // Usage: CelestrakFileSync <Celestrak group name> <file to update>
        // Argument check
        if(args.length != 2) {
            System.err.println("Usage: CelestrakFileSync <Celestrak group name> <file to update>");
            System.exit(1);
        }

        String celestrakGroupName = args[0];
        String fileToUpdate = args[1];

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

        System.out.println("===============================================================================================");
        System.out.println("Dr. Orbiteex - Celestrak File Updater");
        System.out.println("===============================================================================================");

        // Retrieve CelestrakTleData
        System.out.println("Fetching Celestrak data for group " + celestrakGroupName);
        Map<String, CelestrakTleData> name2updatedData = null;
        List<CelestrakTleData> updatedData = CelestrakTleData.retrieveSpacecraftList(celestrakGroupName);
        if(updatedData != null) {
            name2updatedData = updatedData.stream().collect(Collectors.toMap(CelestrakFileSync::generateKey, Function.identity()));
        } else {
            System.err.println("Error: cannot fetch Celestrak data");
            System.exit(1);
        }

        // Retrieve the data in the file
        System.out.println("Loading spacecraft information from file " + fileToUpdate);
        List<CelestrakTleData> newData = null;
        List<CelestrakTleData> oldData = CelestrakTleData.processCelestrakFile(celestrakGroupName, fileToUpdate);
        if(oldData != null) {
            newData = alignData(oldData, name2updatedData);
        } else {
            System.err.println("Error: cannot parse file data " + fileToUpdate);
            System.exit(1);
        }

        // Export file
        exportFile(fileToUpdate, newData);
    }

    private static void exportFile(String fileToUpdate, List<CelestrakTleData> newData) throws IOException {
        // First, move original file to backup
        File oldFile = new File(fileToUpdate);
        boolean moved = oldFile.renameTo(new File(fileToUpdate + ".bkp." + System.currentTimeMillis()));
        String newFileName = fileToUpdate;
        if(!moved) {
            System.err.println("Cannot move original TLE file " + fileToUpdate + " to backup, generating new file");
            // Save file as new
            newFileName = fileToUpdate + ".new." + System.currentTimeMillis();
        }
        File newFile = new File(newFileName);
        if(!newFile.exists()) {
            boolean created = newFile.createNewFile();
            if(!created) {
                System.err.println("Cannot create TLE file " + newFileName + ", no file will be generated");
                System.exit(1);
            }
        }
        PrintStream ps = new PrintStream(new FileOutputStream(newFile));
        for(CelestrakTleData d : newData) {
            ps.println(d.getName());
            ps.println(d.getTle());
        }
        ps.close();
    }

    private static List<CelestrakTleData> alignData(List<CelestrakTleData> oldData, Map<String, CelestrakTleData> name2updatedData) {
        System.out.println("Aligning data for " + oldData.size() + " spacecraft... ");
        List<CelestrakTleData> newData = new LinkedList<>();
        int total = oldData.size();
        int success = 0;
        int failed = 0;
        for(CelestrakTleData d : oldData) {
            System.out.print("Updating spacecraft " + d.getName() + " ... ");
            System.out.flush();
            String key = generateKey(d);
            CelestrakTleData newTle = name2updatedData.get(key);
            if(newTle == null) {
                System.out.println("Failed");
                System.err.println("Spacecraft " + d.getName() + " with key " + key + " not found in Celestrak updated TLE list, old TLE kept");
                newTle = d;
                ++failed;
            } else {
                System.out.println("OK");
                ++success;
            }
            newData.add(newTle);
        }
        System.out.println("Alignment completed: " + success + " success - " + failed + " failed - " + total + " total");
        return newData;
    }

    private static String generateKey(CelestrakTleData data) {
        String key = data.getName() + " ";
        TLE tle = new TLE(data.getTle().substring(0, data.getTle().indexOf("\n")).trim(), data.getTle().substring(data.getTle().indexOf("\n")).trim());
        return key + tle.getSatelliteNumber();
    }
}
