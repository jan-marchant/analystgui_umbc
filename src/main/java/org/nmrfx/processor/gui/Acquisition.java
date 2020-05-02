package org.nmrfx.processor.gui;

import org.nmrfx.processor.datasets.Dataset;

public class Acquisition {
    private Dataset dataset;
    private Experiment experiment;
    private Sample sample;
    private ManagedList managedList;
    private Double sensitivity;

    public Acquisition(Dataset dataset) {
        this.dataset=dataset;
        this.sensitivity=getDatasetSensitivity();
    }

    private double getDatasetSensitivity () {
        return 0.0;
    }

    public ManagedList makeManagedList () {
        if (experiment==null) {
            System.out.println("Need to set up experiment first");
            return null;
        }
        for (int i=0;i<experiment.getNDim();i++) {

        }
        return managedList;
    }
}
