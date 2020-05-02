package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.RNALabels;

import java.util.HashMap;

public class IsotopeLabels {
    private String labelScheme;
    private HashMap<Atom,Float> atomPercent;

    public IsotopeLabels () {
        this.labelScheme="";
    }

    public String getLabelScheme() {
        return labelScheme;
    }

    public void setLabelScheme(String labelScheme) {
        this.labelScheme = labelScheme;
    }

    public float getAtomPercent (Atom atom) {
        if (atom!=null) {
            float percent;
            try {
                percent = atomPercent.get(atom);
            } catch (Exception e) {
                percent = RNALabels.atomPercentLabelString(atom, getLabelScheme());
                if (percent>100) {
                    System.out.println("Check labeling string - "+atom.getName()+" is apparently "+percent+"% labeled");
                    percent=100;
                }
                atomPercent.put(atom, percent);
            }
            return percent;
        } else {
            System.out.println("Couldn't find atom");
            return 0;
        }
    }
}
