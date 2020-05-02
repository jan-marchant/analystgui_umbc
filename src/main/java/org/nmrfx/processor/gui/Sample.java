package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;

public class Sample {
    private String name;
    private String condition;
    private Molecule molecule=null;
    private IsotopeLabels labels=null;
    private Double concentration=null;

    public Sample (String name) {
        this.name=name;
    }

    public Double getAtomIntensity (Atom atom) {
        return 0.0;
    }

}
