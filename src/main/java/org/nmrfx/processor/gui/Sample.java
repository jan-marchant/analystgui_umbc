package org.nmrfx.processor.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;

public class Sample {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Molecule> molecule = new SimpleObjectProperty<>();
    private ObjectProperty<IsotopeLabels> labels = new SimpleObjectProperty<>();
    private ObjectProperty<Condition> condition = new SimpleObjectProperty<>();


    public Sample (String name) {
        setName(name);
    }

    public String toString() {
        return getName();
    }
    public void remove(boolean prompt) {

    }

    public static void addNew() {

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

    public Molecule getMolecule() {
        return molecule.get();
    }

    public ObjectProperty<Molecule> moleculeProperty() {
        return molecule;
    }

    public void setMolecule(Molecule molecule) {
        this.molecule.set(molecule);
    }

    public IsotopeLabels getLabels() {
        return labels.get();
    }

    public ObjectProperty<IsotopeLabels> labelsProperty() {
        return labels;
    }

    public void setLabels(IsotopeLabels labels) {
        this.labels.set(labels);
    }

    public Condition getCondition() {
        return condition.get();
    }

    public ObjectProperty<Condition> conditionProperty() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition.set(condition);
    }
}
