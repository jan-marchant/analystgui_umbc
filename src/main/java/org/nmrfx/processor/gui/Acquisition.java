package org.nmrfx.processor.gui;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.processor.datasets.peaks.AtomResonance;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakDim;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.search.MNode;
import org.nmrfx.structure.chemistry.search.MTree;
import org.nmrfx.utils.GUIUtils;

import java.util.*;

public class Acquisition {

    private UmbcProject project;
    private ObservableList<ManagedList> managedListsList = FXCollections.observableArrayList();
    private ObservableList<Experiment> validExperimentList = FXCollections.observableArrayList();
    private ObjectProperty<Dataset> dataset = new SimpleObjectProperty<>();
    private ObjectProperty<Experiment> experiment = new SimpleObjectProperty<>();
    private ObjectProperty<Sample> sample = new SimpleObjectProperty<>();
    private ListProperty<ManagedList> managedLists = new SimpleListProperty<>(managedListsList);
    private ListProperty<Experiment> validExperiments = new SimpleListProperty<>(validExperimentList);
    private Double sensitivity;
    private HashMap<Atom,Float> atomPercent;
    //think about these implementing some kind of interface
    private MTree acquisitionTree;
    private HashMap<ExpDim, HashMap<Atom, MNode>> dimAtomNodeMap = new HashMap<>();
    private HashMap<ExpDim, Set<MNode>> dimNodeMap = new HashMap<>();
    private MNode firstNode;
    private MNode lastNode;


    public Acquisition() {
        project=UmbcProject.getActive();
        project.acquisitionTable.add(this);
        dataset.addListener((observableValue, oldValue, newValue) -> parseValidExperiments());
        UmbcProject.experimentList.addListener((ListChangeListener.Change<? extends Experiment> c) -> parseValidExperiments());
    }

    private void parseValidExperiments() {
        Dataset theDataset=getDataset();
        validExperimentList.clear();
        if (theDataset==null) {
            setExperiment(null);
            return;
        }
        ArrayList<Nuclei> nuclei = new ArrayList<>();
        int nDim = theDataset.getNDim();
        for (int i=0;i<nDim;i++) {
            nuclei.add(theDataset.getNucleus(i));
        }

        for (Experiment experiment : UmbcProject.experimentList) {
            ArrayList<Nuclei> experimentNuc = (ArrayList) nuclei.clone();
            if (experiment.getNumObsDims()==nDim) {
                for (ExpDim expDim : experiment.expDims) {
                    experimentNuc.remove(expDim.getNucleus());
                }
                if (experimentNuc.isEmpty()) {
                    validExperimentList.add(experiment);
                }
            }
        }
        if (!validExperimentList.contains(getExperiment())) {
            setExperiment(null);
        }
    }

    public void addNewManagedList() {
        if (getDataset()==null || getSample()==null || getExperiment()==null) {
            GUIUtils.warn("Cannot add list","You must define all acquisition parameters before adding any lists.");
        }
        ManagedListSetup managedListSetup = new ManagedListSetup(this);

    }

    public void deleteManagedList(ManagedList managedList,boolean prompt) {
        boolean delete=true;
        if (prompt) {
            delete = GUIUtils.affirm("Are you sure? This cannot be undone.");
        }
        if (delete) {
            getManagedLists().remove(managedList);
            managedList.remove();
        }
    }

    public void remove(boolean prompt) {
        boolean delete=true;
        if (prompt) {
            delete = GUIUtils.affirm("Are you sure you want to delete? This cannot be undone.");
        }
        if (delete) {
            project.acquisitionTable.remove(this);
        }
    }

    private double getDatasetSensitivity () {
        return 0.0;
    }

    public Dataset getDataset() {
        return dataset.get();
    }

