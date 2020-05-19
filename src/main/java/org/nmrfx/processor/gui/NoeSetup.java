package org.nmrfx.processor.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class NoeSetup {

    Stage stage;
    BorderPane borderPane;
    double xOffset = 50;
    ButtonBase bButton;
    Popup popup;
    ComboBox<String> generate;

    ComboBox<NoeSet> noeSetCombo = new ComboBox();

    public NoeSetup () {
        stage = new Stage(StageStyle.DECORATED);
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("NoeSet Setup");
        stage.setAlwaysOnTop(true);
        initialize();
    }

    public void initialize() {
        stage = new Stage(StageStyle.DECORATED);
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("NoeSet Setup");
        stage.setAlwaysOnTop(true);

        Label nameLabel=new Label("Name:");
        String iconSize = "16px";
        String fontSize = "0pt";

        bButton = GlyphsDude.createIconButton(FontAwesomeIcon.PLUS_CIRCLE, "", iconSize, fontSize, ContentDisplay.TOP);
        bButton.setOnAction(e -> showPopup());
        bButton.getStyleClass().add("toolButton");
        bButton.setStyle("-fx-background-color: transparent;");

        UnaryOperator<TextFormatter.Change> nameFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([A-z0-9_\\-])*?")) {
                return change;
            }
            return null;
        };

        TextField editor = new TextField();
        editor.setTextFormatter(new TextFormatter<String>(new DefaultStringConverter(),"",nameFilter));
        editor.addEventFilter(KeyEvent.ANY, event -> {
            KeyCode code = event.getCode();
            if (event.getEventType()== KeyEvent.KEY_PRESSED) {
                if (code==KeyCode.ENTER) {
                    addNoeSet(editor.getText());
                    editor.clear();
                    popup.hide();
                }
                if (code==KeyCode.ESCAPE) {
                    editor.clear();
                    popup.hide();
                }
            }
        });
        editor.setPromptText("Name");

        popup = new Popup();
        popup.getContent().add(editor);

        noeSetCombo.setMaxWidth(Double.MAX_VALUE);
        noeSetCombo.getItems().setAll(UmbcProject.getActive().NOE_SETS.values());
        noeSetCombo.setPromptText("NOE Set:");
        noeSetCombo.setConverter(new StringConverter<NoeSet>() {

            @Override
            public String toString(NoeSet noeSet) {
                Optional<Map.Entry<String, NoeSet>> optionalEntry = UmbcProject.getActive().NOE_SETS.entrySet().stream().filter(ap -> ap.getValue().equals(noeSet)).findFirst();
                return (optionalEntry.map(Map.Entry::getKey).orElse(null));
            }

            @Override
            public NoeSet fromString(String string) {
                return UmbcProject.getActive().NOE_SETS.get(string);
            }
        });

        generate = new ComboBox<>();
        generate.setPromptText("Generate NOEs");
        generate.getItems().add("By attributes");
        generate.getItems().add("From Structure");
        generate.getItems().add("From SubProject NOEs");
        generate.setOnAction(e -> generateNoes());

        generate.disableProperty().bind(noeSetCombo.getSelectionModel().selectedItemProperty().isNull());
        HBox hBox=new HBox(nameLabel,noeSetCombo,bButton);
        hBox.setAlignment(Pos.CENTER_LEFT);

        Button ok = new Button("Close");
        ok.setOnAction((event) -> stage.close());

        ButtonBar buttonBar = new ButtonBar();
        ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
        buttonBar.getButtons().addAll(ok);

        borderPane.setTop(hBox);
        borderPane.setCenter(generate);
        borderPane.setBottom(buttonBar);
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

    private void addNoeSet(String name) {
        if (UmbcProject.getActive().NOE_SETS.get(name)!=null) {
            GUIUtils.warn("Error","NOE set "+name+" already exists. Please choose a new name.");
        } else {
            NoeSet noeSet=NoeSet.addSet(name);
            noeSetCombo.getItems().add(noeSet);
            noeSetCombo.setValue(noeSet);
        }
    }

    private void showPopup() {
        Bounds userTextFieldBounds = bButton.getBoundsInLocal();
        Point2D popupLocation = bButton.localToScreen(userTextFieldBounds.getMaxX(), userTextFieldBounds.getMinY());
        popup.show(bButton, popupLocation.getX(), popupLocation.getY());
    }

    private void generateNoes() {
        String method=generate.getValue();
        switch (method) {
            case "By attributes":
                generateNOEsByAttributes(noeSetCombo.getValue());
                break;
            case "From Structure":
                generateNOEsFromStructure(noeSetCombo.getValue());
                break;
            case "From SubProject NOEs":
                generateNOEsFromSubProject(noeSetCombo.getValue());
                break;
        }
        generate.setValue(null);
    }

    private void generateNOEsFromSubProject(NoeSet noeSet) {
        GUIUtils.warn("Sorry","Not yet implemented");
    }

    private void generateNOEsFromStructure(NoeSet noeSet) {
        GUIUtils.warn("Sorry","Not yet implemented");
    }

    private void generateNOEsByAttributes(NoeSet noeSet) {
        GUIUtils.warn("Sorry","Not yet implemented");
    }

    void cancel() {
        stage.close();
    }

    public Stage getStage() {
        return stage;
    }

}
