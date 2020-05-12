package org.nmrfx.processor.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class IsotopeLabels {
    /**not using this now - DELETE**/
    private StringProperty labelScheme = new SimpleStringProperty();
    Sample sample;

    public IsotopeLabels (String labelScheme,Sample sample) {
        this.labelScheme.set(labelScheme);
        this.sample=sample;
    }

    @Override
    public String toString() {
        return labelScheme.get();
    }

    public String getLabelScheme() {
        return labelScheme.get();
    }

    public StringProperty labelSchemeProperty() {
        return labelScheme;
    }

}
