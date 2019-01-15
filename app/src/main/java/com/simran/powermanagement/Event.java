package com.simran.powermanagement;

public class Event {
    private String eventName, eventTime, eventId;

    public Event(String eventName, String eventType, String eventId) {
        this.eventName = eventName;
        this.eventTime = eventType;
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }
    public String getEventTime() {
        return eventTime;
    }
    public String getEventId(){
        return eventId;
    }

    public void setEventName(String name) {
        this.eventName = name;
    }
    public void setEventTime(String time) {
        this.eventTime = time;
    }
    public void setEventId(String id){
        this.eventId = id;
    }
}