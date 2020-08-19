package org.nmrfx.project;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.gui.*;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.utils.GUIUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UmbcProject extends GUIStructureProject {
    public ObservableList<Acquisition> acquisitionTable = FXCollections.observableArrayList();
    public ObservableList<Sample> sampleList = FXCollections.observableArrayList();
    public ObservableList<Condition> conditionList = FXCollections.observableArrayList();
    public ObservableList<UmbcProject> subProjectList = FXCollections.observableArrayList();
    public static HashMap<Molecule,MoleculeCouplingList> moleculeCouplingMap= new HashMap<>();

    public static ObservableList<Acquisition> gAcquisitionTable = FXCollections.observableArrayList();
    public static ObservableList<Dataset> gDatasetList = FXCollections.observableArrayList();
    public static ObservableList<Sample> gSampleList = FXCollections.observableArrayList();
    public static ObservableList<Condition> gConditionList = FXCollections.observableArrayList();
    public static ObservableList<Experiment> experimentList = FXCollections.observableArrayList();

    public HashMap<Project, BidiMap<Entity,Entity>> entityMap = new HashMap<>();

    public UmbcProject(String name) {
        super(name);
        Bindings.bindContent(gAcquisitionTable,acquisitionTable);
        Bindings.bindContent(gDatasetList,(ObservableList) datasets);
        Bindings.bindContent(gSampleList,sampleList);
        Bindings.bindContent(gConditionList,conditionList);
        //acquisitionTable.addListener(acquisitionChangeListener);
        //NoeSet noeSet=new NoeSet("Set 1");
        //NOE_SETS.put("Set 1",noeSet);
    }

    public static MoleculeCouplingList getMoleculeCouplingList(Molecule molecule) {
        if (!moleculeCouplingMap.containsKey(molecule)) {
            moleculeCouplingMap.put(molecule,new MoleculeCouplingList((molecule)));
        }
        return moleculeCouplingMap.get(molecule);
    }

    public static Set<String> getLabelSet () {
        Set<String> labelSet = new HashSet<>();
        for (Dataset d : gDatasetList) {
            for (int i=0;i<d.getNDim();i++) {
                labelSet.add(d.getLabel(i));
            }
        }
        return labelSet;
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
            Bindings.unbindContent(gDatasetList, (ObservableList) UmbcProject.getActive().datasets);
            Bindings.unbindContent(gSampleList, UmbcProject.getActive().sampleList);
            Bindings.unbindContent(gConditionList, UmbcProject.getActive().conditionList);
        }
        super.setActive();
        if (acquisitionTable!=null) {
            Bindings.bindContent(gAcquisitionTable,acquisitionTable);
            Bindings.bindContent(gDatasetList,(ObservableList) datasets);
            Bindings.bindContent(gSampleList,sampleList);
            Bindings.bindContent(gConditionList,conditionList);
        }
    }

    public static UmbcProject replace(String name, StructureProject project) {
        UmbcProject newProject = new UmbcProject(name);
        newProject.projectDir = project.projectDir;
        newProject.molecules.putAll(project.molecules);
        project.molecules.clear();
        newProject.peakLists.putAll(project.peakLists);
        project.peakLists.clear();
        for (String datasetName : project.datasetMap.keySet()) {
            newProject.datasetMap.put(datasetName,project.datasetMap.get(datasetName));
        }
        newProject.peakLists=project.peakLists;
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

    @Override
    public Dataset getDataset(String name) {
        Dataset dataset = datasetMap.get(name);
        if (dataset != null) {
            return dataset;
        } else {
            for (UmbcProject subProj : subProjectList) {
                dataset = subProj.getDataset(name);
                if (dataset != null) {
                    return dataset;
                }
            }
        }
        return null;
    }


    public void writeSubProjectsStar3(FileWriter chan) throws IOException {
        if (subProjectList.size()<=0) {
            return;
        }
        chan.write("\n\n");
        chan.write("    ####################################\n");
        chan.write("    #      Associated assemblies       #\n");
        chan.write("    ####################################\n");
        chan.write("\n\n");

        int id=0;

        for (UmbcProject project : subProjectList) {
            project.saveProject();
            Path relativePath;
            try {
                relativePath = projectDir.relativize(project.projectDir);
            } catch (Exception e) {
                relativePath = project.projectDir;
            }
            String label = project.name;
            chan.write("save_" + label + "\n");
            chan.write("_Assembly_subsystem.Sf_category                 ");
            chan.write("assembly_subsystems\n");
            chan.write("_Assembly_subsystem.Sf_framecode                ");
            chan.write("save_" + label + "\n");
            chan.write("_Assembly_subsystem.ID                          ");
            chan.write(id+"\n");
            chan.write("_Assembly_subsystem.Name                        ");
            chan.write("'"+label+"'\n");
            chan.write("_Assembly_subsystem.Details                     ");
            chan.write("'"+relativePath.toString()+"'\n");

            //fixme: this is not an "official" STAR category.
            // using names for fear that IDs aren't persistent
            // (e.g. if subproject edited).
            chan.write("loop_\n");
            chan.write("_Entity_map.Assembly_subsystem_ID\n");
            chan.write("_Entity_map.Active_system\n");
            chan.write("_Entity_map.Sub_system\n");
            chan.write("\n");

            //All this just to write them in residue order...
            List<Entity> seen=new ArrayList<>();
            for (Entity entity : activeMol.getEntities()) {
                if (entity instanceof Polymer) {
                    for (Residue residue : ((Polymer) entity).getResidues()) {
                        if (entityMap.containsKey(project)) {
                            if (entityMap.get(project).containsKey(residue)) {
                                chan.write(String.format("%d %s %s\n", id, residue.toString(), entityMap.get(project).get(residue).toString()));
                                seen.add(residue);
                            }
                        }
                    }
                }
            }
            for (Map.Entry<Entity,Entity> entry : entityMap.get(project).entrySet()) {
                if (!seen.contains(entry.getKey())) {
                    chan.write(String.format("%d %s %s\n", id, entry.getKey().toString(), entry.getValue().toString()));
                }
            }

            chan.write("stop_\n");
            chan.write("save_\n\n");
            id++;
        }
        chan.write("\n\n");
    }

    public void addSubProject(String projectName, String projectPath,
                              List<String> activeEntities, List<String> subEntities) {
        String absolute;
        try {
            File parentDir = new File(projectDir.toString());
            File childDir = new File(parentDir, projectPath);
            absolute = childDir.getCanonicalPath();
        } catch (Exception e) {
            absolute = projectPath;
        }

        Path projPath=Paths.get(absolute);
        UmbcProject subProj=addSubProject(projectName,projPath);
        if (subProj!=null) {
            BidiMap<Entity, Entity> map = new DualHashBidiMap<>();
            entityMap.put(subProj, map);
            for (int i = 0; i < activeEntities.size(); i++) {
                map.put(activeMol.getEntitiesAndResidues(activeEntities.get(i)), subProj.activeMol.getEntitiesAndResidues(subEntities.get(i)));
            }
        }
    }

    public UmbcProject findSubProject(Path path) {
        if (getDirectory().equals(path)) {
            return this;
        }
        for (UmbcProject subProject : subProjectList) {
            UmbcProject found=subProject.findSubProject(path);
            if (found!=null) {
                return found;
            }
        }
        return null;
    }

    public UmbcProject addSubProject(Path projectPath) {
        if (projectPath==null) {
            return null;
        }
        UmbcProject found=findSubProject(projectPath);
        if (found!=null) {
            if (getDirectory().equals(projectPath)) {
                GUIUtils.warn("Error","Cannot add project as a subProject of itself");
                return null;
            } else {
                return found;
            }
        }
        String projectName = projectPath.getFileName().toString();
        return addSubProject(projectName,projectPath);
    }

    public UmbcProject addSubProject(String name, Path path) {
        UmbcProject subProj;
        try {
            subProj = new UmbcProject(name);
            this.setActive();
            subProj.loadGUIProject(path);
            this.subProjectList.add(subProj);
        } catch (Exception e) {
            subProj=null;
        }
        return subProj;

    }

    public List<Object> getSubProjMenus(SubProjMenu controller) {
        List<Object> menus=new ArrayList<>();
        for (UmbcProject subProj : subProjectList) {
            if (subProj.subProjectList.size()>0) {
                Menu menu = new Menu(subProj.name);
                for (Object subMenu : subProj.getSubProjMenus(controller)) {
                    if (subMenu instanceof Menu) {
                        menu.getItems().add((Menu) subMenu);
                    } else {
                        menu.getItems().add((MenuItem) subMenu);
                    }
                }
                menus.add(menu);
                menu.setOnAction(e -> controller.setSubProject(subProj));
            } else {
                MenuItem menu = new MenuItem(subProj.name);
                menus.add(menu);
                menu.setOnAction(e -> controller.setSubProject(subProj));
            }
        }
        return menus;
    }

    public boolean containsSubProjectPath(Path path) {
        if (getDirectory().equals(path)) {
            return true;
        }
        for (UmbcProject subProject : subProjectList) {
            if (subProject.containsSubProjectPath(path)) {
                return true;
            }
        }
        return false;
    }

    public List<Acquisition> getAcquisitions(Atom atom) {
        List<Acquisition> acquisitions = new ArrayList<>();
        if (atom==null) {
            return acquisitions;
        }
        for (Acquisition acquisition : acquisitionTable) {
            if (acquisition.getAcqTree().getNodes(atom).size()>0) {
                acquisitions.add(acquisition);
            }
        }
        for (UmbcProject subProj : subProjectList) {
            if (entityMap.containsKey(atom.getTopEntity())) {
                Entity res = entityMap.get(atom.getTopEntity()).get(atom.getEntity());
                for (Atom atom1 : res.getAtoms()) {
                    if (atom1.getName().equals(atom.getName())) {
                        acquisitions.addAll(subProj.getAcquisitions(atom1));
                    }
                }
            }
        }
        return acquisitions;
    }
}
