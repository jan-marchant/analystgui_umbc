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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class AcquisitionListSceneController implements Initializable {

    //static final DecimalFormat formatter = new DecimalFormat();
    //public UmbcProject project;
    private Stage stage;

    @FXML
    private TableView<Acquisition> tableView;
    @FXML
    Button addButton;

    Button addSampleButton=new Button("+");
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
            //controller.project=UmbcProject.getActive();
            stage.setTitle("Acquisitions");
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
        sampleCol.setCellValueFactory(cellData -> cellData.getValue().sampleProperty());
        sampleCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"sample",UmbcProject.gSampleList));

        sampleCol.setPrefWidth(150);
        Label sampleLabel = new Label(sampleCol.getText());
        BorderPane samplePane = new BorderPane();
        samplePane.setLeft(sampleLabel);
        samplePane.setAlignment(sampleLabel, Pos.CENTER_LEFT);
        samplePane.setRight(addSampleButton);
        samplePane.setAlignment(sampleLabel, Pos.CENTER_RIGHT);
        //addExperimentButton.setPadding(new Insets(2,4,2,4));
        //samplePane.setMargin(addSampleButton, Insets.EMPTY);
        //(sampleLabel,addSampleButton);
        sampleCol.setText(null);
        sampleCol.setGraphic(samplePane);
        //remove sort arrow padding - clash with add button
        //String style=sampleCol.getStyle();
        //System.out.println(style);
        //sampleCol.setStyle(style+"\n"+".column-header .arrow { -fx-background-insets: 0; -fx-padding: 0;}");
        //System.out.println(sampleCol.getStyle());

        //TODO: add experiment filter to match selected dataset (/dataset filter to match selected experiment)
        TableColumn<Acquisition, Experiment> experimentCol = new TableColumn<>("Experiment");
        experimentCol.setCellValueFactory(cellData -> cellData.getValue().experimentProperty());
        experimentCol.setCellFactory(ViewableComboBoxTableCell.getForCellFactory(Acquisition.class,"experiment",UmbcProject.experimentList));
        experimentCol.setPrefWidth(150);
        Label experimentLabel = new Label(experimentCol.getText());
        BorderPane experimentPane = new BorderPane();
        experimentPane.setLeft(experimentLabel);
        experimentPane.setAlignment(experimentLabel, Pos.CENTER_LEFT);
        experimentPane.setRight(addExperimentButton);
        experimentPane.setAlignment(experimentLabel, Pos.CENTER_RIGHT);
        experimentCol.setText(null);
        experimentCol.setGraphic(experimentPane);

        //addExperimentButton.setPadding(new Insets(2,4,2,4));
        //samplePane.setMargin(addExperimentButton, Insets.EMPTY);
        //(experimentLabel,addExperimentButton);
        //experimentCol.setText(null);
        //experimentCol.setGraphic(experimentPane);
        //fixme: remove sort arrow padding - clashing with add button
        //style=experimentCol.getStyle();
        //experimentCol.setStyle(style+"\n"+".column-header .arrow { -fx-background-insets: 0; -fx-padding: 0;}");


        //experimentCol.setEditable(false);

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
        /*listCol.setCellFactory(col -> new TableCell<Acquisition, ObservableList<ManagedList>>() {
            private GridPane listPane=new GridPane();
            @Override
            public void updateItem(ObservableList<ManagedList> managedLists, boolean empty) {
                super.updateItem(managedLists, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    listPane.getChildren().removeAll(listPane.getChildren());
                    Hyperlink hyperlink = new Hyperlink("<Add>");
                    listPane.addColumn(
                            0,
                            hyperlink
                    );
                    hyperlink.setOnAction(e -> {
                        System.out.println("Adding new");
                    });

                    int col = 1;
                    for (ManagedList managedList : managedLists) {
                        hyperlink = new Hyperlink(managedList.getName());
                        listPane.addColumn(
                                col,
                                hyperlink
                        );
                        hyperlink.setOnAction(e -> {
                            System.out.println(managedList.getName());
                        });
                        col++;
                    }
                    setGraphic(listPane);
                }

                //setText(managedLists.stream().map(ManagedList::getName)
                 //       .collect(Collectors.joining(" ")));
            }
        });*/

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
        listCol.setPrefWidth(150);
        //listCol.setEditable(false);

        tableView.getColumns().setAll(datasetCol, sampleCol,experimentCol, listCol);
        //tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setOnKeyPressed( new EventHandler<KeyEvent>()
        {
            @Override
            public void handle( final KeyEvent keyEvent )
            {
                final Acquisition selectedItem = tableView.getSelectionModel().getSelectedItem();

                if ( selectedItem != null )
                {
                    if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                    {
                        selectedItem.remove(true);
                    }
                }
            }
        } );

        addSampleButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Sample.addNew();
            }
        });

        addExperimentButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Experiment.addNew();
            }
        });

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
