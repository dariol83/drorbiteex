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

public enum FrequencyEnum {
    HF("HF"),
	VHF("VHF"),
    UHF("UHF"),
	L("L"),
	S_NE("S-NE"),
	S_DS("S-DS"),
	C("C"),
	X_NE("X-NE"),
	X_DS("X-DS"),
    KU("Ku"),
	K("K"),
	KU_NE("Ka-NE"),
	KA_DS("Ka-DS"),
	KA_S("Ka-S"),
	V("V"),
	W("W"),
	MM("mm"),
	O1("O1"),
	O2("O2"),
	N_A("N/A"),
	OTHER("OTHER");

    private String frequencyBand;

    FrequencyEnum(String frequencyBand) {
        this.frequencyBand = frequencyBand;
    }

    public String getFrequencyBand() {
        return frequencyBand;
    }

	@Override
	public String toString() {
		return getFrequencyBand();
	}
}
