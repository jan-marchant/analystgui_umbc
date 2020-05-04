package org.nmrfx.processor.gui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.utils.GUIUtils;

import java.util.function.UnaryOperator;

public class ManagedListSetup {

    Stage stage;
    BorderPane borderPane;
    double xOffset = 50;
    Acquisition acquisition;

    TextField nameField=new TextField();
    ChoiceBox<Integer> ppmSetChoices = new ChoiceBox();
    ChoiceBox<Integer> rPpmSetChoices = new ChoiceBox();

    ChoiceBox<Connectivity.NOETYPE> noeType = new ChoiceBox();
    TextField bondField = new TextField();
    TextField minField = new TextField();
    TextField maxField = new TextField();

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
        Integer suffix=1;
        while (PeakList.get(name+suffix.toString())!=null) {
            suffix+=1;
        }
        nameField.setText(name+suffix.toString());

        Label ppmSetLabel=new Label("PPM Set: ");

        ppmSetChoices.getItems().add(0);
        ppmSetChoices.getItems().add(1);
        ppmSetChoices.getItems().add(2);
        ppmSetChoices.getItems().add(3);
        ppmSetChoices.getItems().add(4);
        ppmSetChoices.setValue(0);

        Label rPpmSetLabel=new Label("Ref Set: ");

        rPpmSetChoices.getItems().add(0);
        rPpmSetChoices.getItems().add(1);
        rPpmSetChoices.getItems().add(2);
        rPpmSetChoices.getItems().add(3);
        rPpmSetChoices.getItems().add(4);
        rPpmSetChoices.setValue(0);

        HBox hBox=new HBox(nameLabel,nameField,ppmSetLabel,ppmSetChoices,rPpmSetLabel,rPpmSetChoices);
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

        Integer conNum=1;
        char alpha='a';
        String labString;
        Label connLabel;
        GridPane connPane=new GridPane();
        int col=0;
        int row=0;
        //for (ExpDim expDim : acquisition.getExperiment().getExpDims()) {
        for (ExpDim expDim =acquisition.getExperiment().getFirst(); expDim.getNextExpDim()!=null;expDim=expDim.getNextExpDim()) {
            labString = "";
            if (expDim.getNextExpDim()==null) {
                break;
            }

            if (expDim.isObserved()) {
                labString += "F"+conNum.toString();
                if (expDim.getNextExpDim().isObserved()) {
                    conNum++;
                    labString+="-F"+conNum.toString();
                } else {
                    labString += "-r" + conNum.toString() + alpha;
                }
            } else {
                labString += "r"+conNum.toString()+alpha;
                alpha++;
                if (expDim.getNextExpDim().isObserved()) {
                    conNum += 1;
                    labString+="-F"+conNum.toString();
                    alpha='a';
                } else {
                    labString += "-r" + conNum.toString() + alpha;
                }
            }

            connLabel=new Label(labString);
            connPane.add(connLabel,0,row++,5,1);
            String labString2 = new String();
            switch (expDim.getNextCon().getType()) {
                case NOE:
                    labString2 = "NOE generation logic:";
                    noeType.getItems().setAll(Connectivity.NOETYPE.values());
                    connPane.add(new Label(labString2),0,row,1,1);
                    connPane.add(noeType,2,row++,4,1);
                    break;
                case J:
                    labString2 = "Number of bonds:";
                    bondField.setTextFormatter(
                            new TextFormatter<String>(new DefaultStringConverter(), expDim.getNextCon().getNumBonds(), bondFilter));
                    connPane.add(new Label(labString2),0,row,1,1);
                    connPane.add(bondField,2,row++,4,1);
                    break;
                case TOCSY:
                    labString2 = "Number of transfers:";
                    Label min = new Label("Min:");
                    Label max = new Label("Max:");
                    minField.setTextFormatter(
                            new TextFormatter<Integer>(new IntegerStringConverter(), expDim.getNextCon().getMinTransfers(), integerFilter));
                    maxField.setTextFormatter(
                            new TextFormatter<Integer>(new IntegerStringConverter(), expDim.getNextCon().getMaxTransfers(), integerFilter));
                    connPane.add(new Label(labString2),0,row,1,1);
                    connPane.add(min,1,row,1,1);
                    connPane.add(minField,2,row,1,1);
                    connPane.add(max,3,row,1,1);
                    connPane.add(maxField,4,row++,1,1);
                    break;
                case HBOND:
                    labString2 = "H-bond (no setup required).";
                    connPane.add(new Label(labString2),0,row++,5,1);
                    break;
                default:
                    labString2 = "ERROR";
                    connPane.add(new Label(labString2),0,row++,5,1);
                    break;
            }
        }
        borderPane.setTop(hBox);
        borderPane.setCenter(connPane);
        Button createButton = new Button("Create");
        createButton.setOnAction(e -> doCreate());
        borderPane.setBottom(createButton);
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
            ManagedList managedList=new ManagedList(acquisition,nameField.getText(),ppmSetChoices.getValue(),rPpmSetChoices.getValue(),noeType.getValue(),bondField.getText(),minField.getText(),maxField.getText());
            acquisition.getManagedLists().add(managedList);
        }
        stage.close();
    }
}
