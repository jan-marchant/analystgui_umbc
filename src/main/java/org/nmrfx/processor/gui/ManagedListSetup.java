package org.nmrfx.processor.gui;

import javafx.collections.MapChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

public class ManagedListSetup {

    Stage stage;
    BorderPane borderPane;
    double xOffset = 50;
    Acquisition acquisition;

    TextField nameField=new TextField();
    ChoiceBox<Integer> ppmSetChoices = new ChoiceBox();
    ChoiceBox<Integer> rPpmSetChoices = new ChoiceBox();
    //all NOE dims must use same NoeSet
    ChoiceBox<NoeSet> noeSet = new ChoiceBox();
    //TextField bondField = new TextField();
    //TextField minField = new TextField();
    //TextField maxField = new TextField();

    public ManagedListSetup (Acquisition acquisition) {
        this.acquisition=acquisition;
        if (acquisition.getDataset()==null || acquisition.getSample()==null || acquisition.getExperiment()==null) {
            GUIUtils.warn("Cannot add list", "You must define all acquisition parameters before adding any lists.");
        } else {
            create();
            show(300,300);
        }
    }


    public void create() {
        stage = new Stage(StageStyle.DECORATED);
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Managed List Setup");
        stage.setAlwaysOnTop(true);
        Label nameLabel=new Label("Name:");

        String name="managed_"+acquisition.getDataset().getName().split("\\.")[0];

        if (PeakList.get(name)!=null) {
            Integer suffix = 2;
            while (PeakList.get(name + suffix.toString()) != null) {
                suffix += 1;
            }
            nameField.setText(name+suffix.toString());
        } else {
            nameField.setText(name);
        }
        nameField.setPrefWidth(300);
        /*
        Label ppmSetLabel=new Label("PPM Set: ");

        ppmSetChoices.getItems().add(0);
        ppmSetChoices.getItems().add(1);
        ppmSetChoices.getItems().add(2);
        ppmSetChoices.getItems().add(3);
        ppmSetChoices.getItems().add(4);
        ppmSetChoices.setValue(0);
        */

        Label rPpmSetLabel=new Label("Ref PPM Set: ");

        rPpmSetChoices.getItems().add(0);
        rPpmSetChoices.getItems().add(1);
        rPpmSetChoices.getItems().add(2);
        rPpmSetChoices.getItems().add(3);
        rPpmSetChoices.getItems().add(4);
        rPpmSetChoices.setValue(0);

        HBox hBox=new HBox(nameLabel,nameField);
        HBox hBox2=new HBox(rPpmSetLabel,rPpmSetChoices);
        VBox vBox=new VBox(hBox,hBox2);
        UnaryOperator<TextFormatter.Change> bondFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-5,])?")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-5])?")) {
                return change;
            }
            return null;
        };

        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");
        ok.setOnAction((event) -> {
            doCreate();
            stage.close();
        });

        cancel.setOnAction((event) -> {
            stage.close();
        });

        Label connLabel;
        GridPane connPane=new GridPane();
        int row=0;
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            if (expDim.getNextExpDim()==null) {
                break;
            }

            connLabel=new Label(expDim.toString()+"-"+expDim.getNextExpDim().toString()+": ");
            connPane.add(connLabel,0,row,1,1);
            String labString = expDim.getNextCon().toString();
            switch (expDim.getNextCon().getType()) {
                case NOE:
                    labString+= " using NOE set: ";
                    //noeType.getItems().setAll(Connectivity.NOETYPE.values());
                    noeSet.getItems().setAll(acquisition.getProject().noeSetMap.values());
                    noeSet.setConverter(new StringConverter<NoeSet>() {

                        @Override
                        public String toString(NoeSet noeSet) {
                            return acquisition.getProject().noeSetMap.entrySet().stream().filter(ap ->ap.getValue().equals(noeSet)).findFirst().orElse(null).getKey();
                        }

                        @Override
                        public NoeSet fromString(String string) {
                            return acquisition.getProject().noeSetMap.get(string);
                        }
                    });

                    connPane.add(new Label(labString),1,row,1,1);
                    connPane.add(noeSet,2,row++,1,1);
                    ok.setDisable(true);
                    noeSet.valueProperty().addListener((observable, oldValue, newValue)->{
                        if (newValue!=null) {
                            ok.setDisable(false);
                        } else {
                            ok.setDisable(true);
                        }
                    });
                    break;
                case J:
                    /*bondField.setTextFormatter(
                            new TextFormatter<String>(new DefaultStringConverter(), expDim.getNextCon().getNumBonds(), bondFilter));
                     */
                case TOCSY:
                    /*minField.setTextFormatter(
                            new TextFormatter<Integer>(new IntegerStringConverter(), expDim.getNextCon().getMinTransfers(), integerFilter));
                    maxField.setTextFormatter(
                            new TextFormatter<Integer>(new IntegerStringConverter(), expDim.getNextCon().getMaxTransfers(), integerFilter));
                     */
                case HBOND:
                default:
                    connPane.add(new Label(labString),1,row++,2,1);
                    break;
            }
        }

        ButtonBar buttonBar = new ButtonBar();
        ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(cancel, ok);

        ok.setOnAction(e -> doCreate());
        borderPane.setTop(vBox);
        borderPane.setCenter(connPane);
        borderPane.setBottom(buttonBar);
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(e -> cancel());
    }


    public void show(double x, double y) {

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        if (x > (screenWidth / 2)) {
            x = x - stage.getWidth() - xOffset;
        } else {
            x = x + 100;
        }

        y = y - stage.getHeight() / 2.0;

        stage.setX(x);
        stage.setY(y);
        stage.show();
    }


    void cancel() {
        stage.close();
    }

    void doCreate() {
        if (acquisition.getDataset()==null || acquisition.getSample()==null || acquisition.getExperiment()==null) {
            GUIUtils.warn("Cannot add list", "You must define all acquisition parameters before adding any lists.");
        } else {
            ManagedList managedList=new ManagedList(acquisition,nameField.getText(),0,rPpmSetChoices.getValue(),noeSet.getValue());
            acquisition.getManagedLists().add(managedList);
        }
        stage.close();
    }
}
