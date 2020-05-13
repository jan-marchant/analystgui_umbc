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

    public int getId() {
        //fixme
        return 0;
    }

    public void writeStar3(FileWriter chan) throws IOException {
/** saveframe sample
 * Tag category Sample
 * Entry_ID	Pointer to '_Entry.ID'	code	yes
 * ID	A value that uniquely identifies the sample described from the other samples listed in the entry.	int	yes
 * Sf_category	Category assigned to the information in the save frame.	code	yes
 * Sf_framecode	A value that uniquely identifies this sample from the other samples listed in the entry.	framecode	yes
 * Sf_ID	An interger value that is the unique identifier for the save frame that applies across the archive. This value is not stable and may be reassigned each time the data are loaded into a database system.	int	yes
 * Type	A descriptive term for the sample that defines the general physical properties of the sample.	line	yes
 *
 * Tag category Sample_component
 * (for individual molecules)
 * Entry_ID	Pointer to '_Entry.ID'	code	yes
 * ID	A value that uniquely identifies each component of the sample in the component list.	int	yes
 * Isotopic_labeling	If this molecule in the sample was isotopically labeled provide a description of the labeling using the methods recommended by the current IUPAC/IUBMB/IUPAB Interunion Task Group	line
 * Mol_common_name	Enter the name for a component of the sample. Include molecules under study (the assembly or entities and ligands) as well as buffers salts reducing agents anti-bacterial agents etc.	line	yes
 * Sample_ID	Pointer to '_Sample.ID'	int	yes
 * Sf_ID	Pointer to '_Sample.Sf_ID'	int	yes
 *
 * */
        if (molecule.get() == null) {
            return;
        }
        chan.write("save_sample_"+getName()+"\n");
        chan.write("_Sample.ID                          ");
        chan.write(getId() + "\n");
        chan.write("_Sample.Name                        ");
        chan.write(getName() + "\n");
        chan.write("_Sample.Sf_category                 ");
        chan.write("sample\n");
        chan.write("_Sample.Sf_framecode                ");
        chan.write("sample_"+getName()+"\n");
        chan.write("_Sample.Type                        ");
        chan.write(".\n");
        chan.write("\n");

        //fixme: handle multiple components with independent labeling. Reinstate IsotopeLabels class
        // and replace "molecule" column of table with component, splitting node into number of entities, each with independent labelString

        chan.write("_loop\n");
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
            chan.write(String.format("%d %d %s %s",getId(),entityID,entity.label,getLabelString().equals("")?"*":getLabelString()));
            chan.write("\n");
            entityID++;
        }
        chan.write("stop_\n");
        chan.write("save_\n\n");
    }

    public static void writeAllStar3 (FileWriter chan) throws IOException {
        for (Sample sample : UmbcProject.getActive().sampleList) {
            for (Acquisition acquisition : UmbcProject.getActive().acquisitionTable) {
                if (acquisition.getSample()==sample) {
                    sample.writeStar3(chan);
                    break;
                }
            }

        }
    }
}