    public ObjectProperty<Dataset> datasetProperty() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset.set(dataset);
    }

    public Experiment getExperiment() {
        return experiment.get();
    }

    public ObjectProperty<Experiment> experimentProperty() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment.set(experiment);
    }

    public Sample getSample() {
        return sample.get();
    }

    public ObjectProperty<Sample> sampleProperty() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample.set(sample);
    }

    public ObservableList<ManagedList> getManagedLists() {
        return managedLists.get();
    }

    public ListProperty<ManagedList> managedListsProperty() {
        return managedLists;
    }

    public void setManagedLists(ObservableList<ManagedList> list) {
        this.managedLists.set(list);
    }

    public Double getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(Double sensitivity) {
        this.sensitivity = sensitivity;
    }

    public ObservableList<Experiment> getValidExperimentList() {
        return validExperimentList;
    }

    //fixme: the below should also account for experiment. Need an experiment version of getPeakPercent
    // move this into sample
    public float getPeakPercent(Peak peak) {
        //TODO: support labeling schemes like A28/Ar (i.e. should not see A2/8-Ar NOEs from same residue in same molecule
        // means subtle difference in label string parsing

        //TODO: Add GUI support for setting labeling percent
        Float total=null;
        float atomFraction;
        AtomResonance resonance;
        for (PeakDim peakDim : peak.getPeakDims()) {
            resonance = (AtomResonance) peakDim.getResonance();
            atomFraction=(float) getAtomPercent(resonance.getAtom()) /100;
            if (total==null) {
                total=atomFraction;
            } else {
                total*=atomFraction;
            }
        }
        return total==null?0:100*total;
    }

    public float getAtomPercent (Atom atom) {
        if (atom!=null) {
            float percent;
            try {
                percent = atomPercent.get(atom);
            } catch (Exception e) {
                percent = getSample().getAtomPercent(atom);
                if (percent>100) {
                    System.out.println("Check labeling string - "+atom.getName()+" is apparently "+percent+"% labeled");
                    percent=100;
                }
                atomPercent.put(atom, percent);
            }
            return percent;
        } else {
            System.out.println("Couldn't find atom");
            return 0;
        }
    }

    public void addNoes(ManagedList list, Peak newPeak) {
        for (ExpDim expDim : getExperiment().expDims) {
            if (expDim.getNextCon().type==Connectivity.TYPE.NOE) {
                Atom atom1=null;
                Atom atom2=null;
                if (expDim.isObserved()) {
                    atom1=((AtomResonance) newPeak.getPeakDim(list.getDimMap().get(expDim)).getResonance()).getAtom();
                }
                if (expDim.getNextExpDim().isObserved()) {
                    atom2=((AtomResonance) newPeak.getPeakDim(list.getDimMap().get(expDim)).getResonance()).getAtom();
                }
                boolean add=true;
                if (atom1==null || atom2 == null) {
                    add=false;
                    //TODO: popup window asking for assignments and parse them
                    // might be useful to implement backwards link to give context (/ limit choices?)
                    //add=response from popup
                }
                if (add) {
                    //fixme: what is newScale for?
                    if (list.noeSet.getConstraints(atom1.getName()+" "+atom2.getName(),true).size()==0) {
                        Noe noe=new Noe(newPeak,atom1.getSpatialSet(),atom2.getSpatialSet(),1.0);
                        list.setAddedNoe(noe);
                        list.noeSet.add(noe);
                    }
                }
            }
        }
    }

    public void resetAcquisitionTree() {
        acquisitionTree=null;
    }

    public MTree getAcquisitionTree() {
        //acquisitionTree nodes (atoms) are contained within generations (expDims) and have edges to connected
        //in the preceding (backward) and following (forward) generations. The weight of each edge is given by the
        //labeling fraction of the first node in the forward direction. If the weight is 0 then the node is not added.
        //
        if (acquisitionTree!=null) {
            return acquisitionTree;
        }
        acquisitionTree=new MTree();

        firstNode =acquisitionTree.addNode();
        lastNode =acquisitionTree.addNode();

        dimAtomNodeMap.clear();
        dimNodeMap.clear();

        boolean firstDim=true;
        //populate generations
        for (ExpDim expDim : getExperiment().expDims) {
            HashMap<Atom, MNode> atomNode = new HashMap<>();
            Set<MNode> nodeSet = new HashSet<>();
            for (Atom atom : expDim.getActiveAtoms(getSample().getMolecule())) {
                double fraction=getSample().getAtomFraction(atom);
                if (fraction>0) {
                    MNode mNode = acquisitionTree.addNode();
                    mNode.setAtom(atom);
                    atomNode.put(atom, mNode);
                    nodeSet.add(mNode);
                    if (firstDim) {
                        mNode.backwardWeightedEdges.put(firstNode,1.0);
                        firstNode.forwardWeightedEdges.put(mNode,1.0);
                    }
                    if (expDim.getNextExpDim()==null) {
                            mNode.forwardWeightedEdges.put(lastNode, fraction);
                            lastNode.backwardWeightedEdges.put(mNode,fraction);
                    }
                }
            }
            dimAtomNodeMap.put(expDim, atomNode);
            dimNodeMap.put(expDim, nodeSet);
        }
        //populate edges
        for (ExpDim expDim : getExperiment().expDims) {
            for (MNode mNode : dimNodeMap.get(expDim)) {
                Atom atom=mNode.getAtom();
                double fraction=getSample().getAtomFraction(atom);
                for (Atom connectedAtom : expDim.getConnected(mNode.getAtom())) {
                    MNode connectedNode = dimAtomNodeMap.get(expDim.getNextExpDim()).get(connectedAtom);
                    if (connectedNode != null) {
                        mNode.forwardWeightedEdges.put(connectedNode,fraction);
                        connectedNode.backwardWeightedEdges.put(mNode,fraction);
                    }
                }
            }
        }
        return acquisitionTree;
    }

    //Probably not needed

    public MNode getFirstNode() {
        return firstNode;
    }

    public MNode getLastNode() {
        return lastNode;
    }

    public HashMap<ExpDim, HashMap<Atom, MNode>> getDimAtomNodeMap() {
        return dimAtomNodeMap;
    }

    public HashMap<ExpDim, Set<MNode>> getDimNodeMap() {
        return dimNodeMap;
    }



