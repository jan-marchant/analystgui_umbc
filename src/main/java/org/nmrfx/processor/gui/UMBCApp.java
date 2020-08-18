package org.nmrfx.processor.gui;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.structure.chemistry.io.MoleculeIOException;

import java.io.IOException;
import java.nio.file.Path;

public class UMBCApp extends AnalystApp {
    public static AcquisitionListSceneController acquisitionListController;
    public static SampleListSceneController sampleListController;
    public static ConditionListSceneController conditionListController;
    public static ExperimentListSceneController experimentListController;
    public static RNALabelsSceneController rnaLabelsController;
    public static SubProjectSceneController subProjectController;
    public static NoeSetup noeSetController;

    @Override
    protected void loadProject(Path path) {
        if (path != null) {
            String projectName = path.getFileName().toString();
            UmbcProject project = new UmbcProject(projectName);
            try {
                project.loadGUIProject(path);
            } catch (IOException | MoleculeIOException ex) {
                ExceptionDialog dialog = new ExceptionDialog(ex);
                dialog.showAndWait();
            }
        }

    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        Experiment.readPar("data/experiments");
        NoeSet.addSet("default");
        interpreter.exec("exec(open('/Users/jan/soft/nmrfx/script').read())");
    }
    @Override
    MenuBar makeMenuBar(String appName) {
        MenuBar myMenuBar=super.makeMenuBar(appName);
        Menu umbcMenu = new Menu("UMBC");
        MenuItem acquisitionListMenuItem = new MenuItem("Show Acquisition Details");
        acquisitionListMenuItem.setOnAction(e -> showAcquisitionList(e));
        MenuItem sampleListMenuItem = new MenuItem("Show Sample Details");
        sampleListMenuItem.setOnAction(e -> showSampleList(e));
        MenuItem conditionListMenuItem = new MenuItem("Show Condition Details");
        conditionListMenuItem.setOnAction(e -> showConditionList(e));
        MenuItem experimentListMenuItem = new MenuItem("Show Experiment Details");
        experimentListMenuItem.setOnAction(e -> showExperimentList(e));
        MenuItem subProjectMenuItem = new MenuItem("Show SubProject Details");
        subProjectMenuItem.setOnAction(e -> showSubProjects(e));
        MenuItem noeSetMenuItem = new MenuItem("Show NoeSets");
        noeSetMenuItem.setOnAction(e -> showNoeSetup(e));
        MenuItem atomBrowserMenuItem = new MenuItem("Show AtomBrowser");
        atomBrowserMenuItem.setOnAction(e -> showAtomBrowser());


        umbcMenu.getItems().addAll(acquisitionListMenuItem,sampleListMenuItem,conditionListMenuItem,experimentListMenuItem,noeSetMenuItem,subProjectMenuItem,atomBrowserMenuItem);
        myMenuBar.getMenus().addAll(umbcMenu);
        if (isMac()) {
            MenuToolkit tk = MenuToolkit.toolkit();
            tk.setGlobalMenuBar(myMenuBar);
        }
        return myMenuBar;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void showAtomBrowser() {
            FXMLController controller = FXMLController.create();
            ToolBar navBar = new ToolBar();
            controller.getBottomBox().getChildren().add(navBar);
            AtomBrowser newAtomBrowser = new AtomBrowser(controller, this::removeAtomBrowser);
            newAtomBrowser.initToolbar(navBar);
            controller.addTool(newAtomBrowser);
    }

    public void removeAtomBrowser(Object o) {
            FXMLController controller = FXMLController.getActiveController();
            AtomBrowser browser = ((AtomBrowser) controller.getTool(AtomBrowser.class));
            if (browser!=null) {
                controller.getBottomBox().getChildren().remove(browser.getToolBar());
                controller.removeTool(AtomBrowser.class);
            }
    }

    @FXML
    private void showAcquisitionList(ActionEvent event) {
        if (acquisitionListController == null) {
            acquisitionListController = AcquisitionListSceneController.create();
            acquisitionListController.setAcquisitionList(UmbcProject.gAcquisitionTable);
        }
        if (acquisitionListController != null) {
            acquisitionListController.getStage().show();
            acquisitionListController.getStage().toFront();
        } else {
            System.out.println("Couldn't make acquisitionListController ");
        }
    }

    @FXML
    private void showSubProjects(ActionEvent event) {
        if (subProjectController == null) {
            subProjectController = new SubProjectSceneController();
        }
        if (subProjectController != null) {
            subProjectController.show(300,300);
            subProjectController.getStage().toFront();
        } else {
            System.out.println("Couldn't make subProjectController ");
        }
    }

    @FXML
    private void showNoeSetup(ActionEvent event) {
        if (noeSetController == null) {
            noeSetController = new NoeSetup();
        }
        if (noeSetController != null) {
            noeSetController.show(300,300);
            noeSetController.getStage().toFront();
        } else {
            System.out.println("Couldn't make noeSetup ");
        }
    }



    @FXML
    private void showExperimentList(ActionEvent event) {
        if (experimentListController == null) {
            experimentListController = ExperimentListSceneController.create();
            experimentListController.setExperimentList(UmbcProject.experimentList);
        }
        if (experimentListController != null) {
            experimentListController.getStage().show();
            experimentListController.getStage().toFront();
        } else {
            System.out.println("Couldn't make experimentListController ");
        }
    }

    @FXML
    static void showSampleList(ActionEvent event) {
        if (sampleListController == null) {
            sampleListController = SampleListSceneController.create();
            sampleListController.setSampleList(UmbcProject.gSampleList);
        }
        if (sampleListController != null) {
            sampleListController.getStage().show();
            sampleListController.getStage().toFront();
        } else {
            System.out.println("Couldn't make sampleListController ");
        }
    }

    @FXML
    static void showConditionList(ActionEvent event) {
        if (conditionListController == null) {
            conditionListController = ConditionListSceneController.create();
            conditionListController.setConditionList(UmbcProject.gConditionList);
        }
        if (conditionListController != null) {
            conditionListController.getStage().show();
            conditionListController.getStage().toFront();
        } else {
            System.out.println("Couldn't make conditionListController ");
        }
    }
}
