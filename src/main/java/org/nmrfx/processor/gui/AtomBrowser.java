/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import com.sun.org.apache.xpath.internal.operations.Bool;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.collections4.BidiMap;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.nmrfx.graphicsio.GraphicsContextInterface;
import org.nmrfx.graphicsio.GraphicsContextProxy;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.processor.datasets.peaks.*;
import org.nmrfx.processor.gui.annotations.AnnoLine;
import org.nmrfx.processor.gui.annotations.AnnoLineWithText;
import org.nmrfx.processor.gui.annotations.AnnoShape;
import org.nmrfx.processor.gui.controls.FractionCanvas;
import org.nmrfx.processor.gui.spectra.DatasetAttributes;
import org.nmrfx.processor.gui.spectra.NMRAxis;
import org.nmrfx.processor.gui.spectra.PeakListAttributes;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.utils.GUIUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AtomBrowser implements ControllerTool {

    ToolBar browserToolBar;
    FXMLController controller;
    Consumer closeAction;
    AtomSelector atomSelector1;
    AtomSelector atomSelector2;

    int centerDim = 0;
    int rangeDim = 1;
    CheckBox aspectCheckBox;
    Slider aspectSlider;
    Label aspectRatioValue;
    ObservableList<RangeItem> rangeItems = FXCollections.observableArrayList();
    ObservableList<FilterItem> filterList = FXCollections.observableArrayList();
    ComboBox<RangeItem> rangeSelector;
    List<DrawItem> drawItems = new ArrayList<>();
    ObservableList<LocateItem> locateItems = FXCollections.observableArrayList();
    ComboBox<Nuclei> otherNucleus;
    //TODO: Consider best default labels
    String xLabel="1H";
    String yLabel="H";
    Molecule mol;


    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    double delta = 0.1;
    boolean scheduled=false;
    private Atom currentAtom=null;

    public AtomBrowser(FXMLController controller, Consumer closeAction) {
        this.controller = controller;
        String title = "Browser 1";
        Integer suffix = 1;
        boolean seen;
        do {
            seen = false;
            for (FXMLController test : FXMLController.getControllers()) {
                if (test.getStage().getTitle().equalsIgnoreCase("Browser " + suffix)) {
                    suffix += 1;
                    seen = true;
                }
            }
        } while (seen);

        controller.getStage().setTitle("Browser "+suffix);
        this.closeAction = closeAction;
    }

    public ToolBar getToolBar() {
        return browserToolBar;
    }

    public void close() {
        closeAction.accept(this);
    }

    void initToolbar(ToolBar toolBar) {
        this.browserToolBar = toolBar;
        toolBar.setPrefWidth(900.0);

        String iconSize = "16px";
        String fontSize = "7pt";
        Button closeButton = GlyphsDude.createIconButton(FontAwesomeIcon.MINUS_CIRCLE, "Close", iconSize, fontSize, ContentDisplay.TOP);
        Button setupButton = GlyphsDude.createIconButton(FontAwesomeIcon.WRENCH, "Setup", iconSize, fontSize, ContentDisplay.TOP);
        closeButton.setOnAction(e -> close());
        setupButton.setOnAction(e -> new AtomBrowserSetup(this.controller));

        toolBar.getItems().add(closeButton);
        toolBar.getItems().add(setupButton);
        addFiller(toolBar);

        mol = Molecule.getActive();

        if (mol==null) {
            GUIUtils.warn("No active mol","You need to set up a molecule before browsing atoms");
            this.close();
            return;
        }

        atomSelector1 = new AtomSelector("Atom",false) {
            @Override
            public void selectAtom (Atom atom) {
                setAtom(atom);
            }

            @Override
            public void clearAction() {

            }
        };

        atomSelector2 = new AtomSelector("Locate",true) {
            @Override
            public void selectAtom(Atom atom) {
                if (atom!=null) {
                    LocateItem locateItem = new LocateItem(atom);
                    if (!locateItems.contains(locateItem)) {
                        locateItems.add(locateItem);
                        locateItem.add();
                    } else {
                        locateItems.get(locateItems.indexOf(locateItem)).remove();
                        locateItems.remove(locateItem);
                    }
                }
                Platform.runLater(() -> this.atomComboBox.setValue(null));
            }

            @Override
            public void clearAction() {
                clearLocates();
            }
        };

        atomSelector1.prefWidthProperty().bindBidirectional(atomSelector2.prefWidthProperty());
        toolBar.getItems().add(atomSelector1);
        toolBar.getItems().add(atomSelector2);

        otherNucleus = new ComboBox<>();
        ObservableList<Nuclei> nucleiList=FXCollections.observableArrayList();
        nucleiList.addAll(Nuclei.H1,Nuclei.C13,Nuclei.N15,Nuclei.F19,Nuclei.P31);
        otherNucleus.setItems(nucleiList);
        otherNucleus.setOnAction(e-> refresh());
        otherNucleus.setValue(Nuclei.H1);
        //toolBar.getItems().add(otherNucleus);
        //addFiller(toolBar);

        VBox vBox2=new VBox();
        toolBar.getItems().add(vBox2);
        addFiller(toolBar);

        rangeSelector = new ComboBox<>();

        rangeSelector.setItems(rangeItems);
        rangeSelector.setConverter(new StringConverter<RangeItem>() {
            @Override
            public String toString(RangeItem object) {
                return object.getName();
            }

            @Override
            public RangeItem fromString(String string) {
                return null;
            }
        });

        Button restore = new Button("Auto");
        restore.setOnAction(e -> setAtom(currentAtom));
        restore.setAlignment(Pos.BASELINE_LEFT);

        VBox vBox3 = new VBox(rangeSelector,restore);
        vBox3.setPrefWidth(100);
        toolBar.getItems().add(vBox3);
        restore.prefWidthProperty().bind(vBox3.widthProperty());
        rangeSelector.prefWidthProperty().bind(vBox3.widthProperty());
        rangeSelector.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (! isShowing) {
                updateRange();
            }
        });
        //rangeSelector.setOnAction(b -> updateRange());

        double initialAspect=1.0;
        aspectCheckBox = new CheckBox("Aspect Ratio");
        aspectSlider = new Slider();
        aspectRatioValue= new Label(String.valueOf(initialAspect));
        aspectCheckBox.selectedProperty().addListener(e -> updateAspectRatio());
        aspectSlider.setMin(0.1);
        aspectSlider.setMax(3.0);
        aspectSlider.setValue(initialAspect);
        aspectSlider.setBlockIncrement(0.01);
        //aspectSlider.setOnMousePressed(e -> shiftState = e.isShiftDown());
        aspectSlider.valueProperty().addListener(e -> updateAspectRatio());

        aspectSlider.setPrefWidth(100);
        aspectRatioValue.setMinWidth(35);
        aspectRatioValue.setMaxWidth(35);
        HBox hBox = new HBox(aspectSlider,aspectRatioValue);
        VBox vBox4 = new VBox(aspectCheckBox,hBox);

        addFiller(toolBar);
        toolBar.getItems().add(vBox4);

        addRangeControl("Aro", 6.5, 8.2);
        rangeSelector.setValue(addRangeControl("H1'", 5.1, 6.2));
        addRangeControl("H2'", 3.8, 5.1);
        updateRange();


    }

    void updateAspectRatio () {
        double aspectRatio = aspectSlider.getValue();
        aspectRatioValue.setText(String.format("%.2f", aspectRatio));

        if (aspectCheckBox.isSelected()) {
            for (PolyChart applyChart : controller.charts) {
                applyChart.chartProps.setAspect(aspectCheckBox.isSelected());
                applyChart.chartProps.setAspectRatio(aspectRatio);
                applyChart.refresh();
            }
        }
    }

    abstract class AtomSelector extends VBox {
        ComboBox<Atom> atomComboBox;
        Button clear;
        ObservableList<Atom> atomList = FXCollections.observableArrayList();
        TextField atomField;
        AutoCompletionBinding<Atom> acb;
        String filterString;
        MolFilter molFilter;

        public AtomSelector (String prompt, boolean includeClear) {
            atomComboBox = new ComboBox<>();
            atomComboBox.setPromptText(prompt);

            atomComboBox.setItems(atomList);
            atomComboBox.setEditable(false);

            atomComboBox.setConverter(new StringConverter<Atom>() {
                @Override
                public String toString(Atom atom) {
                    return atom.getShortName();
                }

                @Override
                public Atom fromString(String string) {
                    return null;
                }
            });

            clear = new Button("Clear");

            clear.setOnAction(e -> clearAction());

            HBox hBox = new HBox();

            hBox.getChildren().add(atomComboBox);

            if (includeClear) {
                hBox.getChildren().add(clear);
            }

            atomField = new TextField();
            atomField.setPromptText("Search");
            atomField.prefWidthProperty().bind(hBox.widthProperty());
            acb = TextFields.bindAutoCompletion(atomField, atomComboBox.getItems());
            acb.setOnAutoCompleted(event -> {
                Atom atom = event.getCompletion();
                atomField.clear();
                atomComboBox.setValue(atom);
                selectAtom(atom);
            });
            acb.setVisibleRowCount(10);
            setFilterString("*.H*");

            atomField.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    Atom atom = mol.getAtom(atomField.getText());
                    if (atom!=null) {
                        atomComboBox.setValue(atom);
                        selectAtom(atom);
                    }
                    atomField.clear();
                } else if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                    atomField.clear();
                }
            });

            atomComboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                if (! isShowing) {
                    selectAtom(atomComboBox.getValue());
                    atomField.clear();
                }
            });

            this.getChildren().addAll(hBox,atomField);
        }

        public abstract void selectAtom (Atom atom);
        public abstract void clearAction();

        public void setFilterString(String string) {
            if (!string.equalsIgnoreCase(filterString)) {
                filterString = string;
                molFilter = new MolFilter(filterString);
                Molecule molecule = Molecule.getActive();
                atomList.clear();
                if (molecule != null) {
                    try {
                        Molecule.selectAtomsForTable(molFilter, atomList);
                    } catch (InvalidMoleculeException ex) {
                    }
                }
                atomComboBox.setItems(atomList);
                acb.dispose();
                acb = TextFields.bindAutoCompletion(atomField, atomComboBox.getItems());
                acb.setOnAutoCompleted(event -> {
                    Atom atom = event.getCompletion();
                    atomField.clear();
                    atomComboBox.setValue(atom);
                    selectAtom(atom);
                });
                acb.setVisibleRowCount(10);
                if (atomList.contains(currentAtom)) {
                    atomComboBox.setValue(currentAtom);
                    selectAtom(currentAtom);
                } else {
                    currentAtom = null;
                }
            }
        }
    }

    public void addFiller(ToolBar toolBar) {
        Pane filler = new Pane();
        HBox.setHgrow(filler, Priority.ALWAYS);
        filler.setMinWidth(25);
        filler.setMaxWidth(50);
        toolBar.getItems().add(filler);
    }

    public void addDrawItems(Atom atom,Project project) {
        if (atom.getResonance()!=null) {
            for (PeakDim peakDim : atom.getResonance().getPeakDims()) {
                if (peakDim.getSpectralDimObj().getDimName().equals(xLabel)) {
                    for (PeakDim otherDim : peakDim.getPeak().getPeakDims()) {
                        if (otherDim == peakDim) {
                            continue;
                        }
                        if (otherDim.getSpectralDimObj().getDimName().equals(yLabel)) {
                            boolean seen = false;
                            for (DrawItem item : drawItems) {
                                if (item.addIfMatch(peakDim, otherDim)) {
                                    seen = true;
                                }
                            }
                            if (!seen) {
                                drawItems.add(new DrawItem(peakDim, otherDim, project));
                            }
                        }
                    }
                }
            }
        }
    }

    public void setAtom(Atom atom) {

        drawItems.clear();
        if (atom!=null) {
            currentAtom = atom;
            addDrawItems(atom, Project.getActive());
            for (UmbcProject subProject : UmbcProject.getActive().subProjectList) {
                BidiMap<Entity, Entity> map = UmbcProject.getActive().entityMap.get(subProject);
                if (map.containsKey(atom.getEntity())) {
                    Entity otherEntity = map.get(atom.getEntity());
                    for (Atom otherAtom : otherEntity.getAtoms()) {
                        if (otherAtom.getName().equals(atom.getName())) {
                            addDrawItems(otherAtom, subProject);
                        }
                    }
                }
            }
        }
        refresh();

    }

    public void drawLocateItems() {
        for (LocateItem locateItem : locateItems) {
            locateItem.update();
        }
    }

    private void clearLocates() {
        for (LocateItem locateItem : locateItems) {
            locateItem.remove();
        }
        locateItems.clear();
    }

    public void setxLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    public void setyLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    public String getxLabel() {
        return xLabel;
    }

    public String getyLabel() {
        return yLabel;
    }

    public void refresh() {
        controller.setBorderState(true);
        controller.setNCharts(0);
        int i = 1;
        Double yMin = Double.MAX_VALUE;
        Double yMax = Double.MIN_VALUE;
        //just filter on active datasets, peakLists, dims, experiment types, subProjects etc. here?
        /*for (DrawItem item : drawItems.values()) {
            if (item.getOtherDims().containsKey(otherNucleus.getValue())) {
                for (DrawItem.OtherDim otherDim : item.getOtherDims().get(otherNucleus.getValue())) {
                    if (otherDim.min < yMin) {
                        yMin = otherDim.min;
                    }
                    if (otherDim.max > yMax) {
                        yMax = otherDim.max;
                    }
                }
            }
        }

         */

        PolyChart previousChart = null;
        for (DrawItem item : drawItems) {
            AtomicReference<Double> ppm = new AtomicReference<>(item.getShift());
            if (ppm.get() != null) {
                if (controller.charts.size() < i) {
                    controller.addChart();
                }
                PolyChart chart = controller.charts.get(i - 1);
                chart.setActiveChart();
                chart.chartProps.setTopBorderSize(40);
                chart.topBorder=40;
                List<Dataset> datasetList = new ArrayList<>();
                datasetList.add(item.dataset);
                List<PeakList> peakListList = new ArrayList<>();
                peakListList.add(item.peakList);
                                chart.updateDatasetObjects(datasetList);
                chart.updatePeakListObjects(peakListList);
                // fixme
//                chart.getChildrenUnmodifiable().stream().filter((node) -> (node instanceof Label)).forEachOrdered((node) -> {
//                    node.setOnMouseClicked(e -> System.out.println("Clicked node " + item.dataset.getName()));
//                });
                DatasetAttributes datasetAttr = chart.datasetAttributesList.get(0);
                datasetAttr.setDim(item.centerDimNo, centerDim);
                datasetAttr.setDim(item.rangeDimNo, rangeDim);

                GraphicsContext gCC = chart.canvas.getGraphicsContext2D();
                GraphicsContextInterface gC = new GraphicsContextProxy(gCC);
                item.dataset.setTitle(item.dataset.getName());

                chart.chartProps.setTitles(true);

                if (item.getMinRange() < yMin) {
                    yMin = item.getMinRange();
                }
                if (item.getMaxRange() > yMax) {
                    yMax = item.getMaxRange();
                }

                chart.getAxis(centerDim).setLowerBound(ppm.get() - delta);
                chart.getAxis(centerDim).setUpperBound(ppm.get() + delta);
                chart.getAxis(rangeDim).setLowerBound(yMin - delta);
                chart.getAxis(rangeDim).setUpperBound(yMax + delta);
                chart.getAxis(rangeDim).lowerBoundProperty().addListener(e -> {
                    if (!scheduled) {
                        service.schedule(() -> {
                            Platform.runLater(() -> {
                                updateAllBounds();
                                scheduled = false;
                            });
                        }, 1, TimeUnit.MILLISECONDS);

                        scheduled = true;
                    }
                });
                chart.getAxis(rangeDim).upperBoundProperty().addListener(e -> {
                    if (!scheduled) {
                        service.schedule(() -> {
                            Platform.runLater(() -> {
                                updateAllBounds();
                                scheduled = false;
                            });
                        }, 1, TimeUnit.MILLISECONDS);

                        scheduled = true;
                    }
                });


                for (int n = 2; n < item.dataset.getNDim(); n++) {
                    int dataDim = datasetAttr.getDim(n);
                    chart.getAxis(n).setLowerBound(item.getMinShift(dataDim));
                    chart.getAxis(n).setUpperBound(item.getMaxShift(dataDim));
                    int finalN = n;
                    chart.getAxis(n).lowerBoundProperty().addListener(e -> {
                        for (PeakDim peakDim : item.dims.get(dataDim)) {
                            peakDim.setChemShift((float) chart.getAxis(finalN).getLowerBound());
                        }
                        if (!scheduled) {
                            service.schedule(() -> {
                                Platform.runLater(() -> {
                                    updateAllBounds();
                                    scheduled = false;
                                });
                            }, 1, TimeUnit.MILLISECONDS);

                            scheduled = true;
                        }
                    });
                    chart.getAxis(n).upperBoundProperty().addListener(e -> {
                        for (PeakDim peakDim : item.dims.get(dataDim)) {
                            peakDim.setChemShift((float) chart.getAxis(finalN).getUpperBound());
                        }
                        if (!scheduled) {
                            service.schedule(() -> {
                                Platform.runLater(() -> {
                                    updateAllBounds();
                                    scheduled = false;
                                });
                            }, 1, TimeUnit.MILLISECONDS);

                            scheduled = true;
                        }
                    });

                }

                if (previousChart != null) {
                    chart.getAxis(rangeDim).lowerBoundProperty().bindBidirectional(previousChart.getAxis(rangeDim).lowerBoundProperty());
                    chart.getAxis(rangeDim).upperBoundProperty().bindBidirectional(previousChart.getAxis(rangeDim).upperBoundProperty());
                }

                item.peakList.registerListener(e -> {
                    boolean dirty=false;
                    Double newPpm = item.getShift();
                    if (newPpm != null && newPpm != ppm.get()) {
                        double currentDelta=chart.getAxis(centerDim).getRange()/2;
                        chart.getAxis(centerDim).setLowerBound(newPpm - currentDelta);
                        chart.getAxis(centerDim).setUpperBound(newPpm + currentDelta);
                        ppm.set(newPpm);
                        dirty = true;
                    }
                    /*
                    for (int n = 2; n < item.dataset.getNDim(); n++) {
                        int dataDim = datasetAttr.getDim(n);
                        Double newMin = item.getMinShift(dataDim);
                        Double newMax = item.getMaxShift(dataDim);
                        if (chart.getAxis(n).getLowerBound()!=newMin) {
                            chart.getAxis(n).setLowerBound(item.getMinShift(dataDim));
                            dirty=true;
                        }
                        if (chart.getAxis(n).getUpperBound()!=newMax) {
                            chart.getAxis(n).setUpperBound(item.getMaxShift(dataDim));
                            dirty=true;
                        }
                    }
                     */
                    if (dirty && !scheduled) {
                        service.schedule(() -> {
                            Platform.runLater(() -> {
                                //updateChartBounds(chart);
                                updateAllBounds();
                                drawLocateItems();
                                scheduled = false;
                            });
                        }, 1, TimeUnit.MILLISECONDS);
                        scheduled = true;
                    }
                });
                //chart.getAxis(rangeDim).lowerBoundProperty().addListener(e->updateBounds());
                //chart.getAxis(rangeDim).upperBoundProperty().addListener(e->updateBounds());

                previousChart = chart;
                chart.refresh();
                i++;
            }
        }
        controller.arrange(FractionCanvas.ORIENTATION.HORIZONTAL);
        controller.showPeakSlider(false);
        drawLocateItems();
        updateAspectRatio();
    }

    public class FilterItem {

        Object object;
        public BooleanProperty allow = new SimpleBooleanProperty();

        public StringProperty name = new SimpleStringProperty();
        public StringProperty type = new SimpleStringProperty();

        FilterItem(Object object, boolean allow) {
            this.object=object;
            setAllow(allow);
            setName(getObjectName());
            setType(getObjectClassName());
        }

        public String getName() {
            return name.get();
        }

        public boolean isAllow() {
            return allow.get();
        }

        public BooleanProperty allowProperty() {
            return allow;
        }

        public void setAllow(boolean allow) {
            this.allow.set(allow);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getType() {
            return type.get();
        }

        public StringProperty typeProperty() {
            return type;
        }

        public void setType(String type) {
            this.type.set(type);
        }

        public String getObjectName () {
            return object.toString();
        }

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public String getObjectClassName () {
            return object.getClass().getSimpleName();
        }

        public Class getObjectClass() {
            return object.getClass();
        }

        public void remove (AtomBrowser browser) {
            browser.filterList.remove(this);
        }

        public boolean matches (DrawItem drawItem) {
            if (object instanceof Dataset) {
                if (drawItem.dataset==getObject()) {
                        return true;
                }
            } else if (object instanceof PeakList) {
                if (drawItem.peakList==getObject()) {
                    return true;
                }
            }  else if (object instanceof Project) {
                if (drawItem.project==getObject()) {
                    return true;
                }
            } else if (object instanceof Acquisition) {
                if (((ManagedList) drawItem.peakList).getAcquisition() == object) {
                    return true;
                }
            } else if (object instanceof Experiment) {
                if (((ManagedList) drawItem.peakList).getAcquisition().getExperiment() == object) {
                    return true;
                }
            } else if (object instanceof Sample) {
                if (((ManagedList) drawItem.peakList).getAcquisition().getSample() == object) {
                    return true;
                }
            } else if (object instanceof Condition) {
                if (((ManagedList) drawItem.peakList).getAcquisition().getCondition() == object) {
                    return true;
                }
            }
            return false;
        }

    }

    public void addFilterItem (Object object, boolean allow) {
        filterList.add(new FilterItem(object,allow));
    }

    public class LocateItem {
        Atom atom;
        HashMap<PolyChart,List<AnnoLine>> annotations;

        LocateItem(Atom atom) {
            this.atom=atom;
            annotations = new HashMap<>();
        }
        @Override
        public boolean equals (Object o) {
            if (this.atom == ((LocateItem) o).atom) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        public void remove () {
            annotations.forEach((chart,annoList) -> {
                for (AnnoLine anno : annoList) {
                    chart.removeAnnotation(anno);
                    GraphicsContextProxy gcPeaks = new GraphicsContextProxy(chart.peakCanvas.getGraphicsContext2D());
                    chart.drawPeakLists(true, gcPeaks);
                    chart.drawAnnotations(gcPeaks);
                }
            });
            annotations.clear();
        }

        public void update () {
            remove();
            add();
        }

        public void add () {
            for (int i = 0; i < controller.charts.size(); i++) {
                PolyChart chart = controller.charts.get(i);
                add(chart);
            }
        }

        public void add (PolyChart chart) {
            if (atom.getResonance() != null) {
                for (PeakDim peakDim : atom.getResonance().getPeakDims()) {
                    for (PeakListAttributes peakAttr : chart.peakListAttributesList) {
                        PeakList peakList = peakAttr.getPeakList();
                        if (peakDim.getPeakList() == peakList) {
                            AnnoLineWithText annoLine;
                            if (peakDim.getSpectralDim() == centerDim) {
                                annoLine = new AnnoLineWithText(atom.getShortName(), peakDim.getChemShiftValue(), 0.99 , peakDim.getChemShiftValue(), 0, peakDim.getChemShiftValue(), 1, CanvasAnnotation.POSTYPE.WORLD, CanvasAnnotation.POSTYPE.FRACTION);
                            } else if (peakDim.getSpectralDim() == rangeDim) {
                                annoLine = new AnnoLineWithText(atom.getShortName(), 0.01, peakDim.getChemShiftValue() ,0, peakDim.getChemShiftValue(), 1, peakDim.getChemShiftValue(), CanvasAnnotation.POSTYPE.FRACTION, CanvasAnnotation.POSTYPE.WORLD);
                            } else {
                                continue;
                            }
                            if (peakDim.isFrozen()) {
                                annoLine.setStroke(Color.RED);
                            } else {
                                annoLine.setStroke(Color.GRAY);
                            }
                            annotations.putIfAbsent(chart, new ArrayList<>());
                            if (!annotations.get(chart).contains(annoLine)) {
                                annotations.get(chart).add(annoLine);
                                chart.addAnnotation(annoLine);
                                GraphicsContextProxy gcPeaks = new GraphicsContextProxy(chart.peakCanvas.getGraphicsContext2D());
                                chart.drawPeakLists(true, gcPeaks);
                                chart.drawAnnotations(gcPeaks);
                            }
                        }
                    }
                }
            }
        }
    }

    public class RangeItem {

        StringProperty name = new SimpleStringProperty();
        DoubleProperty min = new SimpleDoubleProperty();
        DoubleProperty max = new SimpleDoubleProperty();
        //double min;
        //double max;
        //String name;

        RangeItem(String name, double min, double max) {
            setName(name);
            setMin(min);
            setMax(max);
        }

        public void remove (AtomBrowser browser) {
            browser.rangeItems.remove(this);
        }

        @Override
        public String toString() {
            return getName();
        }

        public String getName() {
            return name.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public double getMin() {
            return min.get();
        }

        public DoubleProperty minProperty() {
            return min;
        }

        public void setMin(double min) {
            this.min.set(min);
        }

        public double getMax() {
            return max.get();
        }

        public DoubleProperty maxProperty() {
            return max;
        }

        public void setMax(double max) {
            this.max.set(max);
        }
    }

    public RangeItem addRangeControl(String name, double min, double max) {
        RangeItem rangeItem = new RangeItem(name, min, max);
        rangeItems.add(rangeItem);
        return rangeItem;
    }

    public ObservableList<RangeItem> getRangeItems() {
        return rangeItems;
    }

    public void updateChartBounds(PolyChart chart) {
        chart.refresh();
    }
    public void updateAllBounds() {
        controller.charts.stream().forEach(chart -> {
                updateChartBounds(chart);
        });
    }

    public void updateRange() {
        RangeItem rangeItem = rangeSelector.getValue();
        if (rangeItem != null) {
            double min = rangeItem.getMin();
            double max = rangeItem.getMax();

            controller.charts.stream().forEach(chart -> {
                try {
                    chart.getAxis(rangeDim).setLowerBound(min);
                    chart.getAxis(rangeDim).setUpperBound(max);
                } catch (NumberFormatException nfE) {

                }
            });
        }
    }

    public class DrawItem implements Comparator<DrawItem> {
        final SpectralDim centerSpectralDim;
        final SpectralDim rangeSpectralDim;
        final Nuclei centerNucleus;
        final Nuclei rangeNucleus;
        final Dataset dataset;
        final PeakList peakList;
        final int centerDimNo;
        final int rangeDimNo;
        HashMap<Integer,List<PeakDim>> dims = new HashMap<>();
        List<Peak> peaks=new ArrayList<>();
        Project project;

        DrawItem(PeakDim centerDim,PeakDim rangeDim,Project project) {
            this.project=project;
            Peak peak = centerDim.getPeak();
            peakList = peak.getPeakList();
            dataset = project.getDataset(peakList.getDatasetName());
            centerDimNo=centerDim.getSpectralDimObj().getDataDim();
            rangeDimNo=rangeDim.getSpectralDimObj().getDataDim();
            centerSpectralDim =peakList.getSpectralDim(centerDimNo);
            rangeSpectralDim=peakList.getSpectralDim(rangeDimNo);
            centerNucleus=Nuclei.findNuclei(centerSpectralDim.getNucleus());
            rangeNucleus=Nuclei.findNuclei(peakList.getSpectralDim(rangeDimNo).getNucleus());
            this.add(peak);
        }

        public void add(Peak peak) {
            this.peaks.add(peak);
            for (PeakDim peakDim : peak.getPeakDims()) {
                dims.putIfAbsent(peakDim.getSpectralDimObj().getDataDim(),new ArrayList<>());
                List<PeakDim> list=dims.get(peakDim.getSpectralDimObj().getDataDim());
                if (!list.contains(peakDim)) {
                    list.add(peakDim);
                }
            }
        }

        public boolean addIfMatch(PeakDim centerDim, PeakDim rangeDim) {
            if (centerDim.getSpectralDimObj()!=centerSpectralDim || rangeDim.getSpectralDimObj()!=rangeSpectralDim) {
                return false;
            }
            Peak peak=centerDim.getPeak();
            for (PeakDim peakDim : peak.getPeakDims()) {
                int dim=peakDim.getSpectralDimObj().getDataDim();
                if (dim==centerDimNo || dim==rangeDimNo) {
                    continue;
                }
                if (peakDim.getResonance()!=dims.get(dim).get(0).getResonance()) {
                    return false;
                }
            }
            add(peak);
            return true;
        }

        @Override
        public int compare(DrawItem o1, DrawItem o2) {
            return o1.getShift().compareTo(o2.getShift());
        }

        Double getMinShift(List<PeakDim> peakDims) {
            return peakDims.stream().mapToDouble(val -> Double.parseDouble(val.getChemShift().toString())).min().orElse(Double.MIN_VALUE);
        }
        Double getMaxShift(List<PeakDim> peakDims) {
            return peakDims.stream().mapToDouble(val -> Double.parseDouble(val.getChemShift().toString())).max().orElse(Double.MAX_VALUE);
        }
        Double getShift() {
            return getAverageShift(centerDimNo);
        }
        Double getMinRange() {
            return getMinRange(dims.get(rangeDimNo));
        }
        Double getMinRange(List<PeakDim> peakDims) {
            return peakDims.stream().mapToDouble(val -> Double.parseDouble(((Float) (val.getChemShift()-val.getLineWidth()/2)).toString())).min().orElse(Double.MIN_VALUE);
        }

        Double getMaxRange(List<PeakDim> peakDims) {
            return peakDims.stream().mapToDouble(val -> Double.parseDouble(((Float) (val.getChemShift()+val.getLineWidth()/2)).toString())).max().orElse(Double.MAX_VALUE);
        }

        Double getMaxRange() {
            return getMaxRange(dims.get(rangeDimNo));
        }
        Double getAverageShift(int n) {
            return dims.get(n).stream().mapToDouble(val -> Double.parseDouble(val.getChemShift().toString())).average().orElse(0.0);
        }
        Double getMinShift(int n) {
            return getMinShift(dims.get(n));
        }
        Double getMaxShift(int n) {
            return getMaxShift(dims.get(n));
        }
        List<Double> getShifts (List<PeakDim> peakDims) {
            List<Double> shifts = new ArrayList<>();
            for (PeakDim peakDim :peakDims) {
                if (!shifts.contains(Double.parseDouble(peakDim.getChemShift().toString()))) {
                    shifts.add(Double.parseDouble(peakDim.getChemShift().toString()));
                }
            }
            return shifts;
        }

    }

}
