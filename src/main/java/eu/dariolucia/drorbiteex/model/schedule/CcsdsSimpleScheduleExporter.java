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

package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CcsdsSimpleScheduleExporter {

    private final PrintStream out;
    private final String file;
    private final ScheduleGenerationRequest request;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
    private final IScheduleExporter exporter;

    public CcsdsSimpleScheduleExporter(String file, IScheduleExporter exporter, ScheduleGenerationRequest request) throws IOException {
        this.file = file;
        this.request = request;
        File theFile = new File(file);
        if(!theFile.exists()) {
            theFile.createNewFile();
        }
        this.out = new PrintStream(new FileOutputStream(this.file));
        this.timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.exporter = exporter;
        writeStart();
    }

    private void writeStart() {
        out.println("<?xml version=\"1.0\"?>");
        LinkedHashMap<String, String> attribs = exporter.getSimpleScheduleRootAttributes(request.getGroundStation());
        out.print("<simpleSchedule");
        for(Map.Entry<String, String> e : attribs.entrySet()) {
            out.print(" " + e.getKey() + "=\"" + e.getValue() + "\"");
        }
        out.println(">");
    }

    public void writeHeader(ScheduleGenerationRequest request, Date generationDate) {
        out.println("\t<simpleScheduleHeader" +
                " originatingOrganization=\"" + request.getOriginatingRequest() + "\"" +
                " generationTime=\"" + timeFormatter.format(generationDate)+ "\"" +
                " status=\"" + request.getStatus().name() + "\"" +
                " inclusionType=\"START_INCLUSION\"\n" +
                "\t version=\"1\"" +
                " startTime=\"" + timeFormatter.format(request.getStartTime())+ "\"" +
                " endTime=\"" + timeFormatter.format(request.getEndTime())+ "\"/>");
    }

    public void writeScheduledPackage(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, List<VisibilityWindow> passes, Map<Orbit, List<VisibilityWindow>> allPasses) {
        // Open Package - one per satellite
        String packageId = exporter.getScheduledPackageIdFor(request, station, orbit);
        String packageComment = exporter.getScheduledPackageCommentFor(request, station, orbit, packageId);
        String user = exporter.getScheduledPackageUserFor(request, station, orbit, packageId);
        String originatingRequestId = exporter.getScheduledPackageOriginatingRequestIdFor(request, station, orbit, packageId);
        out.print("\t\t<scheduledPackage " +
                "user=\"" + user + "\" " +
                "comment=\"" + packageComment + "\" " +
                "scheduledPackageId=\"" + packageId + "\"");
        if(originatingRequestId != null) {
            out.println(" originatingRequestId=\"" + originatingRequestId + "\">");
        } else {
            out.println(">");
        }
        // XRef (optional)
        ServicePackageXRef xRef = exporter.getServicePackageXRefFor(request, station, orbit, packageId, packageComment, user);
        if(xRef != null) {
            out.println("\t\t\t<servicePackageXRef serviceAgreementRef=\"" + xRef.getServiceAgreementRef() + "\"" +
                    " servicePackageRef=\"" + xRef.getServicePackageRef() + "\"/>");
        }
        // Activities
        for(VisibilityWindow vw : passes) {
            writeActivity(request, station, orbit, vw, request.getServiceInfoRequests(), packageId, allPasses);
        }
        // Close Package
        out.println("\t\t</scheduledPackage>");
    }

    private void writeActivity(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId, Map<Orbit, List<VisibilityWindow>> allPasses) {
        // Activity
        String activityId = exporter.getScheduledActivityIdFor(request, station, vw);
        ActivityStatusEnum activityStatus = exporter.getScheduledActivityStatusFor(request, station, vw, services, packageId, activityId);
        Date aos = exporter.getBeginningOfTrackFor(request, station, vw, services, packageId, activityId);
        Date los = exporter.getEndOfTrackFor(request, station, vw, services, packageId, activityId);
        out.print("\t\t\t<scheduledActivity scheduledActivityId=\"" + activityId + "\"" +
                " activityStatus=\"" + activityStatus.name() + "\"" +
                " siteRef=\"" + station.getSite() + "\"" +
                " apertureRef=\"" + station.getCode() + "\"\n" +
                " \t\t\torbitNumber=\"" + vw.getOrbitNumber() + "\"" +
                " beginningOfTrack=\"" + timeFormatter.format(aos) + "\"" +
                " endOfTrack=\"" + timeFormatter.format(los) + "\"");
        if(request.getStartEndActivityDeltaSeconds() >= 0) {
            Date beginningOfActivity = new Date(aos.getTime() - (request.getStartEndActivityDeltaSeconds() * 1000L));
            Date endOfActivity = new Date(los.getTime() + (request.getStartEndActivityDeltaSeconds() * 1000L));
            out.println("\n\t\t\t beginningOfActivity=\"" + timeFormatter.format(beginningOfActivity)+ "\"" +
                    " endOfActivity=\"" + timeFormatter.format(endOfActivity)+ "\">");
        } else {
            // Check the default exporter
            Date beginningOfActivity = exporter.getBeginningOfActivityFor(request, station, vw, services, packageId, activityId);
            Date endOfActivity = exporter.getEndOfActivityFor(request, station, vw, services, packageId, activityId);
            if(beginningOfActivity != null) {
                out.print("\n\t\t\t beginningOfActivity=\"" + timeFormatter.format(beginningOfActivity)+ "\"");
            }
            if(endOfActivity != null) {
                out.print("\n\t\t\t endOfActivity=\"" + timeFormatter.format(endOfActivity)+ "\"");
            }
            out.println(">");
        }
        // Service info
        int serviceIdx = 0;
        for(ServiceInfoRequest sir : services) {
            ServiceInfoParameter serviceInfoData = exporter.getServiceInfoParameterFor(request, station, vw, services, packageId, activityId, sir, serviceIdx, services.size(), allPasses);
            if(serviceInfoData == null) {
                out.println("\t\t\t\t<serviceInfo serviceType=\"" + sir.getService().getType() + "\"" +
                        " frequencyBand=\"" + sir.getFrequency().getFrequencyBand() + "\"/>");
            } else {
                out.println("\t\t\t\t<serviceInfo serviceType=\"" + sir.getService().getType() + "\"" +
                        " frequencyBand=\"" + sir.getFrequency().getFrequencyBand() + "\">");
                if(serviceInfoData.getName() != null) {
                    out.println("\t\t\t\t\t<extendedParameter name=\"" + serviceInfoData.getName()+ "\">");
                }
                for (ScheduledActivityParameter p : serviceInfoData.getParameters()) {
                    out.println("\t\t\t\t\t<" + p.getTagName() + " name=\"" + p.getParameterName() + "\" value=\"" + p.getParameterValue() + "\" />");
                }
                if(serviceInfoData.getName() != null) {
                    out.println("\t\t\t\t\t</extendedParameter>");
                }
                out.println("\t\t\t\t</serviceInfo>");
            }
            ++serviceIdx;
        }
        // Extended activity parameters
        List<ScheduledActivityParameter> extendedParameters = exporter.getScheduledActivityParameterFor(request, station, vw, services, packageId, activityId);
        for(ScheduledActivityParameter p : extendedParameters) {
            out.println("\t\t\t\t<" + p.getTagName() + " name=\"" + p.getParameterName() + "\" value=\"" + p.getParameterValue() + "\" />");
        }
        out.println("\t\t\t</scheduledActivity>");
    }

    public void close() {
        out.println("</simpleSchedule>");
        out.close();
    }
}
