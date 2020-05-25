package org.nmrfx.processor.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.nmrfx.processor.datasets.Nuclei;

import java.awt.*;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

public class ExperimentSetup {

    Stage stage;
    BorderPane borderPane;
    HBox hBox;
    BooleanBinding allValid;
    IntegerProperty numDims =new SimpleIntegerProperty(0);
    BooleanProperty nameValid = new SimpleBooleanProperty(true);

    TextField nameField=new TextField();
    ObservableList<ExpDimSetup> expDimSetups=FXCollections.observableArrayList();
    ObservableList<ExpDimSetup> invalidExpDimSetups=FXCollections.observableArrayList();
    BooleanProperty allExpDimSetupsValid = new SimpleBooleanProperty(false);
    Button ok;

    public ExperimentSetup() {
        create();
        show();
    }

    public class ExpDimSetup extends Pane {
        GridPane grid = new GridPane();
        boolean first;
        CheckBox observed;
        ComboBox<Nuclei> nucleus;
        TextField patternField;
        ComboBox<Connectivity.TYPE> connType;
        TextField minTransfersField;
        TextField maxTransfersField;
        TextField bondStringField;
        BooleanProperty valid=new SimpleBooleanProperty(false);
        BooleanBinding validBind;
        BooleanProperty patternParses=new SimpleBooleanProperty(true);

        public ExpDimSetup(boolean first) {
            valid.addListener(e->{
                if (isValid()) {
                    while(invalidExpDimSetups.remove(this));
                } else {
                    if (!invalidExpDimSetups.contains(this)) {
                        invalidExpDimSetups.add(this);
                    }
                }
                if (invalidExpDimSetups.size()==0) {
                    allExpDimSetupsValid.set(true);
                } else {
                    allExpDimSetupsValid.set(false);
                }
            });
            this.first=first;

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


            nucleus=new ComboBox<>();
            ObservableList<Nuclei> nucleiList=FXCollections.observableArrayList();
            nucleiList.addAll(Nuclei.H1,Nuclei.C13,Nuclei.N15,Nuclei.F19,Nuclei.P31);
            nucleus.setItems(nucleiList);
            nucleus.setPromptText("Nucleus");
            nucleus.setPrefWidth(100);

            validBind=nucleus.valueProperty().isNotNull();

            nucleus.styleProperty().bind(Bindings
                    .when(nucleus.valueProperty().isNull())
                    .then("-fx-background-color: yellow")
                    .otherwise(""));
            patternField=new TextField();
            patternField.setPromptText("Pattern");
            patternField.setText("*(*).*");
            validBind=validBind.and(patternParses);
            patternField.styleProperty().bind(Bindings
                    .when(patternParses)
                    .then("")
                    .otherwise("-fx-background-color: yellow"));
            patternField.textProperty().addListener(c -> {
                patternUpdated();
            });
            patternField.setPrefWidth(100);
            observed = new CheckBox("Observed");
            observed.setSelected(true);
            numDims.set(numDims.get()+1);
            observed.setOnAction(e -> {
                if (observed.isSelected()) {
                    numDims.set(numDims.get()+1);
                } else {
                    numDims.set(numDims.get()-1);
                }
            });

            if (!first) {
                //Label arrow = new Label("----------->");
                Group arrow = new Group();
                Line arrowLine = new Line(0,0,100,0);
                arrow.getChildren().add(arrowLine);
                Polygon arrowHead = new Polygon();
                arrowHead.getPoints().addAll(100.0, 0.0,
                        90.0, -10.0,
                        90.0, 10.0);
                arrow.getChildren().add(arrowHead);
                connType=new ComboBox<>();
                connType.getItems().setAll(Connectivity.TYPE.values());
                connType.styleProperty().bind(Bindings
                        .when(connType.valueProperty().isNull())
                        .then("-fx-background-color: yellow")
                        .otherwise(""));
                connType.setPromptText("Transfer");
                connType.setPrefWidth(100);
                validBind=validBind.and(connType.valueProperty().isNotNull());
                bondStringField=new TextField();
                bondStringField.setPromptText("Transfers");
                bondStringField.visibleProperty().bind(connType.valueProperty().isEqualTo(Connectivity.TYPE.J));
                bondStringField.styleProperty().bind(Bindings
                        .when(bondStringField.textProperty().isEmpty())
                        .then("-fx-background-color: yellow")
                        .otherwise(""));
                bondStringField.setPrefWidth(100);
                validBind=validBind.and(connType.valueProperty().isNotEqualTo(Connectivity.TYPE.J).or(bondStringField.textProperty().isNotEmpty()));
                minTransfersField = new TextField();
                minTransfersField.setPromptText("Min");
                minTransfersField.visibleProperty().bind(connType.valueProperty().isEqualTo(Connectivity.TYPE.TOCSY));
                minTransfersField.styleProperty().bind(Bindings
                        .when(minTransfersField.textProperty().isEmpty())
                        .then("-fx-background-color: yellow")
                        .otherwise(""));
                minTransfersField.setPrefWidth(50);
                maxTransfersField = new TextField();
                maxTransfersField.setPromptText("Max");
                maxTransfersField.visibleProperty().bind(connType.valueProperty().isEqualTo(Connectivity.TYPE.TOCSY));
                maxTransfersField.styleProperty().bind(Bindings
                        .when(maxTransfersField.textProperty().isEmpty())
                        .then("-fx-background-color: yellow")
                        .otherwise(""));
                maxTransfersField.setPrefWidth(50);

                validBind=validBind.and(connType.valueProperty().isNotEqualTo(Connectivity.TYPE.TOCSY).or(minTransfersField.textProperty().isNotEmpty().and(maxTransfersField.textProperty().isNotEmpty())));
                Button remove = new Button("Remove");
                remove.setMaxWidth(250);
                remove.setOnAction(e -> {
                    if (observed.isSelected()) {
                        numDims.set(numDims.get()-1);
                    }
                    valid.unbind();
                    valid.set(true);
                    expDimSetups.remove(this);
                    hBox.getChildren().remove(this);
                });

                grid.add(connType,0,0,2,1);
                grid.add(arrow,0,1,2,1);
                grid.add(bondStringField,0,2,2,1);
                grid.add(minTransfersField,0,2);
                grid.add(maxTransfersField,1,2);

                grid.add(remove,0,3,3,1);
            }

            grid.add(nucleus,2,0);
            grid.add(observed,2,1);
            grid.add(patternField,2,2);
            valid.bind(validBind);
            grid.setHgap(10);
            this.getChildren().add(grid);
        }

