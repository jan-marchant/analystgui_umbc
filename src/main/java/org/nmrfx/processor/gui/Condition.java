package org.nmrfx.processor.gui;

public class Condition {
    //possibly this should just be a string
    String name;
    public Condition(String name) {
        this.name=name;
    }
    public String toString() {
        return name;
    }
}
