/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;

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
        //entityCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setPrefWidth(200);

        TableColumn<Sample, String> entityCol = new TableColumn<>("Entity labeling");
        entityCol.setCellValueFactory(cellData -> cellData.getValue().labelStringProperty());
        entityCol.setCellFactory(new Callback<TableColumn<Sample, String>, TableCell<Sample, String>>() {
            @Override
            public TableCell<Sample, String> call(TableColumn<Sample, String> param) {
                TableCell<Sample, String> labeling = new TableCell<Sample, String>() {
                    @Override
                    public void updateItem(String string, boolean empty) {
                        super.updateItem(string, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            Sample sample = (Sample) getTableRow().getItem();
                            if (sample!=null) {
                                GridPane pane = new GridPane();
                                int row = 0;
                                for (Entity entity : sample.getEntities()) {
                                    Label label = new Label(entity.getName());
                                    TextField textField = new TextField(sample.getEntityLabelString(entity));
                                    textField.setPromptText("Click to set labeling");
                                    textField.setDisable(true);
                                    Pane clickPane = new Pane(textField);
                                    pane.add(label, 0, row);
                                    pane.add(clickPane, 1, row++);
                                    label.setAlignment(Pos.CENTER_LEFT);
                                    clickPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                                        sample.setupLabels(entity);
                                    });

                                }
                                setText(null);
                                setGraphic(pane);
                            } else {
                                setText(null);
                                setGraphic(null);
                            }
                        }
                    }
                };
                return labeling;
            }
        });
        entityCol.setPrefWidth(400);

        tableView.getColumns().setAll(nameCol,entityCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setOnKeyPressed(keyEvent -> {
            final Sample selectedItem = tableView.getSelectionModel().getSelectedItem();

            if ( selectedItem != null )
            {
                if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                {
                    selectedItem.remove(true);
                }
            }
        });

        addButton.setOnAction(event -> Sample.addNew());

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
