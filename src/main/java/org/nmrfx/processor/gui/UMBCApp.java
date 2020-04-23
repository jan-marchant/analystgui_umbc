package org.nmrfx.processor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.nmrfx.processor.datasets.Dataset;

public class UMBCApp extends AnalystApp {
    static String appName = "NMRFx Analyst (UMBC)";
    private static MenuBar mainMenuBar = null;

    public static RNAManagedListSceneController rnaManListController;

    @Override
    public void start(Stage stage) throws Exception {
        if (mainMenuBar == null) {
            mainMenuBar = makeMenuBar(appName);
        }
        super.start(stage);
        Menu umbcMenu = new Menu("UMBC");
        MenuItem rnaManListMenuItem = new MenuItem("Show RNA Managed Lists");
        rnaManListMenuItem.setOnAction(e -> showRNAManagedList(e));
        umbcMenu.getItems().addAll(rnaManListMenuItem);
        mainMenuBar.getMenus().addAll(umbcMenu);

        interpreter.exec("import org.nmrfx.processor.gui.LabelDataset as ld");
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
