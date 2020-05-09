package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Polymer;
import org.nmrfx.structure.chemistry.RNALabels;

import java.util.HashMap;

public class IsotopeLabels {
    private String labelScheme;

    public IsotopeLabels (String labelScheme) {
        this.labelScheme=labelScheme;
    }

    public String getLabelScheme() {
        return labelScheme;
    }

    public void setLabelScheme(String labelScheme) {
        this.labelScheme = labelScheme;
    }

    public double getFraction (Atom atom) {
        //TODO: implement label scheme setup for arbitrary molecules
        if (atom!=null) {
            if (atom.getTopEntity() instanceof Polymer) {
                if (((Polymer) atom.getTopEntity()).isRNA()) {
                    return RNALabels.atomPercentLabelString(atom, getLabelScheme())/100;
                }
            }
            return 1.0;
        } else {
            System.out.println("Couldn't find atom");
            return 0;
        }
    }
}
