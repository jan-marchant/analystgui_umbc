package org.nmrfx.processor.gui;

import com.sun.javafx.tk.Toolkit;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.nmrfx.processor.datasets.peaks.AtomResonance;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakDim;
import org.nmrfx.project.Project;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AcqNodeChooser {
    /** popup on adding peak to managed list.
     * uses
     * public HashMap<ExpDim, List<AcqNode>> getPossiblePathNodes(HashMap<ExpDim, AcqNode> pickedNodes)
     * where pickedNodes describes the nodes selected.
     */
    HashMap<ExpDim, ObservableList<AcqNode>> possibleNodes=new HashMap<>();
    HashMap<ExpDim,ComboBox<AcqNode>> combos=new HashMap<>();
    Acquisition acquisition;
    Stage stage;
    GridPane grid;
    ArrayList<ExpDim> expDims=new ArrayList<>();
    double tol = 0.04;
    HashMap<AcqNode,String> nodeLabs=new HashMap<>();
    ManagedList list;
    Peak peak;
    ObservableMap<ExpDim,AcqNode> pickedSet=FXCollections.observableMap(new HashMap<>());
    int numExpDims;
    float matchTolerance=0.1f;

    public AcqNodeChooser(ManagedList list, Peak peak) {
        this.acquisition = list.getAcquisition();
        this.list=list;
        this.peak=peak;
        initPossibleNodes();
    }


    class NodeComparator implements Comparator<AcqNode> {
        Float shift;

        public NodeComparator(Float shift) {
            this.shift=shift;
        }

        @Override
        public int compare(AcqNode a, AcqNode b) {
            if (shift==null) {return 0;}
            double aDelta=a.getDeltaPPM(shift);
            double bDelta=b.getDeltaPPM(shift);
            return aDelta < bDelta ? -1 : aDelta == bDelta ? 0 : 1;
        }
    }

    public void create() {
        stage = new Stage(StageStyle.UTILITY);
        grid=new GridPane();
        Scene scene = new Scene(grid);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Assign new peak");
        numExpDims=0;
        //fixme: if multiple NOE dims need to add way to apportion intensity - need to think a bit harder about intensity anyway
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            if (expDim.isObserved() || expDim.isNoeDim()) {
                Float shift;
                try {
                    shift = peak.getPeakDim(list.getDimMap().get(expDim)).getChemShiftValue();
                } catch (Exception e) {
                    shift=null;
                }
                NodeComparator comparator = new NodeComparator(shift);

                Collections.sort(possibleNodes.get(expDim),comparator);
                Label label=new Label(expDim.toString());
                String shiftString;
                if (shift!=null) {
                    shiftString=String.format("\t%.2f",shift)+" ppm";
                } else {
                    shiftString="";
                }
                Label label2=new Label(shiftString);
                ComboBox<AcqNode> comboBox = new ComboBox();
                combos.put(expDim, comboBox);
                comboBox.setItems(possibleNodes.get(expDim));
                comboBox.setEditable(false);

                Float finalShift = shift;
                comboBox.setConverter(new StringConverter<AcqNode>() {

                    @Override
                    public String toString(AcqNode node) {
                        if (finalShift==null) {
                            return String.format("%-10.10s",node.toString());
                        } else {
                            return String.format("%-10.10s %5.3f", node.toString(), node.getDeltaPPM(finalShift));
                        }
                    }

                    @Override
                    public AcqNode fromString(String string) {
                        //return comboBox.getItems().stream().filter(ap ->ap.getName().equals(string)).findFirst().orElse(null);
                        return null;
                    }
                });

                //comboBox.getEditor().setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
                comboBox.setStyle("-fx-font: 16px \"Courier New\";");

                TextField textField = new TextField();
                textField.setPromptText("Search");
                SuggestionProvider<AcqNode> provider = new SuggestionProvider<AcqNode>() {
                    @Override
                    protected Comparator<AcqNode> getComparator() {
                        return comparator;
                    }

                    @Override
                    protected boolean isMatch(AcqNode suggestion, AutoCompletionBinding.ISuggestionRequest request) {
                        String userTextLower = request.getUserText().toLowerCase();
                        String suggestionStr = suggestion.toString().toLowerCase();
                        return suggestionStr.contains(userTextLower)
                                && !suggestionStr.equals(userTextLower);
                    }
                };
                provider.addPossibleSuggestions(comboBox.getItems());
                AutoCompletionBinding<AcqNode> acb = new AutoCompletionTextFieldBinding<>(textField, provider);
                acb.setOnAutoCompleted(event -> {
                    AcqNode node = event.getCompletion();
                    expDims.clear();
                    textField.clear();
                    comboBox.setValue(node);
                });
                acb.setVisibleRowCount(10);

                textField.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ENTER) | keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        expDims.clear();
                        textField.clear();
                        comboBox.setValue(null);
                    }
                });

                comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue==null) {
                        pickedSet.remove(expDim);
                    } else {
                        pickedSet.put(expDim,newValue);
                    }
                    expDims.add(expDim);
                    updatePossibleNodes(newValue);
                    textField.clear();
                });
                HBox hBox=new HBox(label,label2);
                grid.add(hBox,numExpDims,0);
                grid.add(comboBox, numExpDims, 1);
                grid.add(textField, numExpDims++, 2);
            }
        }
        if (numExpDims>0) {
            Button ok = new Button("OK");
            Button cancel = new Button("Cancel");
            ok.setOnAction((event) -> {
                processPickedNodes();
                stage.close();
            });

            ok.setDisable(true);

            cancel.setOnAction((event) -> {
                stage.close();
            });

            ButtonBar buttonBar = new ButtonBar();
            ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
            ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE);
            buttonBar.getButtons().addAll(cancel, ok);
            grid.add(buttonBar, 0, 3, numExpDims, 1);

            pickedSet.addListener((MapChangeListener<? super ExpDim,? super AcqNode>) e -> {
                if (pickedSet.size() == numExpDims) {
                    ok.setDisable(false);
                    ok.requestFocus();
                } else {
                    ok.setDisable(true);
                }
            });
        } else {
            Label errorLabel=new Label("Something has gone wrong!");
            grid.add(errorLabel,0,0);
        }
    }

    public void showAndWaitAtMouse() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        List<Screen> screens = Screen.getScreens();

        stage.setAlwaysOnTop(true);
        stage.setResizable(false);

        Platform.runLater(() -> {
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
        });

        stage.showAndWait();
    }

    private void initPossibleNodes() {
        possibleNodes = acquisition.getAcqTree().getPossiblePathNodes(null);
    }

    private void updatePossibleNodes(AcqNode node) {
        HashMap<ExpDim, ObservableList<AcqNode>> candidates;
        candidates = acquisition.getAcqTree().getPossiblePathNodes(node);
        for (ExpDim targetExpDim : candidates.keySet()) {
            if (!expDims.contains(targetExpDim)) {
                if (!candidates.get(targetExpDim).contains(combos.get(targetExpDim).getValue())) {
                    combos.get(targetExpDim).setValue(null);
                }
                possibleNodes.putIfAbsent(targetExpDim,FXCollections.observableArrayList());
                possibleNodes.get(targetExpDim).retainAll(candidates.get(targetExpDim));
                for (AcqNode candidateNode : candidates.get(targetExpDim)) {
                    if (!possibleNodes.get(targetExpDim).contains(candidateNode)) {
                        possibleNodes.get(targetExpDim).add(candidateNode);
                    }
                }
                if (possibleNodes.get(targetExpDim).size()==1) {
                    combos.get(targetExpDim).setValue(possibleNodes.get(targetExpDim).get(0));
                }
            }
        }
    }

    private void processPickedNodes() {
        for (Map.Entry<ExpDim,AcqNode> picked : pickedSet.entrySet()) {
            ExpDim expDim=picked.getKey();
            AcqNode node=picked.getValue();
            Atom atom=node.getAtom();

            if (expDim.isObserved()) {
                PeakDim peakDim = peak.getPeakDim(list.getDimMap().get(expDim));
                float pickedShift=peakDim.getChemShift();

                if (atom.getResonance()==null) {
                    ((AtomResonance) peakDim.getResonance()).setAtom(atom);
                    atom.setResonance((AtomResonance) peakDim.getResonance());
                } else {
                    atom.getResonance().add(peakDim);
                    Set<PeakDim> updateMe = new HashSet<>();
                    updateMe.add(peakDim);
                    boolean freezeMe = false;

                    for (PeakDim testPeakDim : atom.getResonance().getPeakDims()) {
                        if (testPeakDim == peakDim) {
                            continue;
                        }
                        if (testPeakDim.getSampleConditionLabel().equals(peakDim.getSampleConditionLabel())) {
                            if (testPeakDim.isFrozen()) {
                                if (Math.abs(testPeakDim.getChemShift() - pickedShift) > matchTolerance) {
                                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                    alert.setTitle("Clash detected");
                                    alert.setHeaderText("Your assignment clashes with existing frozen peaks");
                                    alert.setContentText("e.g. "+testPeakDim.getPeak()+
                                            " is frozen at "+testPeakDim.getChemShiftValue()+" ppm. Do you wish to:");

                                    ButtonType shiftFrozen = new ButtonType("Shift Existing");
                                    ButtonType shiftNew = new ButtonType("Shift New");
                                    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                                    alert.getButtonTypes().setAll(shiftFrozen, shiftNew, cancel);

                                    Optional<ButtonType> result = alert.showAndWait();
                                    if (result.get() == shiftFrozen){
                                        testPeakDim.setFrozen(false);
                                        updateMe.add(testPeakDim);
                                    } else if (result.get() == shiftNew) {
                                        pickedShift = testPeakDim.getChemShift();
                                    } else {
                                        return;
                                    }
                                } else if (testPeakDim.getSampleLabel().equals(peakDim.getSampleLabel())) {
                                    pickedShift = testPeakDim.getChemShift();
                                    freezeMe = true;
                                }
                            }  else {
                                updateMe.add(testPeakDim);
                            }
                        }

                        for (PeakDim updatePeakDim : updateMe) {
                            updatePeakDim.setChemShift(pickedShift);
                            if (updatePeakDim.getSpectralDimObj() == peakDim.getSpectralDimObj()) {
                                updatePeakDim.setLineWidthValue(peakDim.getLineWidthValue());
                                updatePeakDim.setBoundsValue(peakDim.getBoundsValue());
                            }
                        }
                        peakDim.setFrozen(freezeMe);
                    }
                }
            }
        }
        List<Noe> noesToAdd = new ArrayList<>();
        for (Map.Entry<ExpDim,AcqNode> picked : pickedSet.entrySet()) {
            ExpDim expDim = picked.getKey();
            Connectivity nextCon = expDim.getNextCon();
            if (nextCon != null && nextCon.type == Connectivity.TYPE.NOE) {
                AcqNode node1 = picked.getValue();
                AcqNode node2 = pickedSet.get(expDim.getNextExpDim());
                Atom atom1 = node1.getAtom();
                Atom atom2 = node2.getAtom();
                double noeFraction = acquisition.getSample().getAtomFraction(atom1) * acquisition.getSample().getAtomFraction(atom2);
                if (noeFraction <= 0) {
                    return;
                }
                if (!expDim.resPatMatches(atom1, atom2)) {
                    return;
                }
                //todo: add ppm set support
                if (!noeExists(list.noeSet,atom1,atom2)) {
                    if (atom1.getResonance()==null) {
                        atom1.setResonance((AtomResonance) Project.getActive().resFactory.build());
                    }
                    if (atom2.getResonance()==null) {
                        atom2.setResonance((AtomResonance) Project.getActive().resFactory.build());
                    }
                    Noe noe = new Noe(peak, atom1.getSpatialSet(), atom2.getSpatialSet(), noeFraction, atom1.getResonance(), atom2.getResonance());
                    //fixme: apportion intensity where multiple NOE dims
                    noe.setIntensity(peak.getIntensity());
                    noesToAdd.add(noe);
                }
            }
        }
        for (Noe noe: noesToAdd) {
            if (list.getAddedNoe()==null) {
                list.setAddedNoe(noe);
            }
        list.noeSet.add(noe);
        }
    }

    private boolean noeExists(NoeSet noeSet,Atom atom1,Atom atom2) {
        for (Noe noe : noeSet.get()) {
            if (atom1 == noe.spg1.getAnAtom() && atom2 == noe.spg2.getAnAtom() ||
                    atom2 == noe.spg1.getAnAtom() && atom1 == noe.spg2.getAnAtom()) {
                return true;
            }
        }
        return false;
    }

}
