package eu.dariolucia.drorbiteex.model.schedule;

import java.util.Objects;

public class ServiceInfoRequest {

    private final ServiceTypeEnum service;
    private final FrequencyEnum frequency;

    public ServiceInfoRequest(ServiceTypeEnum service, FrequencyEnum frequency) {
        this.service = service;
        this.frequency = frequency;
    }

    public ServiceTypeEnum getService() {
        return service;
    }

    public FrequencyEnum getFrequency() {
        return frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInfoRequest that = (ServiceInfoRequest) o;
        return service == that.service &&
                frequency == that.frequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, frequency);
    }

    @Override
    public String toString() {
        return "ServiceInfoRequest{" +
                "service=" + service +
                ", frequency=" + frequency +
                '}';
    }
}
