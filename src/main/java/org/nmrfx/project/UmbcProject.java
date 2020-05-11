package org.nmrfx.project;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.processor.gui.*;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.util.HashMap;

public class UmbcProject extends GUIStructureProject {
    public ObservableList<Acquisition> acquisitionTable = FXCollections.observableArrayList();
    public ObservableList<Dataset> obsDatasetList = FXCollections.observableArrayList();
    public ObservableList<Sample> sampleList = FXCollections.observableArrayList();
    public static HashMap<Molecule,MoleculeCouplingList> moleculeCouplingMap= new HashMap<>();
    public HashMap<String,NoeSet> noeSetMap = new HashMap<>();

    private MapChangeListener<String, Dataset> datasetChangeListener = (MapChangeListener.Change<? extends String, ? extends Dataset> c) -> {
            obsDatasetList.clear();
            obsDatasetList.addAll(UmbcProject.getActive().datasetList.values());
    };

    /*private ListChangeListener<Acquisition> acquisitionChangeListener = (ListChangeListener.Change<? extends Acquisition> c) -> {
            gAcquisitionTable=UmbcProject.getActive().acquisitionTable;
    };*/

    public static ObservableList<Acquisition> gAcquisitionTable = FXCollections.observableArrayList();
    public static ObservableList<Dataset> gObsDatasetList = FXCollections.observableArrayList();
    public static ObservableList<Sample> gSampleList = FXCollections.observableArrayList();
    public static ObservableList<Experiment> experimentList = FXCollections.observableArrayList();


    public UmbcProject(String name) {
        super(name);
        datasetList.addListener(datasetChangeListener);
        Bindings.bindContent(gAcquisitionTable,acquisitionTable);
        Bindings.bindContent(gObsDatasetList,obsDatasetList);
        Bindings.bindContent(gSampleList,sampleList);
        //acquisitionTable.addListener(acquisitionChangeListener);
        NoeSet noeSet=new NoeSet("Set 1");
        noeSetMap.put("Set 1",noeSet);
    }

    public static MoleculeCouplingList getMoleculeCouplingList(Molecule molecule) {
        if (!moleculeCouplingMap.containsKey(molecule)) {
            moleculeCouplingMap.put(molecule,new MoleculeCouplingList((molecule)));
        }
        return moleculeCouplingMap.get(molecule);
    }


    /*public static NoeSet getNoeSet(Molecule molecule, Connectivity.NOETYPE noeType, int ppmSet) {
        String key=molecule.toString()+noeType.toString()+ppmSet;
        if (!noeSetMap.containsKey(key)) {
            NoeSet noeSet=new NoeSet(noeType.toString(),molecule,ppmSet);
            noeSetMap.put(key,noeSet);
        }
        return noeSetMap.get(key);
    }*/

    public static UmbcProject getActive() {
        Project project = Project.getActive();
        if ((project != null) && !(project instanceof UmbcProject)) {
            project = replace(project.name, (StructureProject) project);
        }

        if (project == null) {
            project = new UmbcProject("Untitled 1");
        }
        return (UmbcProject) project;
    }

    @Override
    public void setActive() {
        if (Project.activeProject instanceof UmbcProject) {
            Bindings.unbindContent(gAcquisitionTable, UmbcProject.getActive().acquisitionTable);
            Bindings.unbindContent(gObsDatasetList, UmbcProject.getActive().obsDatasetList);
            Bindings.unbindContent(gSampleList, UmbcProject.getActive().sampleList);
        }
        super.setActive();
        if (acquisitionTable!=null) {
            Bindings.bindContent(gAcquisitionTable,acquisitionTable);
            Bindings.bindContent(gObsDatasetList,obsDatasetList);
            Bindings.bindContent(gSampleList,sampleList);
        }
    }

    public static UmbcProject replace(String name, StructureProject project) {
        UmbcProject newProject = new UmbcProject(name);
        newProject.projectDir = project.projectDir;
        newProject.molecules.putAll(project.molecules);
        project.molecules.clear();
        newProject.peakLists.putAll(project.peakLists);
        project.peakLists.clear();
        newProject.datasetList=project.datasetList;
        newProject.peakListTable=project.peakListTable;
        newProject.resFactory=project.resFactory;
        newProject.peakPaths=project.peakPaths;
        newProject.compoundMap.putAll(project.compoundMap);
        newProject.activeMol = project.activeMol;
        newProject.NOE_SETS.putAll(project.NOE_SETS);
        newProject.ACTIVE_SET = project.ACTIVE_SET;
        newProject.angleSets = project.angleSets;
        newProject.activeSet = project.activeSet;
        newProject.rdcSets = project.rdcSets;
        newProject.activeRDCSet = project.activeRDCSet;
        return newProject;
    }

}
