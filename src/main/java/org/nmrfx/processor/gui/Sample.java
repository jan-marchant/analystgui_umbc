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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

public class Sample implements Comparable<Sample> {
    private StringProperty name = new SimpleStringProperty();
    private ObjectProperty<Molecule> molecule = new SimpleObjectProperty<>();
    private StringProperty labelString = new SimpleStringProperty("");
    private HashMap<Atom, Double> atomFraction = new HashMap<>();
    private HashMap<Entity, String> entityLabelString = new HashMap<>();

    public Sample(String name) {
        setName(name);
        setMolecule(UmbcProject.getActive().activeMol);
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
        ArrayList<ManagedList> toReturn = new ArrayList<>();
        for (Acquisition acquisition : UmbcProject.getActive().acquisitionTable) {
            if (acquisition.getSample()==this) {
                toReturn.addAll(acquisition.getManagedLists());
            }
        }
        return toReturn;
    }

    public void remove(boolean prompt) {
//todo implement
    }

    public static void addNew() {
        if (UmbcProject.getActive().activeMol == null) {
            GUIUtils.warn("Error", "Molecule must be set before adding samples");
            return;
        }
        String base = "New Sample ";
        int suffix = 1;
        while (Sample.get(base + suffix) != null) {
            suffix++;
        }
        new Sample(base + suffix);
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
        boolean rna = false;
        Entity rnaEnt = null;
        if (molecule.get() != null) {
            for (Entity entity : molecule.get().entities.values()) {
                if (entity instanceof Polymer && ((Polymer) entity).isRNA()) {
                    rna = true;
                    rnaEnt = entity;
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
                UMBCApp.rnaLabelsController.setSampleAndEntity(this, rnaEnt);
            } else {
                GUIUtils.warn("Not implemented", "Sorry, labelling is only for RNA at the moment");
            }
        } else {
            GUIUtils.warn("No active molecule", "Add a molecule before setting up sample");
        }
    }

    public void setupLabels(Entity entity) {
        boolean rna = false;
        if (molecule.get() != null) {
            if (entity instanceof Polymer && ((Polymer) entity).isRNA()) {
                rna = true;
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
                UMBCApp.rnaLabelsController.setSampleAndEntity(this, entity);
            } else {
                GUIUtils.warn("Not implemented", "Sorry, labelling is only for RNA at the moment");
            }
        } else {
            GUIUtils.warn("No active molecule", "Add a molecule before setting up sample");
        }
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
        if (atom != null) {
            if (atom.getTopEntity() instanceof Polymer) {
                if (((Polymer) atom.getTopEntity()).isRNA()) {
                    return RNALabels.atomPercentLabelString(atom, getLabelString()) / 100;
                }
            }
            return 1.0;
        } else {
            System.out.println("Couldn't find atom");
            return 0;
        }
    }

    public String getEntityLabelString(Entity entity) {
        if (entity!=null) {
            entityLabelString.putIfAbsent(entity, "");
            return entityLabelString.get(entity);
        } else {
            return "";
        }
    }

    public void setEntityLabelString(Entity entity, String labels) {
        if (!labels.equals(getEntityLabelString(entity))) {
            Optional<ButtonType> result;
            if (getAssociatedLists().size() > 0) {
                if (getEntityLabelString(entity).equals("")) {
                    result = Optional.of(ButtonType.OK);
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Label Scheme Changed");
                    alert.setHeaderText("Reset labelling information for " + entity.getName() + "?");
                    alert.setContentText("This includes deleting all associated managed peakLists:" + getAssociatedLists());
                    result = alert.showAndWait();
                }
            } else {
                result = Optional.of(ButtonType.OK);
            }
            if (result.get() == ButtonType.OK) {
                atomFraction.clear();
                entityLabelString.put(entity, labels.replace(entity.getName()+":",""));
                updateLabelString();
                for (Acquisition acquisition : UmbcProject.getActive().acquisitionTable) {
                    if (acquisition.getSample()==this) {
                        acquisition.resetAcquisitionTree();
                    }
                }
                for (ManagedList list : getAssociatedLists()) {
                    PeakList.remove(list.getName());
                }
            }
        }
    }

    public int getId() {
        //fixme
        return 0;
    }

    public void writeStar3(FileWriter chan) throws IOException {
        if (molecule.get() == null) {
            return;
        }
        chan.write("save_sample_" + getName().replaceAll("\\W", "") + "\n");
        chan.write("_Sample.ID                          ");
        chan.write(getId() + "\n");
        chan.write("_Sample.Name                        ");
        chan.write("'" + getName() + "'\n");
        chan.write("_Sample.Sf_category                 ");
        chan.write("sample\n");
        chan.write("_Sample.Sf_framecode                ");
        chan.write("sample_" + getName().replaceAll("\\W", "") + "\n");
        chan.write("_Sample.Type                        ");
        chan.write(".\n");
        chan.write("\n");

        //fixme: handle multiple components with independent labeling. Reinstate IsotopeLabels class
        // and replace "molecule" column of table with component, splitting node into number of entities, each with independent labelString

        chan.write("loop_\n");
        chan.write("_Sample_component.Sample_ID\n");
        chan.write("_Sample_component.ID\n");
        chan.write("_Sample_component.Mol_common_name\n");
        //fixme: ? in STAR3 dictionary it says this should be a recognized standard...
        chan.write("_Sample_component.Isotopic_labeling\n");
        chan.write("\n");

        int entityID = 1;
        Iterator entityIterator = molecule.get().entityLabels.values().iterator();
        while (entityIterator.hasNext()) {
            Entity entity = (Entity) entityIterator.next();
            chan.write(String.format("%d %d %s '%s'", getId(), entityID, entity.label, getLabelString().equals("") ? "*" : getLabelString()));
            chan.write("\n");
            entityID++;
        }
        chan.write("stop_\n");
        chan.write("save_\n\n");
    }

    public static void writeAllStar3(FileWriter chan) throws IOException {
        for (Sample sample : UmbcProject.getActive().sampleList) {
            sample.writeStar3(chan);
        }
    }

    public ArrayList<Entity> getEntities() {
        return molecule.get().getEntities();
    }

    private void updateLabelString() {
        String labels="";
        for (Entity entity : entityLabelString.keySet()) {
            for (String group : entityLabelString.get(entity).split(" ")) {
                labels += entity.getName() + ":" + group+" ";
            }
        }
        labelString.set(labels);
    }
}
