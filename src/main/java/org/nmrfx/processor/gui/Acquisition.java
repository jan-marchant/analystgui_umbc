package org.nmrfx.processor.gui;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.controlsfx.dialog.ExceptionDialog;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.processor.datasets.peaks.*;
import org.nmrfx.processor.operations.Exp;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Acquisition {
    //todo: condition should be parameter of acquisition, not sample

    private UmbcProject project;
    private ObservableList<ManagedList> managedListsList = FXCollections.observableArrayList();
    private ObservableList<Experiment> validExperimentList = FXCollections.observableArrayList();
    private ObjectProperty<Dataset> dataset = new SimpleObjectProperty<>();
    private ObjectProperty<Experiment> experiment = new SimpleObjectProperty<>();
    private ObjectProperty<Sample> sample = new SimpleObjectProperty<>();
    private ObjectProperty<Condition> condition = new SimpleObjectProperty<>();

    private ListProperty<ManagedList> managedLists = new SimpleListProperty<>(managedListsList);
    private ListProperty<Experiment> validExperiments = new SimpleListProperty<>(validExperimentList);
    private Double sensitivity;
    private AcqTree acqTree;
    private ArrayList<Float> defaultPeakWidths = new ArrayList<>();

    public Float getDefaultPeakWidth(int i) {
        if (defaultPeakWidths.size() < getDataset().getNDim()) {
            updateDefaultPeakWidths();
        }
        return defaultPeakWidths.get(i);
    }

    public Acquisition() {
        project=UmbcProject.getActive();
        project.acquisitionTable.add(this);
        dataset.addListener((observableValue, oldValue, newValue) -> {
            parseValidExperiments();
            updateDefaultPeakWidths();
        });
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

    public void attachManagedList(ManagedList list) {
        managedLists.get().add(list);
    }

    public void addNewManagedList() {
        if (getDataset()==null || getSample()==null || getExperiment()==null) {
            GUIUtils.warn("Cannot add list","You must define all acquisition parameters before adding any lists.");
            return;
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

    public Condition getCondition() {
        return condition.get();
    }

    public ObjectProperty<Condition> conditionProperty() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition.set(condition);
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

    public List<ManagedPeak> addNoes(ManagedList list, Peak newPeak) {
        List<ManagedPeak> addedPeaks = new ArrayList<>();

        for (ExpDim expDim : getExperiment().expDims) {
            if (expDim.getNextCon() != null && (expDim.getNextCon().type == Connectivity.TYPE.NOE)) {
                Atom atom1 = null;
                Atom atom2 = null;
                AtomResonance resonance1 = null;
                AtomResonance resonance2 = null;
                if (expDim.isObserved()) {
                    resonance1=(AtomResonance) newPeak.getPeakDim(list.getDimMap().get(expDim)).getResonance();
                    atom1 = resonance1.getAtom();
                }
                if (expDim.getNextExpDim().isObserved()) {
                    resonance2=(AtomResonance) newPeak.getPeakDim(list.getDimMap().get(expDim.getNextExpDim())).getResonance();
                    atom2 = resonance2.getAtom();
                }
                boolean add = true;
                if (atom1 == null || atom2 == null) {
                    add = false;
                    //TODO: popup window asking for assignments and parse them
                    // might be useful to implement backwards link to give context (/ limit choices?)
                    //add=response from popup
                }
                double noeFraction = getSample().getAtomFraction(atom1) * getSample().getAtomFraction(atom2);
                if (noeFraction <= 0) {
                    add = false;
                }
                if (!expDim.resPatMatches(atom1,atom2)) {
                    add=false;
                }
                if (add) {
                    if (!noeExists(list.noeSet, resonance1,resonance2)) {
                        //fixme: is this an OK use of newScale?
                        AcqNode node1 = acqTree.getNode(expDim, atom1);
                        AcqNode node2 = acqTree.getNode(expDim.getNextExpDim(), atom2);
                        if (node1 != null && node2 != null) {
                            Noe noe = new Noe(newPeak, atom1.getSpatialSet(), atom2.getSpatialSet(), noeFraction,resonance1,resonance2);
                            noe.setIntensity(newPeak.getIntensity());
                            list.setAddedNoe(noe);
                            list.noeSet.add(noe);
                            addedPeaks.addAll(list.addNoeToList(noe));
                            if (addedPeaks.size() > 0) {
                                noe.peak = addedPeaks.get(addedPeaks.size() - 1);
                            } else {
                                //TODO: implement peakAssign box filter on peakListTree dimNodeMap - then this shouldn't be reached
                                list.noeSet.get().remove(noe);
                            }
                        }
                    }
                }
            }
        }
        return addedPeaks;
    }

    public boolean noeExists(NoeSet noeSet, AtomResonance resonance1,AtomResonance resonance2) {
        Atom atom1 = resonance1.getAtom();
        Atom atom2 = resonance2.getAtom();
        for (Noe noe : noeSet.get()) {
            boolean seen1=false;
            boolean seen2=false;
            for (PeakDim testPeakDim : noe.peak.getPeakDims()) {
                Atom testAtom=((AtomResonance) testPeakDim.getResonance()).getAtom();
                //if (testAtom==atom1 && testPeakDim.getChemShiftSet()==resonance1.getChemShiftSet()) {
                //TODO: implement chemical shift sets
                if (testAtom==atom1) {
                    seen1=true;
                }
                //if (testAtom==atom2 && testPeakDim.getChemShiftSet()==resonance2.getChemShiftSet()) {
                if (testAtom==atom2) {
                    seen2=true;
                }
            }
            if (seen1 && seen2) {
                return true;
            }
        }
        return false;
    }

    public void resetAcquisitionTree() {
        acqTree =null;
    }

    public AcqTree getAcqTree() {
        //acquisitionTree nodes (atoms) are contained within generations (expDims) and have edges to connected
        //in the preceding (backward) and following (forward) generations. The weight of each edge is given by the
        //labeling fraction of the first node in the forward direction. If the weight is 0 then the node is not added.
        //
        if (acqTree !=null) {
            return acqTree;
        }
        acqTree =new AcqTree(this);

        boolean firstDim=true;
        //populate generations
        for (ExpDim expDim : getExperiment().expDims) {
            HashMap<Atom, AcqNode> atomNode = new HashMap<>();
            Set<AcqNode> nodeSet = new HashSet<>();
            for (Atom atom : expDim.getActiveAtoms(getSample().getMolecule())) {
                double fraction=getSample().getAtomFraction(atom);
                if (fraction>0) {
                    AcqNode node = acqTree.addNode(atom,expDim);
                    node.setAtom(atom);
                    atomNode.put(atom, node);
                    nodeSet.add(node);
                    if (firstDim) {
                        acqTree.addLeadingEdge(node);
                    }
                    if (expDim.getNextExpDim()==null) {
                        acqTree.addTrailingEdge(node,fraction);
                    }
                }
            }
            firstDim=false;
        }
        //populate edges
        for (AcqNode node : acqTree.getNodes()) {
            Atom atom=node.getAtom();
            if (atom!=null) {
                for (Atom connectedAtom : node.getExpDim().getConnected(atom)) {
                    AcqNode connectedNode = acqTree.getNode(node.getNextExpDim(), connectedAtom);
                    if (connectedNode != null) {
                        double weight = getSample().getAtomFraction(atom);
                        acqTree.addEdge(node, connectedNode, weight);
                    }
                }
            }
        }
        //prune nodes with no possible paths
        for (AcqNode node : acqTree.getNodes()) {
            if (acqTree.getPossiblePaths(node, node, true, new HashMap<>(), new ArrayList<>()).size()==0) {
                acqTree.removeNode(node);
            }
        }
        return acqTree;
    }

    public void setupListeners() {
        for (ManagedList list : managedLists.get()) {
            list.setupListener();
        }
    }

    public UmbcProject getProject() {
        return project;
    }

    public void writePeakConstraintLinksStar3(FileWriter chan) throws IOException {
        for (ManagedList managedList : getManagedLists()) {
            managedList.writePeakConstraintLinks(chan);
        }
    }

    public void updateDefaultPeakWidths() {
        Dataset theDataset=getDataset();
        if (theDataset==null) {
            defaultPeakWidths.clear();
            return;
        }
        int nDim = getDataset().getNDim();
        int[][] pt = new int[nDim][2];
        int[] cpt = new int[nDim];
        int[] dim = new int[nDim];
        double[] width = new double[nDim];
        for (int i = 0; i < nDim; i++) {
            dim[i] = i;
            pt[i][0] = 0;
            pt[i][1] = theDataset.getSize(i)-1;
            cpt[i] = (pt[i][0] + pt[i][1]) / 2;
            width[i] = (double) Math.abs(pt[i][0] - pt[i][1]);
        }
        //RegionData rData;
        double[] percentile = null;
        try {
            //rData = dataset.analyzeRegion(pt, cpt, width, dim);
            percentile = theDataset.getPercentile(90.0, pt, dim);
        } catch (IOException e) {
            ExceptionDialog dialog = new ExceptionDialog(e);
            dialog.showAndWait();
        }

        double value = theDataset.guessNoiseLevel() * 5.0;
        if (percentile != null) {
            if (value < percentile[0]) {
                value = percentile[0];
            }
        }
        //value is the result we would get for an auto level on the whole dataset.
        //n.b. currently only picking positive peaks
        PeakPickParameters peakPickPar = (new PeakPickParameters(theDataset, "temp_for_measure")).level(value).mode("appendregion");
        for (int i = 0; i < nDim; i++) {
            peakPickPar = peakPickPar.limit(i,pt[i][0],pt[i][1]);
        }
        PeakPicker picker = new PeakPicker(peakPickPar);
        PeakList tempPeakList = null;
        try {
            tempPeakList = picker.peakPick();
        } catch (IOException e) {
            ExceptionDialog dialog = new ExceptionDialog(e);
            dialog.showAndWait();
        }
        ArrayList<Float>[] widths = new ArrayList[nDim];
        for (int j = 0; j < nDim; j++) {
            widths[j]=new ArrayList<>();
        }
        if (tempPeakList != null) {
            for (int i = 0; i < tempPeakList.peaks().size(); i++) {
                Peak peak = tempPeakList.peaks().get(i);
                for (int j = 0; j < nDim; j++) {
                    widths[j].add(peak.getPeakDim(j).getLineWidthValue());
                }
            }
        }
        PeakList.remove("temp_for_measure");
        //(approximate) median
        for (int j = 0; j < nDim; j++) {
            Collections.sort(widths[j]);
            Float defaultWidth=widths[j].get(widths[j].size()/2);
            try {
                defaultPeakWidths.set(j,defaultWidth);
            } catch ( IndexOutOfBoundsException e ) {
                defaultPeakWidths.add(j,defaultWidth);
            }
        }
    }
}
