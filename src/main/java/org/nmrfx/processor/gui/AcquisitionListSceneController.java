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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.utils.GUIUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AcquisitionListSceneController implements Initializable {

    private Stage stage;

    @FXML
    private TableView<Acquisition> tableView;
    @FXML
    Button addButton;

    Button addSampleButton=new Button("+");
    Button addConditionButton=new Button("+");
    Button addExperimentButton=new Button("+");

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
            stage.setTitle("Acquisitions");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

    void initTable() {
        tableView.setEditable(true);

        TableColumn<Acquisition, Dataset> datasetCol = new TableColumn<>("Dataset");
        datasetCol.setCellValueFactory(cellData -> cellData.getValue().datasetProperty());
        datasetCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"dataset",UmbcProject.gDatasetList));
        datasetCol.setPrefWidth(200);

        TableColumn<Acquisition, Sample> sampleCol = new TableColumn<>("Sample");
        sampleCol.setCellValueFactory(cellData -> cellData.getValue().sampleProperty());
        sampleCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));
        sampleCol.setPrefWidth(150);
        Label sampleLabel = new Label(sampleCol.getText());
        BorderPane samplePane = new BorderPane();
        samplePane.setLeft(sampleLabel);
        samplePane.setAlignment(sampleLabel, Pos.CENTER_LEFT);
        samplePane.setRight(addSampleButton);
        samplePane.setAlignment(sampleLabel, Pos.CENTER_RIGHT);
        sampleCol.setText(null);
        sampleCol.setGraphic(samplePane);

        TableColumn<Acquisition, Condition> conditionCol = new TableColumn<>("Condition");
        conditionCol.setCellValueFactory(cellData -> cellData.getValue().conditionProperty());
        conditionCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"condition",UmbcProject.gConditionList));
        conditionCol.setPrefWidth(150);
        Label conditionLabel = new Label(conditionCol.getText());
        BorderPane conditionPane = new BorderPane();
        conditionPane.setLeft(conditionLabel);
        conditionPane.setAlignment(conditionLabel, Pos.CENTER_LEFT);
        conditionPane.setRight(addConditionButton);
        conditionPane.setAlignment(conditionLabel, Pos.CENTER_RIGHT);
        conditionCol.setText(null);
        conditionCol.setGraphic(conditionPane);

        TableColumn<Acquisition, Experiment> experimentCol = new TableColumn<>("Experiment");
        experimentCol.setCellValueFactory(cellData -> cellData.getValue().experimentProperty());
        experimentCol.setCellFactory(col -> new TableCell<Acquisition, Experiment>() {
            ComboBox<Experiment> combo = new ComboBox();

            {
                combo.prefWidthProperty().bind(this.widthProperty());
                combo.valueProperty().addListener((obs,oldV,newV) -> {
                    Acquisition acq=getAcq();
                    if (acq != null) {
                        acq.setExperiment(combo.getValue());
                    }
                });

                addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (!isEmpty()) {
                        Acquisition acq = getAcq();
                        if (acq != null) {
                            if (acq.getManagedLists().size() > 0) {
                                boolean result = GUIUtils.affirm("All associated managed lists will be deleted. Continue?");
                                if (!result) {
                                    event.consume();
                                } else {
                                    acq.getManagedLists().clear();
                                    combo.valueProperty().set(null);
                                    combo.show();
                                }
                            }
                        }
                    }
                });
            }
            //TODO: is there a proper way to do this?
           private Acquisition getAcq() {
                try {
                    return getTableView().getItems().get(getIndex());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void updateItem(Experiment t, boolean empty) {
                super.updateItem(t, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Acquisition acq=getAcq();
                    //TODO: should only need to set this once really
                    combo.setItems(acq.getValidExperimentList());
                    combo.setValue(t);
                    setGraphic(combo);
                }
            }

        });

        experimentCol.setPrefWidth(150);
        Label experimentLabel = new Label(experimentCol.getText());
        BorderPane experimentPane = new BorderPane();
        experimentPane.setLeft(experimentLabel);
        experimentPane.setAlignment(experimentLabel, Pos.CENTER_LEFT);
        experimentPane.setRight(addExperimentButton);
        experimentPane.setAlignment(experimentLabel, Pos.CENTER_RIGHT);
        experimentCol.setText(null);
        experimentCol.setGraphic(experimentPane);

        TableColumn<Acquisition, ObservableList<ManagedList>> listCol = new TableColumn<>("Associated lists");
        listCol.setCellValueFactory(new PropertyValueFactory<>("managedLists"));
        listCol.setCellFactory(col -> new TableCell<Acquisition, ObservableList<ManagedList>>() {
            private MenuButton menuButton=new MenuButton();
            @Override
            public void updateItem(ObservableList<ManagedList> managedLists, boolean empty) {
                menuButton.prefWidthProperty().bind(this.widthProperty());
                super.updateItem(managedLists, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    menuButton.setText(managedLists.size() + " list"+(managedLists.size()==1?"":"s"));
                    menuButton.getItems().removeAll(menuButton.getItems());
                    Menu menu;
                    Menu detailMenu;
                    MenuItem detailItem;
                    MenuItem deleteMenuItem;

                    for (ManagedList managedList : managedLists) {
                        menu = new Menu(managedList.getName());
                        detailMenu=new Menu("Details");
                        for (String detailText : managedList.getDetailText()) {
                            detailItem=new MenuItem(detailText);
                            detailMenu.getItems().add(detailItem);
                        }
                        deleteMenuItem=new MenuItem("Delete");
                        deleteMenuItem.setOnAction(e -> ((Acquisition) this.getTableRow().getItem()).deleteManagedList(managedList,true));
                        menu.getItems().addAll(detailMenu,deleteMenuItem);
                        menuButton.getItems().add(menu);
                    }
                    MenuItem addMenuItem = new MenuItem("Add New");
                    menuButton.getItems().add(addMenuItem);
                    addMenuItem.setOnAction(e -> ((Acquisition) this.getTableRow().getItem()).addNewManagedList());
                    setGraphic(menuButton);
                }
            }

        });
        listCol.setPrefWidth(150);

        tableView.getColumns().setAll(datasetCol, sampleCol, conditionCol,experimentCol, listCol);

        tableView.setOnKeyPressed(keyEvent -> {
            final Acquisition selectedItem = tableView.getSelectionModel().getSelectedItem();

            if ( selectedItem != null )
            {
                if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                {
                    selectedItem.remove(true);
                }
            }
        });

        addSampleButton.setOnAction(event -> {
            Sample.addNew();
            UMBCApp.showSampleList(event);
        });
        addConditionButton.setOnAction(event -> {
            Condition.addNew();
            UMBCApp.showConditionList(event);
        });
        addExperimentButton.setOnAction(event -> Experiment.addNew());

        addButton.setOnAction(event -> new Acquisition());

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
