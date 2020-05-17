package org.nmrfx.project;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.gui.*;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.Polymer;
import org.nmrfx.structure.chemistry.SmithWaterman;
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

    public HashMap<Project,HashMap<Entity,Entity>> entityMap = new HashMap<>();


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

        for (Project project : subProjectList) {
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
            chan.write("loop_\n");
            chan.write("_Entity_map.Assembly_subsystem_ID\n");
            chan.write("_Entity_map.Active_system\n");
            chan.write("_Entity_map.Sub_system\n");
            chan.write("\n");
            for (Map.Entry<Entity,Entity> entry : entityMap.get(project).entrySet()) {
                chan.write(String.format("%d %s %s",id,entry.getKey().getName(),entry.getValue().getName()));
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
            HashMap<Entity, Entity> map = new HashMap<>();
            entityMap.put(subProj, map);
            for (int i = 0; i < activeEntities.size(); i++) {
                map.put(activeMol.getEntity(activeEntities.get(i)), subProj.activeMol.getEntity(subEntities.get(i)));
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


    public List<Object> getSubProjMenus(SubProjectSceneController controller) {
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

    public void alignEntities(UmbcProject subProj,HashMap<Entity,Entity> map) {
        //really need to set up this mapping graphically - one to one entity mapping and then show aligner
        for (Entity entity : activeMol.getEntities()) {
            if (entity instanceof Polymer) {
                for (Entity entity2 : subProj.activeMol.getEntities()) {
                    if (entity2 instanceof Polymer && ((Polymer) entity).getPolymerType()==((Polymer) entity2).getPolymerType()) {
                        SmithWaterman aligner=new SmithWaterman(((Polymer) entity).getOneLetterCode(),((Polymer) entity2).getOneLetterCode());
                        aligner.buildMatrix();
                        aligner.dumpH();
                        aligner.processMatrix();
                        for (int i=0;i<aligner.getA().size();i++) {
                            map.put(((Polymer) entity).getResidues().get(aligner.getA().get(i)),((Polymer) entity2).getResidues().get(aligner.getB().get(i)));
                        }
                    }
                }
            }
        }
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
}
