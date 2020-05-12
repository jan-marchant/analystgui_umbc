package org.nmrfx.processor.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.utils.GUIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class Sample implements Comparable<Sample> {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Molecule> molecule = new SimpleObjectProperty<>();
    private StringProperty labelString = new SimpleStringProperty("");
    private ObjectProperty<Condition> condition = new SimpleObjectProperty<>();
    private HashMap<Atom,Double> atomFraction=new HashMap<>();

    public Sample (String name) {
        setName(name);
        setMolecule(UmbcProject.getActive().activeMol);
        setCondition(new Condition(name));
        UmbcProject.getActive().sampleList.add(this);
    }

    public Sample (String name,String labelString) {
        setName(name);
        setMolecule(UmbcProject.getActive().activeMol);
        setCondition(new Condition(name));
        setLabelString(labelString);
        UmbcProject.getActive().sampleList.add(this);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(Sample other) {
        return this.getName().compareTo(other.getName());
    }

    public String getLabelString() {
        return labelString.get();
    }

    public StringProperty labelStringProperty() {
        return labelString;
    }

    public ArrayList<ManagedList> getAssociatedLists() {
        return new ArrayList<>();
    }
    public void remove(boolean prompt) {

    }

    public static void addNew() {
        String base="Sample ";
        int suffix=1;
        while (Sample.get(base+suffix)!=null) {
            suffix++;
        }
        new Sample(base+suffix);
    }

    public static Sample get(String name) {
        for (Sample sample : UmbcProject.getActive().sampleList) {
            if (sample.getName().equals(name)) {
                return sample;
            }
        }
        return null;
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

    public void setupLabels() {
        boolean rna=false;
        if (molecule.get()!=null) {
            for (Entity entity : molecule.get().entities.values()) {
                if (entity instanceof Polymer && ((Polymer) entity).isRNA()) {
                    rna = true;
                }
            }
            if (rna) {
                if (UMBCApp.rnaLabelsController == null) {
                    UMBCApp.rnaLabelsController = RNALabelsSceneController.create();
                }
                if (UMBCApp.rnaLabelsController != null) {
                    UMBCApp.rnaLabelsController.getStage().show();
                    UMBCApp.rnaLabelsController.getStage().toFront();
                } else {
                    System.out.println("Couldn't make rnaLabelsController ");
                }
                UMBCApp.rnaLabelsController.setSample(this);
            } else {
                GUIUtils.warn("Not implemented", "Sorry, labelling is only for RNA at the moment");
            }
        } else {
            GUIUtils.warn("No active molecule", "Add a molecule before setting up sample");
        }
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
                fraction = getFraction(atom);
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

    public double getFraction(Atom atom) {
        //TODO: implement label scheme setup for arbitrary molecules
        if (atom!=null) {
            if (atom.getTopEntity() instanceof Polymer) {
                if (((Polymer) atom.getTopEntity()).isRNA()) {
                    return RNALabels.atomPercentLabelString(atom, getLabelString())/100;
                }
            }
            return 1.0;
        } else {
            System.out.println("Couldn't find atom");
            return 0;
        }
    }

    public void setLabelString(String labels) {
        if (!labels.equals(getLabelString())) {
            Optional<ButtonType> result;
            if (getAssociatedLists().size() > 0) {
                if (labelString.get().equals("")) {
                    result = Optional.of(ButtonType.OK);
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Label Scheme Changed");
                    alert.setHeaderText("Clear all labelling information for " + this + "?");
                    alert.setContentText("This includes deleting all associated managed peakLists:" + getAssociatedLists());
                    result = alert.showAndWait();
                }
            } else {
                result = Optional.of(ButtonType.OK);
            }
            if (result.get() == ButtonType.OK) {
                atomFraction.clear();
                labelString.set(labels);
                for (ManagedList list : getAssociatedLists()) {
                    PeakList.remove(list.getName());
                }
            }
        }
    }
}