        public boolean isValid() {
            return valid.get();
        }

        public BooleanProperty validProperty() {
            return valid;
        }

        public void patternUpdated() {
            for (String group : patternField.getText().split(",")) {
                Matcher matcher = ExpDim.matchPattern.matcher(group.trim());
                if (!matcher.matches()) {
                    patternParses.setValue(false);
                } else {
                    patternParses.setValue(true);
                }
            }
         }

        public Experiment.Pair getPair() {
            ExpDim expDim;
            expDim = new ExpDim(nucleus.getValue(),observed.isSelected(),patternField.getText());
            Connectivity connectivity;
            if (first) {
                connectivity=null;
            } else {
                connectivity = new Connectivity(connType.getValue());
                switch (connType.getValue()) {
                    case J:
                        connectivity.setNumBonds(bondStringField.getText());
                        break;
                    case TOCSY:
                        connectivity.setMinTransfers(Integer.parseInt(minTransfersField.getText()));
                        connectivity.setMaxTransfers(Integer.parseInt(maxTransfersField.getText()));
                        break;
                    case HBOND:
                    case NOE:
                        break;
                }

            }
            return new Experiment.Pair(connectivity,expDim);
        }
    }

    public void create() {
        stage = new Stage(StageStyle.DECORATED);
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Experiment Setup");
        stage.setAlwaysOnTop(true);
        stage.setWidth(610);
        stage.setHeight(220);
        stage.setResizable(false);

        String base="New Experiment ";
        int suffix=1;
        while (Experiment.get(base+suffix)!=null) {
            suffix++;
        }
        String name=base+suffix;

        nameField.setText(name);
        nameField.setPrefWidth(150);
        nameField.setPromptText("Name");

        nameField.textProperty().addListener(c -> nameUpdated());
        nameField.styleProperty().bind(Bindings
                .when(nameValid)
                .then("")
                .otherwise("-fx-background-color: yellow"));

        Label dimLab = new Label();
        dimLab.setAlignment(Pos.CENTER);
        dimLab.setPrefWidth(30);
        dimLab.textProperty().bind(Bindings.format("%dD",numDims));
        dimLab.styleProperty().bind(Bindings
                .when(numDims.lessThan(1))
                .then("-fx-background-color: yellow")
                .otherwise(""));

        Button add = new Button("Add transfer");

        add.setOnAction(e -> addExpDim());

        ok = new Button("OK");
        Button cancel = new Button("Cancel");
        ok.setOnAction((event) -> {
            doCreate();
            stage.close();
        });

        HBox top = new HBox(nameField,dimLab,add);
        top.setAlignment(Pos.CENTER_LEFT);

        allValid=Bindings.createBooleanBinding(()->nameValid.get(),nameValid);
        allValid=allValid.and(numDims.greaterThan(0));
        allValid = allValid.and(allExpDimSetupsValid);

        allValid.addListener(e-> {
            if (allValid.get()==true) {
                ok.setDisable(false);
            } else {
                ok.setDisable(true);
            }
        });

        //ok.disableProperty().bind(allValid);
        cancel.setOnAction((event) -> {
            stage.close();
        });

        ButtonBar buttonBar = new ButtonBar();
        ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(cancel, ok);

        hBox=new HBox();
        hBox.setPadding(new Insets(15, 12, 15, 12));
        hBox.setSpacing(10);
        ScrollPane scrollPane=new ScrollPane();
        scrollPane.setContent(hBox);
        scrollPane.pannableProperty().set(true);
        scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);

