package ca.yorku.eecs3311.roomscheduler;

public enum Role {
    REGISTERED_USER("Registered User"),
    ADMIN("Administrator"),
    CHIEF_EVENT_COORDINATOR("Chief Event Coordinator");

    private final String displayName;

    Role(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}

