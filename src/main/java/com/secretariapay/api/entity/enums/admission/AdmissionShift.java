package com.secretariapay.api.entity.enums.admission;

public enum AdmissionShift {
    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite");

    private final String label;

    AdmissionShift(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
