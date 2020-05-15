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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.NumberStringConverter;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ConditionListSceneController implements Initializable {

    //static final DecimalFormat formatter = new DecimalFormat();
    //public UmbcProject project;
    private Stage stage;

    @FXML
    private TableView<Condition> tableView;
    @FXML
    Button addButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
    }

    public Stage getStage() {
        return stage;
    }

    public static ConditionListSceneController create() {
        FXMLLoader loader = new FXMLLoader(ConditionListSceneController.class.getResource("/ConditionListScene.fxml"));
        ConditionListSceneController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.getController();
            controller.stage = stage;
            //controller.project=UmbcProject.getActive();
            stage.setTitle("Conditions");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    void initTable() {
        tableView.setEditable(true);

        TableColumn<Condition, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setPrefWidth(200);
        nameCol.setEditable(true);

        TableColumn<Condition, Number> temperatureCol = new TableColumn<>("Temp. (K)");
        temperatureCol.setCellValueFactory(cellData -> cellData.getValue().temperatureProperty());
        temperatureCol.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        temperatureCol.setPrefWidth(100);
        temperatureCol.setEditable(true);

        TableColumn<Condition, Number> pressureCol = new TableColumn<>("Press. (atm)");
        pressureCol.setCellValueFactory(cellData -> cellData.getValue().pressureProperty());
        pressureCol.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        pressureCol.setPrefWidth(100);
        pressureCol.setEditable(true);

        TableColumn<Condition, Number> pHCol = new TableColumn<>("pH");
        pHCol.setCellValueFactory(cellData -> cellData.getValue().pHProperty());
        pHCol.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        pHCol.setPrefWidth(80);
        pHCol.setEditable(true);

        TableColumn<Condition, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(cellData -> cellData.getValue().detailsProperty());
        detailsCol.setPrefWidth(300);
        detailsCol.setEditable(true);
        detailsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        //unfortunately looks like no line breaks in STAR file
        /*detailsCol.setCellFactory(column -> new TableCell<Condition, String>() {

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





        tableView.getColumns().setAll(nameCol,temperatureCol,pHCol,pressureCol,detailsCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setOnKeyPressed(keyEvent -> {
            final Condition selectedItem = tableView.getSelectionModel().getSelectedItem();

            if ( selectedItem != null )
            {
                if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                {
                    selectedItem.remove(true);
                }
            }
        });

        addButton.setOnAction(event -> Condition.addNew());

    }

    public void setConditionList(ObservableList<Condition> conditions) {
        if (tableView == null) {
            System.out.println("null table");
        } else {
            tableView.setItems(conditions);
        }

    }

    void refresh() {
        tableView.refresh();
    }

}
