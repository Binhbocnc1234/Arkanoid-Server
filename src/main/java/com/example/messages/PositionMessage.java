package com.example.messages;

public class PositionMessage {
    public float x;
    public float y;
    
    public PositionMessage() {
        // Default constructor required for Kryo
    }
    
    public PositionMessage(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public String toString() {
        return "PositionMessage{x=" + x + ", y=" + y + "}";
    }
}

