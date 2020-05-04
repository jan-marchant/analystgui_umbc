package org.nmrfx.processor.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.utils.GUIUtils;

import java.util.ArrayList;
import java.util.List;

public class Acquisition {

    private UmbcProject project;
    private ObservableList<ManagedList> managedListsList = FXCollections.observableArrayList();
    private ObjectProperty<Dataset> dataset = new SimpleObjectProperty<>();
    private ObjectProperty<Experiment> experiment = new SimpleObjectProperty<>();
    private ObjectProperty<Sample> sample = new SimpleObjectProperty<>();
    private ListProperty<ManagedList> managedLists = new SimpleListProperty<>(managedListsList);
    private Double sensitivity;

    public Acquisition() {
        project=UmbcProject.getActive();
        project.acquisitionTable.add(this);
    }

    public void addNewManagedList() {
        if (getDataset()==null || getSample()==null || getExperiment()==null) {
            GUIUtils.warn("Cannot add list","You must define all acquisition parameters before adding any lists.");
        }
        ManagedListSetup managedListSetup = new ManagedListSetup(this);

    }

    public void deleteManagedList(ManagedList managedList,boolean prompt) {
        boolean delete=true;
        if (prompt) {
            delete = GUIUtils.affirm("Are you sure? This cannot be undone.");
        }
        if (delete) {
            getManagedLists().remove(managedList);
            managedList.remove();
        }
    }

    public void remove(boolean prompt) {
        boolean delete=true;
        if (prompt) {
            delete = GUIUtils.affirm("Are you sure you want to delete? This cannot be undone.");
        }
        if (delete) {
            project.acquisitionTable.remove(this);
        }
    }

    private double getDatasetSensitivity () {
        return 0.0;
    }

    public Dataset getDataset() {
        return dataset.get();
    }

    public ObjectProperty<Dataset> datasetProperty() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset.set(dataset);
    }

    public Experiment getExperiment() {
        return experiment.get();
    }

    public ObjectProperty<Experiment> experimentProperty() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment.set(experiment);
    }

    public Sample getSample() {
        return sample.get();
    }

    public ObjectProperty<Sample> sampleProperty() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample.set(sample);
    }

    public ObservableList<ManagedList> getManagedLists() {
        return managedLists.get();
    }

    public ListProperty<ManagedList> managedListsProperty() {
        return managedLists;
    }

    public void setManagedLists(ObservableList<ManagedList> list) {
        this.managedLists.set(list);
    }

    public Double getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(Double sensitivity) {
        this.sensitivity = sensitivity;
    }
}
