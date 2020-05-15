/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.RNALabels;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FXML Controller class
 *
 * @author Bruce Johnson
 */
public class RNALabelsSceneController implements Initializable {

    String baseAAtoms = "28";

    Pattern quickPat0 = Pattern.compile("^([AGCU][nrH1-9']+)+$");
    Pattern quickPat1 = Pattern.compile("([AGCU][nrH1-9']+)");
    Pattern quickPat2 = Pattern.compile("(n|r|H|[1-5]'|[2-8])");
    Pattern quickPatA = Pattern.compile("([1-5]'|[28])");
    Pattern quickPatG = Pattern.compile("([1-5]'|[8])");
    Pattern quickPatC = Pattern.compile("([1-5]'|[56])");
    Pattern quickPatU = Pattern.compile("([1-5]'|[56])");
    Background defaultBackground = null;
    Background errorBackground = new Background(new BackgroundFill(Color.YELLOW, null, null));

    @FXML
    TextField quickCodeField;
    @FXML
    GridPane adenosinePane;
    @FXML
    GridPane guanosinePane;
    @FXML
    GridPane uridinePane;
    @FXML
    GridPane cytidinePane;
    @FXML
    CheckBox d2oCheckBox;
    @FXML
    CheckBox carbonCheckBox;
    @FXML
    CheckBox nitrogenCheckBox;
    @FXML
    ListView<String> selGroupListView;
    @FXML
    Button clearSelGroupButton;
    @FXML
    Button clearAllSelGroupsButton;
    @FXML
    Button addSelGroupButton;
    @FXML
    Button loadSelGroupButton;
    @FXML
    Button applySelGroupButton;
    @FXML
    Button replaceSelGroupButton;
    @FXML
    Button showSelGroupButton;
    @FXML
    ChoiceBox<Entity> entityChoiceBox;
    @FXML
    TextField firstResidueField;
    @FXML
    TextField lastResidueField;
    @FXML
    private ComboBox<Sample> sampleField;

    ObservableList<String> selGroupList;

    Stage stage;
    String[][] baseAtoms = {
        {"H2", "C2", "H8", "C8"}, // Adenine
        {"H61", "H62", "N6"},
        {"H8", "C8"},// Guanine
        {"H21", "H22", "N2"},
        {"H5", "C5", "H6", "C6"},// Uridine
        {"H3", "N3"},
        {"H5", "C5", "H6", "C6"},//Cytosine
        {"H41", "H42", "N4"}
    };
    String[] riboseAtoms = {"H1'", "H2'", "H3'", "C1'", "C2'", "C3'", "H4'", "H5'", "H5''", "C4'", "C5'"};
    String[] baseChars = {"A", "G", "U", "C"};

    CheckBox[][][] checkBoxes = new CheckBox[4][3][];

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        GridPane[] gridPanes = {adenosinePane, guanosinePane, uridinePane, cytidinePane};

