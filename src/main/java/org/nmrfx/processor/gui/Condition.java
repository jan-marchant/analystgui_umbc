package org.nmrfx.processor.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Condition {
    //possibly this should just be a string
    StringProperty name=new SimpleStringProperty();

    public Condition(String name) {
        setName(name);
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }
}
