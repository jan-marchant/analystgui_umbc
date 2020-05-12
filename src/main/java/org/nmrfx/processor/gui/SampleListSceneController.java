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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
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
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setPrefWidth(200);

        TableColumn<Sample, Molecule> moleculeCol = new TableColumn<>("Molecule");
        moleculeCol.setCellValueFactory(cellData -> cellData.getValue().moleculeProperty());
        //moleculeCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));
        moleculeCol.setPrefWidth(200);

        TableColumn<Sample, String> labelsCol = new TableColumn<>("Labeling");
        //labelsCol.setCellValueFactory(cellData -> cellData.getValue().labelStringProperty());
        labelsCol.setCellValueFactory(new PropertyValueFactory("labelString"));
        //labelsCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gObsDatasetList));
        labelsCol.setPrefWidth(200);
        labelsCol.setEditable(false);
        labelsCol.setCellFactory(new Callback<TableColumn<Sample, String>, TableCell<Sample, String>>() {
            @Override
            public TableCell<Sample, String> call(TableColumn<Sample, String> col) {
                final TableCell<Sample, String> cell = new TableCell<Sample, String>() {
                    @Override
                    public void updateItem(String labels, boolean empty) {
                        super.updateItem(labels, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(labels);
                        }
                    }
                };
                cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getClickCount() > 1) {
                        Sample sample=(Sample) cell.getTableRow().getItem();
                        if (sample!=null) {
                            sample.setupLabels();
                        }
                    }
                });
                return cell;
            }
        });


        TableColumn<Sample, String> conditionCol = new TableColumn<>("Condition");
        conditionCol.setCellValueFactory(cellData -> cellData.getValue().getCondition().nameProperty());
        conditionCol.setCellFactory(TextFieldTableCell.forTableColumn());
        //labelsCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gObsDatasetList));
        /*conditionCol.setCellFactory(column -> new TableCell<Sample, String>() {

            private final Text text;

            private Stage editingStage;

            {
                text = new Text();
                text.wrappingWidthProperty().bind(column.widthProperty());
                setGraphic(text);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                closeStage();
            }

            private void closeStage() {
                if (editingStage != null && editingStage.isShowing()) {
                    editingStage.setOnHidden(null);
                    editingStage.hide();
                    editingStage = null;
                }
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                closeStage();
            }

            @Override
            public void startEdit() {
                super.startEdit();

                // create editing ui
                Button cancel = new Button("Cancel");
                cancel.setCancelButton(true);
                cancel.setOnAction(evt -> cancelEdit());

                TextArea editor = new TextArea(getItem());

                Button commit = new Button("OK");
                commit.setOnAction(evt -> commitEdit(editor.getText()));

                VBox vbox = new VBox(10, editor, commit, cancel);

                // display editing window
                Scene scene = new Scene(vbox);
                editingStage = new Stage();
                editingStage.initModality(Modality.WINDOW_MODAL);
                editingStage.initOwner(getScene().getWindow());
                editingStage.setScene(scene);

                editingStage.setOnHidden(evt -> this.cancelEdit());
                editingStage.show();
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                cancelEdit();
                text.setText(item);
            }
        });
         */
        conditionCol.setPrefWidth(200);
        conditionCol.setEditable(true);

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
