package ca.yorku.eecs3311.roomscheduler;

import java.time.LocalDateTime;

public final class RoomSensor {
    private final String sensorId;
    private final String roomId;
    private boolean occupied;
    private LocalDateTime lastUpdate;

    RoomSensor(String sensorId, String roomId) {
        this.sensorId = sensorId;
        this.roomId = roomId;
    }

    public void reportOccupancy(boolean occupied) {
        this.occupied = occupied;
        this.lastUpdate = LocalDateTime.now();
    }

    public String scanBadge(String accountId) {
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("Badge has no account ID.");
        lastUpdate = LocalDateTime.now();
        return accountId.trim();
    }

    public String sensorId() { return sensorId; }
    public String roomId() { return roomId; }
    public boolean occupied() { return occupied; }
    public LocalDateTime lastUpdate() { return lastUpdate; }
}
