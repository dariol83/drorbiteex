package eu.dariolucia.drorbiteex.model.schedule;

public class ServicePackageXRef {

    private final String serviceAgreementRef;
    private final String servicePackageRef;

    public ServicePackageXRef(String serviceAgreementRef, String servicePackageRef) {
        this.serviceAgreementRef = serviceAgreementRef;
        this.servicePackageRef = servicePackageRef;
    }

    public String getServiceAgreementRef() {
        return serviceAgreementRef;
    }

    public String getServicePackageRef() {
        return servicePackageRef;
    }
}
