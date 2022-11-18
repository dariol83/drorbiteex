package eu.dariolucia.drorbiteex.model.schedule;

import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.station.GroundStation;
import eu.dariolucia.drorbiteex.model.station.VisibilityWindow;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CcsdsSimpleScheduleExporter {

    private final PrintStream out;
    private final String file;

    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
    private final IScheduleExporter exporter;

    public CcsdsSimpleScheduleExporter(String file, IScheduleExporter exporter) throws IOException {
        this.file = file;
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
        out.println("<simpleSchedule xmlns=\"urn:ccsds:schema:cssm:1.0.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"\n" +
                " xsi:schemaLocation=\"urn:ccsds:schema:cssm:1.0.0 902x01b1TC1-SmplSchd.xsd\" >");
    }

    public void writeHeader(ScheduleGenerationRequest request) {
        out.println("\t<simpleScheduleHeader" +
                " originatingOrganization=\"" + request.getOriginatingRequest() + "\"" +
                " generationTime=\"" + timeFormatter.format(new Date())+ "\"" +
                " status=\"" + request.getStatus().name() + "\"" +
                " inclusionType=\"START_INCLUSION\"\n" + // TODO: make it parametric
                "\t version=\"1\"" +
                " startTime=\"" + timeFormatter.format(request.getStartTime())+ "\"" +
                " endTime=\"" + timeFormatter.format(request.getEndTime())+ "\"/>");
    }

    public void writeScheduledPackage(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, List<VisibilityWindow> passes) {
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
            writeActivity(request, station, orbit, vw, request.getServiceInfoRequests(), packageId);
        }
        // Close Package
        out.println("\t\t</scheduledPackage>");
    }

    private void writeActivity(ScheduleGenerationRequest request, GroundStation station, Orbit orbit, VisibilityWindow vw, List<ServiceInfoRequest> services, String packageId) {
        // Activity
        String activityId = exporter.getScheduledActivityIdFor(request, station, vw);
        ActivityStatusEnum activityStatus = exporter.getScheduledActivityStatusFor(request, station, vw, services, packageId, activityId);
        out.print("\t\t\t<scheduledActivity scheduledActivityId=\"" + activityId + "\"" +
                " activityStatus=\"" + activityStatus.name() + "\"" +
                " siteRef=\"" + station.getSite() + "\"" +
                " apertureRef=\"" + station.getCode() + "\"\n" +
                " \t\t\torbitNumber=\"" + vw.getOrbitNumber() + "\"" +
                " beginningOfTrack=\"" + timeFormatter.format(vw.getAos()) + "\"" +
                " endOfTrack=\"" + timeFormatter.format(vw.getLos()) + "\"");
        if(request.getStartEndActivityDeltaSeconds() >= 0) {
            Date beginningOfActivity = new Date(vw.getAos().getTime() - (request.getStartEndActivityDeltaSeconds() * 1000L));
            Date endOfActivity = new Date(vw.getLos().getTime() + (request.getStartEndActivityDeltaSeconds() * 1000L));
            out.println("\n\t\t\t beginningOfActivity=\"" + timeFormatter.format(beginningOfActivity)+ "\"" +
                    " endOfActivity=\"" + timeFormatter.format(endOfActivity)+ "\">");
        } else {
            out.println(">");
        }
        // Service info
        for(ServiceInfoRequest sir : services) {
            out.println("\t\t\t\t<serviceInfo serviceType=\"" + sir.getService().getType() + "\"\n" +
                    " frequencyBand=\"" + sir.getFrequency().getFrequencyBand() + "\" />");
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
