package org.nmrfx.processor.gui;


import java.util.LinkedList;
import java.util.List;

public class Experiment {
    private String name;
    private int nDim;
    private List<ExpDim> expDims;
    private List<Connectivity> connectivities;

    public Experiment(String name,int nDim){
        this.name=name;
        this.nDim=nDim;
        this.expDims=new LinkedList<>();
        this.connectivities=new LinkedList<>();
    }

    public int getNDim () {
        return nDim;
    }


}
