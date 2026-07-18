package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;

/** Registered-user categories and their configurable hourly room rates. */
public enum UserType {
    STUDENT("Student", "20.00", true),
    FACULTY("Faculty", "30.00", true),
    STAFF("Staff", "40.00", true),
    PARTNER("External Partner", "50.00", false);

    private final String displayName;
    private final BigDecimal hourlyRate;
    private final boolean universityType;

    UserType(String displayName, String hourlyRate, boolean universityType) {
        this.displayName = displayName;
        this.hourlyRate = new BigDecimal(hourlyRate);
        this.universityType = universityType;
    }

    public String displayName() { return displayName; }
    public BigDecimal hourlyRate() { return hourlyRate; }
    public boolean universityType() { return universityType; }
    @Override public String toString() { return displayName + " ($" + hourlyRate + "/hour)"; }
}

