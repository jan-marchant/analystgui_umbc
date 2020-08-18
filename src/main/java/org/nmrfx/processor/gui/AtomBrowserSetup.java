package org.nmrfx.processor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AtomBrowserSetup {

    Stage stage;
    BorderPane borderPane;
    double xOffset = 50;
    FXMLController controller;
    ComboBox<String> xLabel;
    ComboBox<String> yLabel;
    TableView<AtomBrowser.RangeItem> rangeItemTable;
    TableView<AtomBrowser.FilterItem> filterItemTable;
    static final Map<String, String> filterMap = new HashMap<>();
    TextField atomFilterTextField;
    TextField atomFilterTextField2;

    MenuButton addFilterItem = new MenuButton("Add Filter Item");
    Menu projectFilter = new Menu ("Projects");
    Menu experimentFilter = new Menu ("Experiments");
    Menu conditionFilter = new Menu ("Conditions");
    Menu sampleFilter = new Menu ("Samples");
    Menu acquisitionFilter = new Menu ("Acquisitions");


    ChoiceBox<FXMLController> atomBrowserChoice = new ChoiceBox<>();

    static {
        filterMap.put("RNA-H", "*.H8,H2,H6,H5,H1'");
        filterMap.put("H", "*.H*");
        filterMap.put("Carbons", "*.C*");
        filterMap.put("Nitrogen", "*.N*");
        filterMap.put("Phosphorous", "*.P*");
    }


    public AtomBrowserSetup(FXMLController controller) {
            this.controller=controller;
            create();
            initialize();
            show();
    }


    public void create() {
        stage = new Stage(StageStyle.DECORATED);
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Atom Browser Setup");
        stage.setAlwaysOnTop(true);

        Label nameLabel=new Label("Window:");

        ObservableList<FXMLController> controllerList = FXCollections.observableArrayList(FXMLController.getControllers().stream().filter(c -> c.containsTool(AtomBrowser.class)).collect(Collectors.toList()));

        atomBrowserChoice.setConverter(new StringConverter<FXMLController>() {
            @Override
            public String toString(FXMLController object) {
                return object.getStage().getTitle();
            }

            @Override
            public FXMLController fromString(String string) {
                return null;
            }
        });

        atomBrowserChoice.setItems(controllerList);
        atomBrowserChoice.setValue(controller);
        atomBrowserChoice.setOnAction(e -> updateController());

        Label xAxis = new Label("x Axis Label: ");
        Label yAxis = new Label("y Axis Label: ");

        xLabel = new ComboBox<>();
        //ObservableList<String> nucleiList=FXCollections.observableArrayList();
        xLabel.setItems(UmbcProject.getActive().labelList);

        yLabel = new ComboBox<>();
        //ObservableList<String> nucleiList=FXCollections.observableArrayList();
        yLabel.setItems(UmbcProject.getActive().labelList);

        MenuButton atomFilter = new MenuButton("Atom filter");

        atomFilterTextField=new TextField();
                filterMap.keySet().stream().sorted().forEach(filterName -> {
            MenuItem backBoneItem = new MenuItem(filterName);
            backBoneItem.setOnAction(e -> setFilterString(filterName));
            atomFilter.getItems().add(backBoneItem);
        });

        MenuButton atomFilter2 = new MenuButton("Locate filter");
        atomFilterTextField2=new TextField();
        filterMap.keySet().stream().sorted().forEach(filterName -> {
            MenuItem backBoneItem = new MenuItem(filterName);
            backBoneItem.setOnAction(e -> setFilterString2(filterName));
            atomFilter2.getItems().add(backBoneItem);
        });

        atomFilter.setPrefWidth(110);
        atomFilterTextField.setPrefWidth(130);
        atomFilter2.setPrefWidth(110);
        atomFilterTextField2.setPrefWidth(130);
        atomFilter.prefWidthProperty().bindBidirectional(atomFilter2.prefWidthProperty());
        atomFilterTextField.prefWidthProperty().bindBidirectional(atomFilterTextField2.prefWidthProperty());


        rangeItemTable = new TableView<>();
        rangeItemTable.setMaxHeight(120);
        Button addRangeItem = new Button("Add Range Item");
        rangeItemTable.setEditable(true);
        TableColumn<AtomBrowser.RangeItem, String> nameCol = new TableColumn<>("Label");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setEditable(true);
        TableColumn<AtomBrowser.RangeItem, Number> minCol = new TableColumn<>("minShift");
        minCol.setCellValueFactory(cellData -> cellData.getValue().minProperty());
        minCol.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        TableColumn<AtomBrowser.RangeItem, Number> maxCol = new TableColumn<>("maxShift");
        maxCol.setCellValueFactory(cellData -> cellData.getValue().maxProperty());
        maxCol.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));

        rangeItemTable.getColumns().setAll(nameCol,minCol,maxCol);

        rangeItemTable.setOnKeyPressed(keyEvent -> {
            final AtomBrowser.RangeItem selectedItem = rangeItemTable.getSelectionModel().getSelectedItem();

            if ( selectedItem != null )
            {
                if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                {
                    selectedItem.remove(getAtomBrowser());
                }

            }
        });

        addRangeItem.setOnAction(event -> getAtomBrowser().addRangeControl("New",-1000,1000));

        filterItemTable = new TableView<>();
        filterItemTable.setMaxHeight(120);
        filterItemTable.setEditable(false);
        TableColumn<AtomBrowser.FilterItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());

        TableColumn<AtomBrowser.FilterItem, String> objNameCol = new TableColumn<>("Name");
        objNameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<AtomBrowser.FilterItem, Boolean> allowCol = new TableColumn<>("Allow?");
        allowCol.setCellValueFactory(cellData -> cellData.getValue().allowProperty());


        filterItemTable.getColumns().setAll(typeCol,objNameCol,allowCol);

        filterItemTable.setOnKeyPressed(keyEvent -> {
            final AtomBrowser.FilterItem selectedItem = filterItemTable.getSelectionModel().getSelectedItem();

            if ( selectedItem != null )
            {
                if ( keyEvent.getCode().equals( KeyCode.DELETE ) | keyEvent.getCode().equals( KeyCode.BACK_SPACE ))
                {
                    selectedItem.remove(getAtomBrowser());
                }

            }
        });

        addFilterItem.getItems().addAll(projectFilter,experimentFilter,conditionFilter,sampleFilter,acquisitionFilter);

        Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);
        HBox hBox=new HBox(nameLabel,region1,atomBrowserChoice);
        Region region2 = new Region();
        HBox.setHgrow(region2, Priority.ALWAYS);
        HBox hBox2=new HBox(xAxis,region2,xLabel);
        Region region3 = new Region();
        HBox.setHgrow(region3, Priority.ALWAYS);
        HBox hBox3=new HBox(yAxis,region3,yLabel);

        HBox hBox4=new HBox(atomFilter,atomFilterTextField);
        HBox hBox5=new HBox(atomFilter2,atomFilterTextField2);

        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox2.setAlignment(Pos.CENTER_LEFT);
        hBox3.setAlignment(Pos.CENTER_LEFT);
        hBox4.setAlignment(Pos.CENTER_LEFT);
        hBox5.setAlignment(Pos.CENTER_LEFT);

        Separator separator1 = new Separator(Orientation.HORIZONTAL);
        Separator separator2 = new Separator(Orientation.HORIZONTAL);
        Separator separator3 = new Separator(Orientation.HORIZONTAL);
        Separator separator4 = new Separator(Orientation.HORIZONTAL);


        VBox vBox=new VBox(hBox,hBox2,hBox3,hBox4,hBox5,separator1,rangeItemTable,addRangeItem,separator2,filterItemTable,addFilterItem,separator3);

        //vBox.setSpacing(2);
        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");
        ok.setOnAction((event) -> {
            apply();
            stage.close();
        });

        cancel.setOnAction((event) -> {
            stage.close();
        });

        ButtonBar buttonBar = new ButtonBar();
        ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        buttonBar.getButtons().addAll(cancel, ok);

        borderPane.setTop(vBox);
        borderPane.setBottom(buttonBar);
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(e -> cancel());
    }

    public void updateController () {
        controller = atomBrowserChoice.getValue();
        initialize();
    }

    public void initialize() {
        xLabel.setValue(getAtomBrowser().getxLabel());
        yLabel.setValue(getAtomBrowser().getyLabel());
        rangeItemTable.setItems(getAtomBrowser().rangeItems);
        atomFilterTextField.setText(getAtomBrowser().atomSelector1.filterString);
        atomFilterTextField2.setText(getAtomBrowser().atomSelector2.filterString);

        Menu projectMenuItem = new Menu(Project.getActive().name());
        MenuItem allow = new MenuItem("Allow");
        MenuItem deny = new MenuItem("Deny");
        allow.setOnAction(e-> getAtomBrowser().addFilterItem(Project.getActive(),true));
        deny.setOnAction(e-> getAtomBrowser().addFilterItem(Project.getActive(),false));

        projectFilter.getItems().clear();
        projectFilter.getItems().add(projectMenuItem);
        projectMenuItem.getItems().addAll(allow,deny);

        for (UmbcProject object : UmbcProject.getActive().subProjectList) {
            Menu item = new Menu(object.name());
            MenuItem allow2 = new MenuItem("Allow");
            MenuItem deny2 = new MenuItem("Deny");
            allow2.setOnAction(e-> getAtomBrowser().addFilterItem(object,true));
            deny2.setOnAction(e-> getAtomBrowser().addFilterItem(object,false));
            projectFilter.getItems().add(item);
            item.getItems().addAll(allow2,deny2);
        }

        experimentFilter.getItems().clear();

        for (Experiment object : UmbcProject.experimentList) {
            Menu item = new Menu(object.toString());
            MenuItem allow2 = new MenuItem("Allow");
            MenuItem deny2 = new MenuItem("Deny");
            allow2.setOnAction(e-> getAtomBrowser().addFilterItem(object,true));
            deny2.setOnAction(e-> getAtomBrowser().addFilterItem(object,false));
            experimentFilter.getItems().add(item);
            item.getItems().addAll(allow2,deny2);
        }

        for (Condition object : UmbcProject.gConditionList) {
            Menu item = new Menu(object.toString());
            MenuItem allow2 = new MenuItem("Allow");
            MenuItem deny2 = new MenuItem("Deny");
            allow2.setOnAction(e-> getAtomBrowser().addFilterItem(object,true));
            deny2.setOnAction(e-> getAtomBrowser().addFilterItem(object,false));
            conditionFilter.getItems().add(item);
            item.getItems().addAll(allow2,deny2);
        }

        for (Sample object : UmbcProject.gSampleList) {
            Menu item = new Menu(object.toString());
            MenuItem allow2 = new MenuItem("Allow");
            MenuItem deny2 = new MenuItem("Deny");
            allow2.setOnAction(e-> getAtomBrowser().addFilterItem(object,true));
            deny2.setOnAction(e-> getAtomBrowser().addFilterItem(object,false));
            sampleFilter.getItems().add(item);
            item.getItems().addAll(allow2,deny2);
        }

        for (Acquisition object : UmbcProject.gAcquisitionTable) {
            Menu item = new Menu(object.getDataset().toString());
            MenuItem allow2 = new MenuItem("Allow");
            MenuItem deny2 = new MenuItem("Deny");
            allow2.setOnAction(e-> getAtomBrowser().addFilterItem(object,true));
            deny2.setOnAction(e-> getAtomBrowser().addFilterItem(object,false));
            acquisitionFilter.getItems().add(item);
            item.getItems().addAll(allow2,deny2);
        }
        filterItemTable.setItems(getAtomBrowser().filterList);

    }

    public void show() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        List<Screen> screens = Screen.getScreens();

        stage.setAlwaysOnTop(true);
        //stage.setResizable(false);
        stage.show();
        Double x=null;
        Double y=null;

        if (p != null && screens != null) {
            Rectangle2D screenBounds;
            for (Screen screen : screens) {
                screenBounds=screen.getVisualBounds();
                if (screenBounds.contains(p.getX(),p.getY())) {
                    x=p.getX()-stage.getWidth()/2;
                    y=p.getY()-stage.getHeight()/2;
                    if (x+stage.getWidth()>screenBounds.getMaxX()) {
                        x=screenBounds.getMaxX()-stage.getWidth()-50;
                    }
                    if (x<screenBounds.getMinX()) {
                        x=screenBounds.getMinX()+50;
                    }
                    if (y+stage.getHeight()>screenBounds.getMaxY()) {
                        y=screenBounds.getMaxY()-stage.getHeight()-50;
                    }
                    if (y<screenBounds.getMinY()) {
                        y=screenBounds.getMinY()+50;
                    }
                }
            }
        }
        stage.centerOnScreen();
        if (x!=null) {
            stage.setX(x);
        }
        if (y!=null) {
            stage.setY(y);
        }
        stage.setWidth(240);
    }

    public void setFilterString(String filterName) {
        String filterString = filterMap.get(filterName);
        if (filterString == null) {
            filterString = "";
        }
        atomFilterTextField.setText(filterString);
    }

    public void setFilterString2(String filterName) {
        String filterString = filterMap.get(filterName);
        if (filterString == null) {
            filterString = "";
        }
        atomFilterTextField2.setText(filterString);
    }

    AtomBrowser getAtomBrowser() {
        return ((AtomBrowser) controller.getTool(AtomBrowser.class));
    }

    void cancel() {
        stage.close();
    }

    void apply() {
        getAtomBrowser().setxLabel(xLabel.getValue());
        getAtomBrowser().setyLabel(yLabel.getValue());
        getAtomBrowser().rangeSelector.setItems(getAtomBrowser().rangeItems);
        getAtomBrowser().atomSelector1.setFilterString(atomFilterTextField.getText());
        getAtomBrowser().atomSelector2.setFilterString(atomFilterTextField2.getText());
    }
}
