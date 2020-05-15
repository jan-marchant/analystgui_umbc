package org.nmrfx.processor.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.nmrfx.processor.datasets.peaks.AtomResonance;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakDim;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.util.*;

public class AcqNodeChooser {
    /** popup on adding peak to managed list.
     * uses
     * public HashMap<ExpDim, List<AcqNode>> getPossiblePathNodes(HashMap<ExpDim, AcqNode> pickedNodes)
     * where pickedNodes describes the nodes selected.
     */
    HashMap<ExpDim, ObservableList<AcqNode>> possibleNodes=new HashMap<>();
    HashMap<ExpDim,ComboBox> combos=new HashMap<>();
    Acquisition acquisition;
    Stage stage;
    GridPane grid;
    ArrayList<ExpDim> expDims=new ArrayList<>();
    double tol = 0.04;
    HashMap<AcqNode,String> nodeLabs=new HashMap<>();
    ManagedList list;
    Peak peak;
    double xOffset = 50;
    ObservableMap<ExpDim,AcqNode> pickedSet=FXCollections.observableMap(new HashMap<>());
    int numExpDims;

    public AcqNodeChooser(ManagedList list, Peak peak) {
        this.acquisition = list.getAcquisition();
        this.list=list;
        this.peak=peak;
        initPossibleNodes();
    }

    public void create() {
        stage = new Stage(StageStyle.DECORATED);
        grid=new GridPane();
        Scene scene = new Scene(grid);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Assign new peak");
        numExpDims=0;
        //fixme: if multiple NOE dims need to add way to apportion intensity - need to think a bit harder about intensity anyway
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            if (expDim.isObserved() || expDim.isNoeDim()) {
                double shift;
                try {
                    shift = peak.getPeakDim(list.getDimMap().get(expDim)).getChemShiftValue();
                } catch (Exception e) {
                    shift=-100.0;
                }
                //Update method to (also) use atom chemical shift assignment and rppm
                List<AtomBrowser.AtomDelta> atoms1 = AtomBrowser.getMatchingAtomNames(acquisition.getDataset(), shift, tol);
                //put these nodes to front of list
                for (int j = atoms1.size(); j-- > 0; ) {
                    AtomBrowser.AtomDelta atom = atoms1.get(j);
                    Atom atom1=acquisition.getSample().getMolecule().findAtom(atom.getName());
                    if (atom1!=null) {
                        AcqNode node=acquisition.getAcqTree().getNode(expDim,atom1);
                        if (node!=null) {
                            possibleNodes.get(expDim).remove(node);
                            possibleNodes.get(expDim).add(0,node);
                            nodeLabs.put(node,String.format("%2d%%",Math.round(99.0 * atom.fDelta)));
                        }
                    }
                }
                Label label=new Label(expDim.toString());
                String shiftString;
                if (shift>-99) {
                    shiftString=String.format("\t%.2f",shift)+" ppm";
                } else {
                    shiftString="";
                }
                Label label2=new Label(shiftString);
                ComboBox<AcqNode> comboBox = new ComboBox();
                combos.put(expDim, comboBox);
                comboBox.setItems(possibleNodes.get(expDim));
                comboBox.setEditable(false);

                comboBox.setConverter(new StringConverter<AcqNode>() {

                    @Override
                    public String toString(AcqNode node) {
                        String returnString=node.toString();
                        if (nodeLabs.containsKey(node)) {
                            returnString+=" "+nodeLabs.get(node);
                        } else {
                            returnString+=" -";
                        }
                        return returnString;
                    }

                    @Override
                    public AcqNode fromString(String string) {
                        //return comboBox.getItems().stream().filter(ap ->ap.getName().equals(string)).findFirst().orElse(null);
                        return null;
                    }
                });

                TextField textField = new TextField();
                textField.setPromptText("Search");
                AutoCompletionBinding<AcqNode> acb = TextFields.bindAutoCompletion(textField, comboBox.getItems());
                acb.setOnAutoCompleted(event -> {
                    AcqNode node = event.getCompletion();
                    expDims.clear();
                    textField.clear();
                    comboBox.setValue(node);
                });

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
                } else {
                    ok.setDisable(true);
                }
            });
        } else {
            Label errorLabel=new Label("Something has gone wrong!");
            grid.add(errorLabel,0,0);
        }
    }

    public void showAndWait(double x, double y) {
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        if (x > (screenWidth / 2)) {
            x = x - stage.getWidth() - xOffset;
        } else {
            x = x + 100;
        }

        y = y - stage.getHeight() / 2.0;
        stage.setX(x);
        stage.setY(y);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
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
        //need to process existing peaks on list here - and offer multiple ppm sets
        for (Map.Entry<ExpDim,AcqNode> picked : pickedSet.entrySet()) {
            ExpDim expDim=picked.getKey();
            Connectivity nextCon=expDim.getNextCon();
            if (nextCon!=null && nextCon.type== Connectivity.TYPE.NOE) {
                AcqNode node1=picked.getValue();
                AcqNode node2=pickedSet.get(expDim.getNextExpDim());
                Atom atom1=node1.getAtom();
                Atom atom2=node2.getAtom();
                double noeFraction = acquisition.getSample().getAtomFraction(atom1) * acquisition.getSample().getAtomFraction(atom2);
                boolean add=true;
                if (noeFraction <= 0) {
                    add = false;
                }
                if (!expDim.resPatMatches(atom1,atom2)) {
                    add=false;
                }
                if (add) {
                    //todo ppm set support
                    if (!noeExists(list.noeSet, atom1,atom2)) {
                        //fixme: is this an OK use of newScale?
                        //fixme: apportion intensity where multiple NOE dims
                        if (node1 != null && node2 != null) {
                            PeakDim peakDim1=null;
                            PeakDim peakDim2=null;
                            if (expDim.isObserved()) {
                                peakDim1=peak.getPeakDim(list.getDimMap().get(expDim));
                                if (atom1.getResonance()!=null) {
                                    peakDim1.setResonance(atom1.getResonance());
                                } else {
                                    ((AtomResonance) peakDim1.getResonance()).setAtom(atom1);
                                    atom1.setResonance((AtomResonance) peakDim1.getResonance());
                                }
                            }
                            if (expDim.getNextExpDim().isObserved()) {
                                peakDim2=peak.getPeakDim(list.getDimMap().get(expDim.getNextExpDim()));
                                if (atom2.getResonance()!=null) {
                                    peakDim2.setResonance(atom2.getResonance());
                                } else {
                                    ((AtomResonance) peakDim2.getResonance()).setAtom(atom2);
                                    atom2.setResonance((AtomResonance) peakDim2.getResonance());
                                }

                            }
                            Noe noe = new Noe(peak, atom1.getSpatialSet(), atom2.getSpatialSet(), noeFraction,(AtomResonance) peakDim1.getResonance(),(AtomResonance) peakDim2.getResonance());
                            noe.setIntensity(peak.getIntensity());

                            if (list.getAddedNoe()==null) {
                                list.setAddedNoe(noe);
                            }
                            list.noeSet.add(noe);
                        }
                    }
                }

            }
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
