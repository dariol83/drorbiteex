/*
 * Copyright (c) 2022 Dario Lucia (https://www.dariolucia.eu)
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

public enum ServiceTypeEnum {
    APA_AZ_EL("APA-AZ/EL"),
    APA_X_Y("APA-X/Y"),
    DELTADOR("DELTADOR"),
    DOPPLER("DOPPLER"),
    OFFLINE_TMRECORDING("OFFLINE-TMRECORDING"),
    OFFLINE_TMPROVISION("OFFLINE-TMPROVISION"),
    RF_ONLY("RF-ONLY"),
    RANGING("RANGING"),
    RESERVED("RESERVED"),
    TBD("TBD"),
    TELECOMMAND("TELECOMMAND"),
    TELEMETRY("TELEMETRY"),
    TEST("TEST"),
    UNUSED("UNUSED"),
    VLBI("VLBI");

    private String type;

    ServiceTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return getType();
    }
}
