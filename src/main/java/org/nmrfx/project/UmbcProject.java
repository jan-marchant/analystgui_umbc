package org.nmrfx.project;

import org.nmrfx.processor.gui.MoleculeCouplingList;

public class UmbcProject extends GUIStructureProject {
    private MoleculeCouplingList moleculeCouplingList;

    public UmbcProject(String name) {
        super(name);
        moleculeCouplingList=new MoleculeCouplingList(this);
    }

    public MoleculeCouplingList getMoleculeCouplingList() {
        return moleculeCouplingList;
    }

}
