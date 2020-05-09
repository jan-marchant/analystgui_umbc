package org.nmrfx.processor.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nmrfx.processor.datasets.peaks.AtomResonance;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakDim;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;

import java.util.HashMap;

public class Sample {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Molecule> molecule = new SimpleObjectProperty<>();
    private ObjectProperty<IsotopeLabels> labels = new SimpleObjectProperty<>();
    private ObjectProperty<Condition> condition = new SimpleObjectProperty<>();
    private HashMap<Atom,Double> atomFraction=new HashMap<>();



    public Sample (String name) {
        setName(name);
        setMolecule(UmbcProject.getActive().activeMol);
        setCondition(new Condition(name));
        setLabels(new IsotopeLabels(""));
    }

    public Sample (String name,String labelScheme) {
        setName(name);
        setMolecule(UmbcProject.getActive().activeMol);
        setCondition(new Condition(name));
        setLabels(new IsotopeLabels(labelScheme));
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

    public double getAtomFraction(Atom atom) {
        //TODO: support labeling schemes like A28/Ar (i.e. should not see A2/8-Ar NOEs from same residue in same molecule
        // means subtle difference in label string parsing
        //TODO: Add GUI support for setting labeling percent

        if (atom != null) {
            double fraction;
            try {
                fraction = atomFraction.get(atom);
            } catch (Exception e) {
                fraction = getLabels().getFraction(atom);
                if (fraction > 1.0) {
                    System.out.println("Check labeling string - " + atom.getFullName() + " is apparently " + fraction * 100 + "% labeled");
                    fraction = 1.0;
                }
                atomFraction.put(atom, fraction);
            }
            return fraction;
        } else {
            System.out.println("Couldn't find atom");
            return 0.0;
        }
    }
}
