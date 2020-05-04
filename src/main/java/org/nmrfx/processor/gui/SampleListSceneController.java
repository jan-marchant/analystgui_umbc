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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.RNALabels;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SampleListSceneController implements Initializable {

    //static final DecimalFormat formatter = new DecimalFormat();
    //public UmbcProject project;
    private Stage stage;

    @FXML
    private TableView<Sample> tableView;
    @FXML
    Button addButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
    }

    public Stage getStage() {
        return stage;
    }

    public static SampleListSceneController create() {
        FXMLLoader loader = new FXMLLoader(SampleListSceneController.class.getResource("/SampleListScene.fxml"));
        SampleListSceneController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.getController();
            controller.stage = stage;
            //controller.project=UmbcProject.getActive();
            stage.setTitle("Samples");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    void initTable() {
        tableView.setEditable(true);

        TableColumn<Sample, String> nameCol = new TableColumn<>("ID");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        //moleculeCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));
        nameCol.setPrefWidth(200);

        TableColumn<Sample, Molecule> moleculeCol = new TableColumn<>("Molecule");
        moleculeCol.setCellValueFactory(cellData -> cellData.getValue().moleculeProperty());
        //moleculeCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));
        moleculeCol.setPrefWidth(200);

        TableColumn<Sample, IsotopeLabels> labelsCol = new TableColumn<>("Labeling");
        labelsCol.setCellValueFactory(cellData -> cellData.getValue().labelsProperty());
        //labelsCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gObsDatasetList));
        labelsCol.setPrefWidth(200);

        TableColumn<Sample, Condition> conditionCol = new TableColumn<>("Condition");
        conditionCol.setCellValueFactory(cellData -> cellData.getValue().conditionProperty());
        //labelsCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gObsDatasetList));
        conditionCol.setPrefWidth(200);

        tableView.getColumns().setAll(nameCol,conditionCol,moleculeCol,labelsCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setOnKeyPressed( new EventHandler<KeyEvent>()
        {
            @Override
            public void handle( final KeyEvent keyEvent )
            {
                final Sample selectedItem = tableView.getSelectionModel().getSelectedItem();

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
                Sample.addNew();
            }
        });

    }

    public void setSampleList(ObservableList<Sample> samples) {
        if (tableView == null) {
            System.out.println("null table");
        } else {
            tableView.setItems(samples);
        }

    }

    void refresh() {
        tableView.refresh();
    }

}
