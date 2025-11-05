package com.example.messages;

public class AssignIdMessage {
    public int id; // 0=rejected, 1=playerA, 2=playerB
    
    public AssignIdMessage() {
        // Default constructor required for Kryo
    }
    
    public AssignIdMessage(int id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "AssignIdMessage{id=" + id + "}";
    }
}

