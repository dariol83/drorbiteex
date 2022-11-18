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
