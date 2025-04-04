package com.pressure_sensor;

public class Event {
    public String description;
    public long timestamp; // Use System.currentTimeMillis() when creating an event
    public long symptomLogId; // Stores the ID of a symptom log; -1 means not applicable

    // Constructor for events that don't have an associated symptom log
    public Event(String description, long timestamp) {
        this(description, timestamp, -1);
    }

    // Constructor for events with a symptom log ID
    public Event(String description, long timestamp, long symptomLogId) {
        this.description = description;
        this.timestamp = timestamp;
        this.symptomLogId = symptomLogId;
    }
}
