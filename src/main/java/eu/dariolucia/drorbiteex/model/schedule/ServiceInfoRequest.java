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