        double colWidth = 70;
        for (int iBase = 0; iBase < 4; iBase++) {
            gridPanes[iBase].getColumnConstraints().add(new ColumnConstraints(colWidth));
            gridPanes[iBase].getColumnConstraints().add(new ColumnConstraints(colWidth));
            gridPanes[iBase].getColumnConstraints().add(new ColumnConstraints(colWidth));
            gridPanes[iBase].getColumnConstraints().add(new ColumnConstraints(colWidth));
            int row = 0;
            Label label = new Label("Base");
            Button allButton = new Button("All");
            final int baseIndex = iBase;
            allButton.setOnAction(e -> {
                setAll(true, baseIndex, true);
            });
            Button noneButton = new Button("None");
            noneButton.setOnAction(e -> {
                setAll(true, baseIndex, false);
            });
            gridPanes[iBase].addRow(row, label, allButton, noneButton);
            row++;
            for (int iType = 0; iType < 2; iType++) {
                String[] atomNames = baseAtoms[iBase * 2 + iType];
                int iAtom = 0;
                checkBoxes[iBase][iType] = new CheckBox[atomNames.length];
                for (String aName : atomNames) {
                    CheckBox checkBox = new CheckBox(aName);
                    checkBoxes[iBase][iType][iAtom] = checkBox;
                    gridPanes[iBase].add(checkBox, iAtom, row);
                    if (aName.charAt(0) == 'C') {
                        checkBox.disableProperty().bind(carbonCheckBox.selectedProperty().not());
                    }
                    if (aName.charAt(0) == 'N') {
                        checkBox.disableProperty().bind(nitrogenCheckBox.selectedProperty().not());
                    }
                    if ((iType == 1) && aName.charAt(0) == 'H') {
                        checkBox.disableProperty().bind(d2oCheckBox.selectedProperty());
                    }

                    iAtom++;
                }
                row++;
            }
            Label riboLabel = new Label("Ribose");
            Button riboAllButton = new Button("All");
            riboAllButton.setOnAction(e -> {
                setAll(false, baseIndex, true);
            });
            Button riboNoneButton = new Button("None");
            riboNoneButton.setOnAction(e -> {
                setAll(false, baseIndex, false);
            });
            gridPanes[iBase].addRow(row, riboLabel, riboAllButton, riboNoneButton);
            row++;
            int col = 0;
            int iAtom = 0;
            checkBoxes[iBase][2] = new CheckBox[riboseAtoms.length];
            for (String aName : riboseAtoms) {
                CheckBox checkBox = new CheckBox(aName);
                checkBoxes[iBase][2][iAtom++] = checkBox;
                gridPanes[iBase].add(checkBox, col, row);
                if (aName.charAt(0) == 'C') {
                    checkBox.disableProperty().bind(carbonCheckBox.selectedProperty().not());
                }

                col++;
                if (col > 2) {
                    col = 0;
                    row++;
                }
            }
        }
        d2oCheckBox.setSelected(true);
        setD2O();
        d2oCheckBox.setOnAction(e -> setD2O());

        selGroupList = selGroupListView.getItems();
        clearSelGroupButton.disableProperty().bind(selGroupListView.getSelectionModel().selectedItemProperty().isNull());
        addSelGroupButton.disableProperty().bind(entityChoiceBox.getSelectionModel().selectedItemProperty().isNull());
        replaceSelGroupButton.disableProperty().bind(selGroupListView.getSelectionModel().selectedItemProperty().isNull());
        showSelGroupButton.disableProperty().bind(selGroupListView.getSelectionModel().selectedItemProperty().isNull());
        applySelGroupButton.disableProperty().bind(entityChoiceBox.getSelectionModel().selectedItemProperty().isNull());
        loadSelGroupButton.disableProperty().bind(entityChoiceBox.getSelectionModel().selectedItemProperty().isNull());
        sampleField.setOnShowing(e -> updateSample());
        entityChoiceBox.setOnShowing(e -> updateMolecule());
        quickCodeField.setOnKeyReleased(e -> {
            doQuickCode(e, quickCodeField.getText());

        });
        updateSample();
        updateMolecule();
        //entityChoiceBox.setValue("*");
        entityChoiceBox.setConverter(new StringConverter<Entity>() {
             @Override
             public String toString(Entity object) {
                 return object.getName();
             }
             @Override
             public Entity fromString(String string) {
                 return null;
             }
         }
        );

        sampleField.setOnAction(e -> {
            this.loadSelGroup();
        });

