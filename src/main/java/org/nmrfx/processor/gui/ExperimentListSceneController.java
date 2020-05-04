/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ExperimentListSceneController implements Initializable {

    private Stage stage;

    @FXML
    private TableView<Experiment> tableView;
    @FXML
    Button addButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
    }

    public Stage getStage() {
        return stage;
    }

    public static ExperimentListSceneController create() {
        FXMLLoader loader = new FXMLLoader(ExperimentListSceneController.class.getResource("/ExperimentListScene.fxml"));
        ExperimentListSceneController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.getController();
            controller.stage = stage;
            //controller.project=UmbcProject.getActive();
            stage.setTitle("Experiments");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    void initTable() {
        tableView.setEditable(true);

        TableColumn<Experiment, String> nameCol = new TableColumn<>("name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        //dimCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"Experiment",UmbcProject.gExperimentList));
        nameCol.setPrefWidth(200);

        TableColumn<Experiment, Number> dimCol = new TableColumn<>("nDim");
        dimCol.setCellValueFactory(cellData -> cellData.getValue().numObsDimsProperty());
        //dimCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"Experiment",UmbcProject.gExperimentList));
        dimCol.setPrefWidth(200);

        //some kind of accordion to show the details of each dimension

        tableView.getColumns().setAll(nameCol,dimCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setOnKeyPressed( new EventHandler<KeyEvent>()
        {
            @Override
            public void handle( final KeyEvent keyEvent )
            {
                final Experiment selectedItem = tableView.getSelectionModel().getSelectedItem();

                if ( selectedItem != null )
                {
                    if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                    {
                        selectedItem.remove(true);
                    }
                }
            }
        } );

        addButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Experiment.addNew();
            }
        });

    }

    public void setExperimentList(ObservableList<Experiment> Experiments) {
        if (tableView == null) {
            System.out.println("null table");
        } else {
            tableView.setItems(Experiments);
        }

    }

    void refresh() {
        tableView.refresh();
    }

}