        addExpDim(true);
        ok.setOnAction(e -> doCreate());
        borderPane.setTop(top);
        borderPane.setBottom(buttonBar);
        borderPane.setCenter(scrollPane);
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(e -> cancel());
    }

    public void nameUpdated() {
        if (Experiment.get(nameField.getText())!=null || nameField.getText().isEmpty()) {
            nameValid.set(false);
        } else {
            nameValid.set(true);
        }
        System.out.println(allValid);
    }
    public void addExpDim(boolean first) {
        ExpDimSetup expDimSetup = new ExpDimSetup(first);
        expDimSetups.add(expDimSetup);
        hBox.getChildren().add(expDimSetup);
    }

    public void addExpDim() {
        addExpDim(false);
    }

    public void show() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        List<Screen> screens = Screen.getScreens();

        stage.setAlwaysOnTop(true);
        stage.show();
        Double x = null;
        Double y = null;

        if (p != null && screens != null) {
            Rectangle2D screenBounds;
            for (Screen screen : screens) {
                screenBounds = screen.getVisualBounds();
                if (screenBounds.contains(p.getX(), p.getY())) {
                    x = p.getX() - stage.getWidth() / 2;
                    y = p.getY() - stage.getHeight() / 2;
                    if (x + stage.getWidth() > screenBounds.getMaxX()) {
                        x = screenBounds.getMaxX() - stage.getWidth() - 50;
                    }
                    if (x < screenBounds.getMinX()) {
                        x = screenBounds.getMinX() + 50;
                    }
                    if (y + stage.getHeight() > screenBounds.getMaxY()) {
                        y = screenBounds.getMaxY() - stage.getHeight() - 50;
                    }
                    if (y < screenBounds.getMinY()) {
                        y = screenBounds.getMinY() + 50;
                    }
                }
            }
        }
        stage.centerOnScreen();
        if (x != null) {
            stage.setX(x);
        }
        if (y != null) {
            stage.setY(y);
        }
    }


    void cancel() {
        stage.close();
    }

    void doCreate() {
        Experiment experiment = new Experiment(nameField.getText());
        for (ExpDimSetup expDimSetup : expDimSetups) {
            experiment.add(expDimSetup.getPair());
        }
        Experiment.writePar();
        stage.close();
    }
}
