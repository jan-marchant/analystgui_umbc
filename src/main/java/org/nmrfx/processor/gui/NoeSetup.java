package org.nmrfx.processor.gui;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
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
import org.apache.commons.collections4.BidiMap;
import org.nmrfx.processor.datasets.peaks.AtomResonance;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;
import org.python.modules._hashlib;
import org.python.modules.math;
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;

public class NoeSetup implements SubProjMenu {

    Stage stage;
    BorderPane borderPane;
    double xOffset = 50;
    ButtonBase bButton;
    Popup popup;
    MenuButton generate;

    ComboBox<NoeSet> noeSetCombo = new ComboBox<>();

    List<ResidueDistances> distances = new ArrayList<>();

    class ResidueDistances {
        String type;
        String res1;
        String res2;
        HashMap<String[],double []> distances = new HashMap<>();

        ResidueDistances(String type, String res1, String res2) {
            this.type=type;
            this.res1=res1;
            this.res2=res2;
        }

        public void add (String a1,String a2,double distance, double nInst) {
            String[] sArr = new String[2];
            double[] dArr = new double[2];
            sArr[0]=a1;
            sArr[1]=a2;
            dArr[0]=0;
            dArr[1]=0;
            distances.putIfAbsent(sArr,dArr);
            distances.get(sArr)[0]+=distance*nInst;
            distances.get(sArr)[1]+=nInst;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ResidueDistances) {
                //System.out.println("Comparing "+type+res1+res2+" with "+((ResidueDistances) obj).type+((ResidueDistances) obj).res1+((ResidueDistances) obj).res2);
                if (type.equals(((ResidueDistances) obj).type) &&
                        res1.equals(((ResidueDistances) obj).res1) &&
                        res2.equals(((ResidueDistances) obj).res2)) {
                    return true;
                }
            }
            return false;
        }
    }

    {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            InputStream iStream = cl.getResourceAsStream("data/res_pair_table.txt");
            Scanner inputStream = new Scanner(iStream);
            int row=0;
            while (inputStream.hasNextLine()) {
                String line = inputStream.nextLine();
                if (!line.isEmpty()) {
                    String[] data = line.split("\t");
                    if (data.length != 9) {
                        System.out.println("Check res_pair_table line " + row);
                    }
                    if (row++ == 0) {
                        continue;
                    }
                    //interType,res1,res2,atom1,atom2,minDis,maxDis,avgDis,nInst
                    if (data[3].charAt(data[3].length()-1)=='\'') {
                        data[1]="r";
                    }
                    if (data[4].charAt(data[4].length()-1)=='\'') {
                        data[2]="r";
                    }
                    ResidueDistances distance = new ResidueDistances(data[0].trim(),data[1].trim(),data[2].trim());
                    int index = distances.indexOf(distance);
                    if (index==-1) {
                        distance.add(data[3].trim(),data[4].trim(),Double.parseDouble(data[7]),Double.parseDouble(data[8]));
                        distances.add(distance);
                        //System.out.println("Added new ResidueDistance "+data[0].trim()+data[1].trim()+data[2].trim());
                    } else {
                        distances.get(index).add(data[3].trim(),data[4].trim(),Double.parseDouble(data[7]),Double.parseDouble(data[8]));
                        //System.out.println("Added new distances to "+data[0].trim()+data[1].trim()+data[2].trim());
                    }
                }
            }
            iStream.close();
        } catch (IOException e) {
            System.out.println("Couldn't read res_pair_table");
        }
    }

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

        generate = new MenuButton("Generate NOEs");
        Menu attr = new Menu("By Attributes");
        Menu struct = new Menu("From Structure");
        Menu sub = new Menu("From SubProject NOEs");

        MenuItem setVienna = new MenuItem("Setup Vienna");
        MenuItem setShifts = new MenuItem("Predict shifts");
        MenuItem gen = new MenuItem("Generate");
        gen.setOnAction(e -> generateNOEsByAttributes(noeSetCombo.getValue()));

        attr.getItems().addAll(setVienna,setShifts,gen);

        MenuItem notYet = new MenuItem("Not Implemented Yet");
        struct.getItems().add(notYet);

        for (Object menu : UmbcProject.getActive().getSubProjMenus(this)) {
            if (menu instanceof Menu) {
                sub.getItems().add((Menu) menu);
            } else {
                sub.getItems().add((MenuItem) menu);
            }
        }

        generate.getItems().addAll(attr,struct,sub);

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

    @Override
    public void setSubProject(UmbcProject subProject) {
            generateNOEsFromSubProject(subProject,noeSetCombo.getValue());
            generate.hide();
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

    private void generateNOEsFromSubProject(UmbcProject project, NoeSet noeSet) {
        //Should update this to prompt if more than one NOE Set
        for (NoeSet noeSet1 : project.NOE_SETS.values()) {
            for (Noe noe1 : noeSet1.get()) {
                Atom origAtom1 = noe1.spg1.getAnAtom();
                Atom origAtom2 = noe1.spg2.getAnAtom();
                Atom atom1=null;
                Atom atom2=null;
                BidiMap<Entity, Entity> map = UmbcProject.getActive().entityMap.get(project);
                if (map.inverseBidiMap().containsKey(origAtom1.getEntity())) {
                    Entity otherEntity = map.inverseBidiMap().get(origAtom1.getEntity());
                    for (Atom otherAtom : otherEntity.getAtoms()) {
                        if (otherAtom.getName().equals(origAtom1.getName())) {
                            atom1=otherAtom;
                        }
                    }
                }
                if (map.inverseBidiMap().containsKey(origAtom2.getEntity())) {
                    Entity otherEntity = map.inverseBidiMap().get(origAtom2.getEntity());
                    for (Atom otherAtom : otherEntity.getAtoms()) {
                        if (otherAtom.getName().equals(origAtom2.getName())) {
                            atom2=otherAtom;
                        }
                    }
                }

                if (atom1==null || atom2 == null) {continue;}
                if (!noeExists(noeSet, atom1, atom2)) {
                    if (atom1.getResonance() == null) {
                        atom1.setResonance((AtomResonance) Project.getActive().resFactory.build());
                    }
                    if (atom2.getResonance() == null) {
                        atom2.setResonance((AtomResonance) Project.getActive().resFactory.build());
                    }
                    Noe noe = new Noe(null, atom1.getSpatialSet(), atom2.getSpatialSet(), noe1.getScale(), atom1.getResonance(), atom2.getResonance());
                    noe.setIntensity(noe1.getIntensity());
                    noeSet.add(noe);
                }
            }
        }
    }

    private void generateNOEsFromStructure(NoeSet noeSet) {
        GUIUtils.warn("Sorry","Not yet implemented");
    }

    private void generateNOEsByAttributes(NoeSet noeSet) {
        Molecule mol = noeSet.molecule;
        if (mol==null) {
            noeSet.molecule=Molecule.getActive();
            mol=noeSet.molecule;
        }
        String vienna = mol.getDotBracket();
        if (vienna=="") {
            GUIUtils.warn("No Vienna","Please enter secondary structure before running.");
            return;
        }
        SSGen ss = new SSGen(mol, vienna);
        //iterate through every combination of two residues
        List<Residue> rnaResidues = new ArrayList<>();
        for (Polymer polymer : mol.getPolymers()) {
            if (polymer.isRNA()) {
                rnaResidues.addAll(polymer.getResidues());
            }
        }
        int start=0;
        for (Residue residue : rnaResidues) {
            for (int i = start;i<rnaResidues.size();i++) {
                Residue residue2=rnaResidues.get(i);
                String iType = InteractionType.determineType(residue,residue2);

                if (iType==null) {continue;}
                List<ResidueDistances> rDists = new ArrayList<>();
                rDists.add(new ResidueDistances(iType,""+residue.getOneLetter(),""+residue2.getOneLetter()));
                rDists.add(new ResidueDistances(iType,"r",""+residue2.getOneLetter()));
                rDists.add(new ResidueDistances(iType,""+residue.getOneLetter(),"r"));
                rDists.add(new ResidueDistances(iType,"r","r"));

                for (ResidueDistances distance : rDists) {

                    int index = distances.indexOf(distance);
                    if (index != -1) {
                        for (Map.Entry<String[], double[]> entry : distances.get(index).distances.entrySet()) {
                            double dist = entry.getValue()[0] / entry.getValue()[1];
                            if (dist < 5.25 && entry.getValue()[1] > 10) {
                                //add new NOE!!
                                String aString1 = entry.getKey()[0];
                                String aString2 = entry.getKey()[1];
                                Atom atom1 = residue.getAtom(aString1);
                                Atom atom2 = residue2.getAtom(aString2);
                                if (atom1==null || atom2 == null) {continue;}
                                if (!noeExists(noeSet, atom1, atom2)) {
                                    if (atom1.getResonance() == null) {
                                        atom1.setResonance((AtomResonance) Project.getActive().resFactory.build());
                                    }
                                    if (atom2.getResonance() == null) {
                                        atom2.setResonance((AtomResonance) Project.getActive().resFactory.build());
                                    }
                                    Noe noe = new Noe(null, atom1.getSpatialSet(), atom2.getSpatialSet(), 1.0, atom1.getResonance(), atom2.getResonance());
                                    double scaleConst = 100.0/ math.pow(2.0,-6);
                                    noe.setIntensity(math.pow(dist, -6)*scaleConst);
                                    noeSet.add(noe);
                                }
                            }
                        }
                    }
                }
            }
            start++;
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

    void cancel() {
        stage.close();
    }

    public Stage getStage() {
        return stage;
    }

}
