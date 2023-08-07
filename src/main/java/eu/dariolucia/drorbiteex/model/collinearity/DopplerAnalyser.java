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

package eu.dariolucia.drorbiteex.model.collinearity;

import eu.dariolucia.drorbiteex.model.station.TrackPoint;
import eu.dariolucia.drorbiteex.model.util.ITaskProgressMonitor;
import org.orekit.utils.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DopplerAnalyser {
    private static final ITaskProgressMonitor DUMMY_MONITOR = new ITaskProgressMonitor() { };

    public static Map<String, List<ErrorPoint>> analyse(DopplerAnalysisRequest request, ITaskProgressMonitor monitor) throws IOException {
        if(monitor == null) {
            monitor = DUMMY_MONITOR;
        }
        if(monitor.isCancelled()) {
            return null;
        }

        Map<String, List<ErrorPoint>> toReturn = new HashMap<>();
        List<ErrorPoint> points = new LinkedList<>();
        toReturn.put(request.getTrackPoints().get(0).getSpacecraftPosition().getOrbit().getName(), points);
        for(TrackPoint tp : request.getTrackPoints()) {
            double doppler = tp.getDoppler();
            double range = tp.getRange();
            double dopplerFrequency = - tp.getDoppler() * (request.getFrequency()/Constants.SPEED_OF_LIGHT);
            ErrorPoint ep = new ErrorPoint(tp.getTime().toInstant(), doppler, range, dopplerFrequency);
            points.add(ep);
        }
        return toReturn;
    }
}
