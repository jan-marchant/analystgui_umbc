package org.nmrfx.processor.gui;

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class AcqNodeChooser {
    /** popup on adding peak to managed list.
     * uses
     * public HashMap<ExpDim, List<AcqNode>> getPossiblePathNodes(HashMap<ExpDim, AcqNode> pickedNodes)
     * where pickedNodes describes the nodes selected.
     */
    HashMap<ExpDim, ObservableList<AcqNode>> possibleNodes=new HashMap<>();
    ObservableMap<ExpDim, AcqNode> pickedNodes=FXCollections.observableMap(new HashMap<>());
    Acquisition acquisition;
    Stage stage;
    GridPane grid;

    public AcqNodeChooser(Acquisition acquisition) {
        this.acquisition=acquisition;
        pickedNodes.addListener((MapChangeListener<ExpDim, AcqNode>) change -> {
            updatePossibleNodes();
        });
        updatePossibleNodes();
        stage = new Stage(StageStyle.DECORATED);
        grid=new GridPane();
        Scene scene = new Scene(grid);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("AutoComplete Nodes");
        int i=0;
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            ComboBox<AcqNode> comboBox=new ComboBox();
            ObservableList<AcqNode> list=possibleNodes.get(expDim);
            comboBox.setItems(list);
            comboBox.setEditable(true);
            AutoCompletionBinding<AcqNode> acb =TextFields.bindAutoCompletion(comboBox.getEditor(),comboBox.getItems());
            acb.setOnAutoCompleted(event -> {
                pickedNodes.put(expDim,event.getCompletion());
            });
            grid.add(comboBox,i++,0);
        }
        stage.setAlwaysOnTop(true);
        stage.show();
        /*SuggestionProvider<String> provider = SuggestionProvider.create(Collections.emptyList());
        AtomicReference<Boolean> cleared= new AtomicReference<>(false);

        new AutoCompletionTextFieldBinding<>(comboBox.getEditor(), provider).setVisibleRowCount(10);
        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, b, c) -> {
            provider.clearSuggestions();
            cleared.set(true);
        });

        comboBox.getEditor().setOnKeyPressed(e -> {
            if (cleared.get()){
                provider.clearSuggestions();
                provider.addPossibleSuggestions(comboBox.getItems());
                cleared.set(false);
            }
        });
         */

    }

    private void updatePossibleNodes() {
        possibleNodes=acquisition.getAcqTree().getPossiblePathNodes(pickedNodes);
    }
}
