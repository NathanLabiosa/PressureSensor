package com.pressure_sensor;

public class Event {
    public String description;
    public long timestamp; // Use System.currentTimeMillis() when creating an event

    public Event(String description, long timestamp) {
        this.description = description;
        this.timestamp = timestamp;
    }
}