/*
    public MTree getAcquisitionTree () {
        //acquisition tree nodes are weighted according to the label scheme of the sample
        if (acquisitionTree!=null) {
            return acquisitionTree;
        }
        acquisitionTree=getExperiment().getExperimentTree().copy();
        //private HashMap<ExpDim, HashMap<Atom, MNode>> dimAtomNodeMap = new HashMap<>();
        //private HashMap<ExpDim, Set<MNode>> dimNodeMap = new HashMap<>();

        for (ExpDim expDim : getExperiment().expDims) {
            for (MNode mNode : getExperiment().getDimNodeMap().get(expDim)) {
                Atom atom=mNode.getAtom();
                double fraction=getSample().getAtomFraction(atom);
                if (fraction>0) {
                    mNode.weightedEdges.replaceAll((k, v) -> v *= fraction);
                    //add to atomNodeMap and dimNodeMap for acquisition
                }
            }
        }
        for (MNode node : acquisitionTree.nodes) {
            double fraction=getSample().getAtomFraction(node.getAtom());
            if (fraction==0) {

            } else {
                node.weightedEdges.replaceAll((k, v) -> v *= fraction);
            }
        }
        return acquisitionTree;
    }

    public void updateTree(Noe noe) {
        //when an NOE is added to the NoeSet being watched by this acquisition, work out which additional connections to make. Then work out all additional peaks needed and add them? But the NoeSet is a fn of the peaklist
        // not the acquisition. The acquisition tree will remain the same. Hmm. PeakList tree is a copy of the acquisition tree! PeakList watches the NoeSet changes! Acquisition tree never updates I guess. PeakList tree knows its sample so can update.
    }
 */
}
