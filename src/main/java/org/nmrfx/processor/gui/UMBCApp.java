package org.nmrfx.processor.gui;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.PeakLabeller;
import org.nmrfx.processor.gui.spectra.KeyBindings;

public class UMBCApp extends AnalystApp {
    public static RNAManagedListSceneController rnaManListController;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        interpreter.exec("import org.nmrfx.processor.gui.LabelDataset as ld");
    }
    @Override
    MenuBar makeMenuBar(String appName) {
        MenuBar myMenuBar=super.makeMenuBar(appName);
        Menu umbcMenu = new Menu("UMBC");
        MenuItem rnaManListMenuItem = new MenuItem("Show RNA Managed Lists");
        rnaManListMenuItem.setOnAction(e -> showRNAManagedList(e));
        umbcMenu.getItems().addAll(rnaManListMenuItem);
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

    @FXML
    private void showRNAManagedList(ActionEvent event) {
        if (rnaManListController == null) {
            rnaManListController = RNAManagedListSceneController.create();
            rnaManListController.setDatasetList(LabelDataset.labelDatasetTable);
        }
        if (rnaManListController != null) {
            rnaManListController.getStage().show();
            rnaManListController.getStage().toFront();
        } else {
            System.out.println("Couldn't make rnaManListController ");
        }
    }


    @Override
    public void datasetAdded(Dataset dataset) {
        if (Platform.isFxApplicationThread()) {
            FXMLController.updateDatasetList();
            LabelDataset.parseNew(dataset);
        } else {
            Platform.runLater(() -> {
                        FXMLController.updateDatasetList();
                        LabelDataset.parseNew(dataset);
                    }
            );
        }
    }
}
