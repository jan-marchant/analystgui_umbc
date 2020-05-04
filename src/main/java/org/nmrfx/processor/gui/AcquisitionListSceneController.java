/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import com.sun.javafx.property.PropertyReference;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.utils.GUIUtils;

import javax.xml.crypto.Data;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class AcquisitionListSceneController implements Initializable {

    //static final DecimalFormat formatter = new DecimalFormat();
    public UmbcProject project;
    private Stage stage;

    @FXML
    private TableView<Acquisition> tableView;
    @FXML
    Button addButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
    }

    public Stage getStage() {
        return stage;
    }

    public static AcquisitionListSceneController create() {
        FXMLLoader loader = new FXMLLoader(AcquisitionListSceneController.class.getResource("/AcquisitionListScene.fxml"));
        AcquisitionListSceneController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.getController();
            controller.stage = stage;
            controller.project=UmbcProject.getActive();
            stage.setTitle("Acquisitions Setup");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    static class DatasetStringFieldTableCell extends TextFieldTableCell<LabelDataset, String> {

        DatasetStringFieldTableCell(StringConverter<String> converter) {
            super(converter);
        }

        @Override
        public void commitEdit(String newValue) {
            String column = getTableColumn().getText();
            LabelDataset dataset = (LabelDataset) getTableRow().getItem();
            super.commitEdit(newValue);
            switch (column) {
                case "Labeling":
                    dataset.setLabelScheme(newValue);
                    break;
                case "Managed List":
                    dataset.setManagedListName(newValue);
                    break;
                case "Condition":
                    dataset.setCondition(newValue);
                    break;
            }
        }
    }


    void initTable() {
        StringConverter<String> sConverter = new DefaultStringConverter();
        StringConverter<Dataset> dConverter = new StringConverter<Dataset>() {
            @Override
            public String toString(Dataset object) {
                if (object==null) {
                    return "";
                }
                return object.getName();
            }

            @Override
            public Dataset fromString(String string) {
                return Project.getActive().datasetList.get(string);
            }
        };
        tableView.setEditable(true);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Acquisition, Dataset> datasetCol = new TableColumn<>("Dataset");
        datasetCol.setCellValueFactory(cellData -> cellData.getValue().datasetProperty());
        datasetCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gObsDatasetList));

        //nice but combobox doesn't show until click
        // datasetCol.setCellFactory(ComboBoxTableCell.forTableColumn(dConverter, Project.getActive().datasetList.values().toArray(new Dataset[0])));
        /*datasetCol.setCellFactory(tc -> {
            ComboBox<Dataset> combo = new ComboBox();
            combo.setItems(UmbcProject.getActive().obsDatasetList);
            //combo.getItems().addAll(Project.getActive().datasetList.values());
            TableCell<Acquisition, Dataset> cell = new TableCell<Acquisition, Dataset>() {
                @Override
                protected void updateItem(Dataset dataset, boolean empty) {
                    super.updateItem(dataset, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(dataset);
                        setGraphic(combo);
                    }
                }
            };

            combo.prefWidthProperty().bind(datasetCol.widthProperty());
            combo.setOnAction(e -> {
                tableView.getItems().get(cell.getIndex()).setDataset(combo.getValue());
                }
            );
            combo.setOnMouseClicked(e -> {
                if (combo.isDisable()) {
                    GUIUtils.warn("Editing Acquisition","You must delete all managed lists for this acquisition before editing.");
                }
            });

            return cell ;
        });
        */
        //datasetCol.setCellFactory(ComboBoxTableCell.forTableColumn(Project.getActive().datasetList.values()));
        //datasetCol.setCellValueFactory(new PropertyValueFactory<>("dataset"));
        datasetCol.setPrefWidth(200);
        //datasetCol.setEditable(false);


        TableColumn<Acquisition, Sample> sampleCol = new TableColumn<>("Sample");
        sampleCol.setCellValueFactory(new PropertyValueFactory<>("sample"));
        sampleCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));

        sampleCol.setPrefWidth(150);
        //sampleCol.setEditable(false);

        TableColumn<Acquisition, Experiment> experimentCol = new TableColumn<>("Experiment");
        experimentCol.setCellValueFactory(new PropertyValueFactory<>("experiment"));
        experimentCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.experimentList));

        experimentCol.setPrefWidth(150);
        //experimentCol.setEditable(false);

        TableColumn<Acquisition, ObservableList<ManagedList>> listCol = new TableColumn<>("Associated lists");
        listCol.setCellValueFactory(new PropertyValueFactory<>("managedLists"));
        listCol.setCellFactory(col -> new TableCell<Acquisition, ObservableList<ManagedList>>() {
            @Override
            public void updateItem(ObservableList<ManagedList> managedLists, boolean empty) {
                super.updateItem(managedLists, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(managedLists.stream().map(ManagedList::getName)
                            .collect(Collectors.joining(", ")));
                }
            }
        });

        /*listCol.setCellFactory(tc -> {
            TableCell<Acquisition, ObservableList> cell = new TableCell<Acquisition, ObservableList>() {
                @Override
                protected void updateItem(ObservableList list, boolean empty) {
                    super.updateItem(list, empty);
                    if (empty) {
                        //setGraphic(null);
                    } else {
                        //combo.setValue(dataset);
                        //setGraphic(combo);
                    }
                }
            };

            return cell;
        });
         */
        listCol.setPrefWidth(350);
        //listCol.setEditable(false);

        tableView.getColumns().setAll(datasetCol, sampleCol,experimentCol, listCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        addButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Acquisition acquisition=new Acquisition();
                //open edit window with acquisition
            }
        });

    }

    public void setAcquisitionList(ObservableList<Acquisition> acquisitions) {
        if (tableView == null) {
            System.out.println("null table");
        } else {
            tableView.setItems(acquisitions);
        }

    }

    void refresh() {
        tableView.refresh();
    }

}