        entityChoiceBox.setOnAction(e -> {
            this.loadSelGroup();
        });
    }

    void doQuickCode(KeyEvent e, String code) {
        System.out.println("quick " + code);
        if (defaultBackground == null) {
            defaultBackground = quickCodeField.getBackground();
        }
        code = code.trim();
        if (code.length() == 0) {
            quickCodeField.setBackground(defaultBackground);
            if (e.getCode() == KeyCode.ENTER) {
                clearAllButtons();
            }
            return;
        }
        if (!quickPat0.matcher(code).matches()) {
            quickCodeField.setBackground(errorBackground);
            return;
        } else {
            quickCodeField.setBackground(defaultBackground);
        }

        Matcher matcher = quickPat1.matcher(code);
        StringBuilder sBuilder = new StringBuilder();
        boolean ok = false;
        while (matcher.find()) {
            System.out.println(matcher.group(1));
            Matcher matcher2 = quickPat2.matcher(matcher.group(1));
            String base = matcher.group(1).substring(0, 1);
            sBuilder.append("*:").append(base).append("*.");
            boolean first = true;
            while (matcher2.find()) {
                ok = true;
                if (!first) {
                    sBuilder.append(",");
                } else {
                    first = false;
                }
                String atomType = matcher2.group(1);
                System.out.println(atomType);
                if (atomType.equals("r")) {
                    sBuilder.append("Hr");
                } else if (atomType.equals("n")) {
                    sBuilder.append("Hn");
                } else {
                    if (atomType.length() != 2) {
                        Pattern basePat = null;
                        switch (base) {
                            case "A":
                                basePat = quickPatA;
                                break;
                            case "G":
                                basePat = quickPatG;
                                break;
                            case "C":
                            case "U":
                                basePat = quickPatC;
                                break;
                            default:
                                ok = false;
                                break;
                        }
                        if (!basePat.matcher(atomType).matches()) {
                            ok = false;
                        }
                    }
                    sBuilder.append("H").append(atomType);
                }
            }
            if (!ok) {
                break;
            }
            sBuilder.append(" ");
        }
        System.out.println(sBuilder.toString());
        if (ok) {
            if (e.getCode() == KeyCode.ENTER) {
                updateButtons(sBuilder.toString());
                quickCodeField.setText("");
            }
        } else {
            quickCodeField.setBackground(errorBackground);
        }

    }

    public void updateSample() {
        sampleField.getItems().clear();
        UmbcProject.getActive().sampleList.stream().sorted().forEach(sample -> {
            sampleField.getItems().add(sample);
        });
    }

    public void setSampleAndEntity(Sample sample,Entity entity) {
        try {
            sampleField.getSelectionModel().select(sample);
            entityChoiceBox.setValue(entity);
            this.loadSelGroup();
        } catch (Exception e) {
            System.out.println("No sample named "+sample);
        }
    }

    void updateMolecule() {
        ObservableList<Entity> entities = FXCollections.observableArrayList();
        //entityNames.add("*");
        Molecule mol = Molecule.getActive();
        if (mol != null) {
            entities.addAll(mol.entities.values());
        }
        entityChoiceBox.getItems().setAll(entities);

    }

    void setD2O() {
        boolean state = d2oCheckBox.isSelected();
        for (int iBase = 0; iBase < 4; iBase++) {
            for (CheckBox checkBox : checkBoxes[iBase][1]) {
                if (checkBox.getText().startsWith("H")) {
                    checkBox.setSelected(!state);
                }
            }
        }
    }

    @FXML
    void setAllBasesOn() {
        setAll(0, true);
    }

    @FXML
    void setAllBasesOff() {
        setAll(0, false);
    }

    @FXML
    void setAllRibosesOn() {
        setAll(2, true);
    }

    @FXML
    void setAllRibosesOff() {
        setAll(2, false);
    }

    void setAll(int iType, boolean state) {
        for (int iBase = 0; iBase < 4; iBase++) {
            for (CheckBox checkBox : checkBoxes[iBase][iType]) {
                if (!checkBox.isDisabled()) {
                    checkBox.setSelected(state);
                }
            }
        }
    }

    void setAll(boolean isBase, int iBase, boolean state) {
        int index = 0;
        if (!isBase) {
            index = 2;
        }
        for (CheckBox checkBox : checkBoxes[iBase][index]) {
            if (!checkBox.isDisabled()) {
                checkBox.setSelected(state);
            }
        }
        if (isBase) {
            for (CheckBox checkBox : checkBoxes[iBase][1]) {
                if (!checkBox.isDisabled()) {
                    checkBox.setSelected(state);
                }
            }
        }
    }

    @FXML
    void clearSelGroup() {
        int index = selGroupListView.getSelectionModel().getSelectedIndex();
        if (index != -1) {
            selGroupList.remove(index);
        }
    }

    @FXML
    void clearAllSelGroups() {
        selGroupList.clear();
    }

    @FXML
    void addSelGroup() {
        String selGroup = genSelGroup();
        selGroupList.add(selGroup);
    }

    @FXML
    void replaceSelGroup() {
        String selGroup = genSelGroup();
        int index = selGroupListView.getSelectionModel().getSelectedIndex();
        if (index != -1) {
            selGroupList.set(index, selGroup);
        }

    }

    @FXML
    void applySelGroup() {
        Sample sample = sampleField.getValue();
        Entity entity = entityChoiceBox.getValue();
        if (sample != null) {
            StringBuilder sBuilder = new StringBuilder();
            for (String selGroup : selGroupList) {
                if (sBuilder.length() != 0) {
                    sBuilder.append(';');
                }
                sBuilder.append(selGroup.trim());
            }
            sample.setEntityLabelString(entity,sBuilder.toString());
        }
    }

    @FXML
    void loadSelGroup() {
        Sample sample = sampleField.getValue();
        Entity entity = entityChoiceBox.getValue();
        selGroupList.clear();
        if (sample != null) {
            String selGroupPar = sample.getEntityLabelString(entity);
            String[] labelStrings = selGroupPar.split(";");
            for (String labelString : labelStrings) {
                if (!labelString.isEmpty()) {
                    selGroupList.add(labelString);
                }
            }
        }
    }

    @FXML
    void showSelGroup() {
        updateButtons();
    }

    @FXML
    public String genSelGroup() {
        String[] typeChars = {"n", "e", "r"};
        String[] atomChars = {"C", "N", "C"};
        String[] prefix = new String[4];
        String[] suffix = new String[4];
        for (int iBase = 0; iBase < 4; iBase++) {
            StringBuilder sBuilder = new StringBuilder();
            for (int iType = 0; iType < 3; iType++) {
                boolean allH = true;
                boolean allX = true;
                String[] atomNames;
                if (iType < 2) {
                    atomNames = baseAtoms[iBase * 2 + iType];
                } else {
                    atomNames = riboseAtoms;
                }
                int col = 0;
                int iAtom = 0;
                List<String> hNames = new ArrayList<>();
                List<String> xNames = new ArrayList<>();
                for (String aName : atomNames) {
                    CheckBox checkBox = checkBoxes[iBase][iType][iAtom++];
                    boolean status = !checkBox.isDisabled() && checkBox.isSelected();
                    if (aName.charAt(0) == 'H') {
                        if (status) {
                            hNames.add(aName);
                        } else {
                            allH = false;
                        }
                    } else if (aName.charAt(0) == 'C') {
                        if (status) {
                            xNames.add(aName);
                        } else {
                            allX = false;
                        }
                    } else if (aName.charAt(0) == 'N') {
                        if (status) {
                            xNames.add(aName);
                        } else {
                            allX = false;
                        }
                    }
                }
//                System.out.println(baseChars[iBase] + " " + iType + " " + allH + " " + allC + " " + allN);
//                System.out.println(hNames);
//                System.out.println(xNames);
                String sym = typeChars[iType];
                String xatomChar = atomChars[iType];
                if (allH) {
                    sBuilder.append(',').append("H").append(sym);
                } else {
                    for (String name : hNames) {
                        sBuilder.append(',').append(name);
                    }
                }
                if (allX) {
                    sBuilder.append(',').append(xatomChar).append(sym);
                } else {
                    for (String name : xNames) {
                        sBuilder.append(',').append(name);
                    }
                }
            }
            StringBuilder sAtoms = new StringBuilder();

            if (sBuilder.length() > 0) {
                //sAtoms.append(entityChoiceBox.getValue().getName()).append(":");
                sAtoms.append(baseChars[iBase]);
                String range = getResidueRange();
                sAtoms.append(range).append('.');
                prefix[iBase] = sAtoms.toString();
                suffix[iBase] = sBuilder.substring(1);
            } else {
                prefix[iBase] = "";
                suffix[iBase] = "";
            }
        }
        boolean allMatch = true;
        boolean empty = true;
        for (int iBase = 0; iBase < 4; iBase++) {
            if (!suffix[iBase].equals(suffix[0])) {
                allMatch = false;
            }
            if (!suffix[iBase].equals("")) {
                empty = false;
            }
        }
        StringBuilder result = new StringBuilder();
        if (!empty) {
            String entityStr = entityChoiceBox.getValue().getName() + ":";
            if (!allMatch) {
                boolean[] done = new boolean[4];
                for (int iBase = 0; iBase < 4; iBase++) {
                    if (!done[iBase]) {
                        done[iBase] = true;
                        StringBuilder baseValue = new StringBuilder();
                        baseValue.append(baseChars[iBase]);
                        for (int jBase = iBase + 1; jBase < 4; jBase++) {
                            if (suffix[iBase].equals(suffix[jBase])) {
                                baseValue.append(',').append(baseChars[jBase]);
                                done[jBase] = true;
                            }
                        }
                        String range = getResidueRange();
                        baseValue.append(range).append('.');
                        if (suffix[iBase].length() > 0) {
                            if (iBase != 0) {
                                result.append(' ');
                            }
                            //result.append(entityStr).append(baseValue.toString()).append(suffix[iBase]);
                            result.append(baseValue.toString()).append(suffix[iBase]);
                        }
                    }
                }
            } else {
                //result.append(entityStr);
                String range = getResidueRange();
                result.append(range).append('.');
                result.append(suffix[0]);
            }
        }
        System.out.println("selAtoms: " + result.toString());
        return result.toString().trim();
    }

    public String getResidueRange() {
        String range = "*";
        String firstResidue = firstResidueField.getText().trim();
        String lastResidue = lastResidueField.getText().trim();
        if (!firstResidue.equals("") || !lastResidue.equals("")) {
            if (!firstResidue.equals("") && !lastResidue.equals("")) {
                if (firstResidue.equals(lastResidue)) {
                    range = firstResidue;
                } else {
                    range = firstResidue + "-" + lastResidue;
                }
            } else if (firstResidue.equals("") && !lastResidue.equals("")) {
                range = "-" + lastResidue;
            } else if (!firstResidue.equals("") && lastResidue.equals("")) {
                range = firstResidue + "-";
            }
        }
        return range;

    }

    void clearAllButtons() {
        for (int iBase = 0; iBase < 4; iBase++) {
            for (int iType = 0; iType < 3; iType++) {
                for (CheckBox checkBox : checkBoxes[iBase][iType]) {
                    checkBox.setSelected(false);
                }
            }
        }

    }

    void updateButtons() {
        clearAllButtons();
        firstResidueField.setText("");
        lastResidueField.setText("");
        //entityChoiceBox.setValue(null);
        int index = selGroupListView.getSelectionModel().getSelectedIndex();
        if (index != -1) {
            String selGroupEntry = selGroupList.get(index);
            updateButtons(selGroupEntry);
        }
    }

    void updateButtons(String selGroupEntry) {
        if (selGroupEntry.length() > 0) {
            String[] selGroupStrs = selGroupEntry.split(" ");
            for (String selGroupStr : selGroupStrs) {
                RNALabels.SelGroup selGroup = RNALabels.parseSelGroup(selGroupStr);
                if (selGroup.firstRes != null) {
                    firstResidueField.setText(String.valueOf(selGroup.firstRes));
                }
                if (selGroup.lastRes != null) {
                    lastResidueField.setText(String.valueOf(selGroup.lastRes));
                }
                Entity entity=Molecule.getActive().entities.get(selGroup.entityStr);

                entityChoiceBox.setValue(entity);
                for (int iBase = 0; iBase < 4; iBase++) {
                    if (RNALabels.checkResType(baseChars[iBase], selGroup.resTypes)) {
                        for (int iType = 0; iType < 3; iType++) {
                            for (CheckBox checkBox : checkBoxes[iBase][iType]) {
                                boolean exchangeable = iType == 1;
                                boolean ribose = iType == 2;
                                boolean state = RNALabels.checkAtom(checkBox.getText(), checkBox.getText().substring(0, 1), selGroup.gAtomNames, ribose, exchangeable);
                                if (state) {
                                    checkBox.setSelected(true);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public Stage getStage() {
        return stage;
    }

    public static RNALabelsSceneController create() {
        FXMLLoader loader = new FXMLLoader(MinerController.class.getResource("/RNALabelsScene.fxml"));
        RNALabelsSceneController controller = null;
        Stage stage = new Stage(StageStyle.DECORATED);
        try {
            Scene scene = new Scene((Pane) loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");
            scene.getStylesheets().add("/styles/rnapeakgeneratorscene.css");

            controller = loader.<RNALabelsSceneController>getController();
            controller.stage = stage;
            stage.setTitle("RNA Label Schemes");
            stage.show();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return controller;

    }

}
