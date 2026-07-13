package ca.yorku.eecs3311.roomscheduler;

import java.util.Objects;

public final class Room {
    public enum Status { AVAILABLE, DISABLED, MAINTENANCE }

    private final String id;
    private String name;
    private String location;
    private int capacity;
    private Status status;
    private final RoomSensor sensor;

    public Room(String id, String name, String location, int capacity, Status status) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Room ID is required.");
        this.id = id.trim();
        updateDetails(name, location, capacity);
        this.status = Objects.requireNonNull(status);
        this.sensor = new RoomSensor("SENSOR-" + this.id, this.id);
    }

    public void updateDetails(String name, String location, int capacity) {
        if (name == null || name.isBlank() || location == null || location.isBlank())
            throw new IllegalArgumentException("Room name and location are required.");
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive.");
        this.name = name.trim();
        this.location = location.trim();
        this.capacity = capacity;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String location() { return location; }
    public int capacity() { return capacity; }
    public Status status() { return status; }
    public RoomSensor sensor() { return sensor; }
    public void setStatus(Status status) { this.status = Objects.requireNonNull(status); }
    @Override public String toString() { return id + " - " + name + " (" + location + ", " + capacity + ")"; }
}

