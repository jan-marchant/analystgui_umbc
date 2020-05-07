package org.nmrfx.processor.gui;

import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;

import java.util.ArrayList;
import java.util.List;

public class ExpDim {
    /**
     * An experiment dimension. Includes observed and not observed dimensions.
     * For example an H(C)CH-TOCSY has 4 dimensions with the following patterns:
     * 1: i.Hali 2: i.Cali 3: i.Cali 4: i.Hali
     * Associated connectivities:
     * 1-2: J(1) 2-3: TOCSY(3) 3-4: J(1)
     * And 1,3 and 4 observable
     */

    private String pattern;
    private Boolean observed;
    private ExpDim nextExpDim;
    private ExpDim previousExpDim;
    private Connectivity nextCon;
    private Connectivity previousCon;
    private Nuclei nucleus;

    public ExpDim(Nuclei nucleus,Boolean observed) {
        this.observed=observed;
        this.nucleus=nucleus;
    }
    public ExpDim(Boolean observed) {
        this.observed=observed;
        this.nucleus=Nuclei.H1;
    }

    public Boolean isObserved() {
        return observed;
    }

    public ExpDim getNextExpDim(boolean forward) {
        if (forward) {
            return nextExpDim;
        } else {
            return previousExpDim;
        }
    }

    public Connectivity getNextCon(boolean forward) {
        if (forward) {
            return nextCon;
        } else {
            return previousCon;
        }
    }

    public ExpDim getNextExpDim() {
        return getNextExpDim(true);
    }

    public Connectivity getNextCon() {
        return getNextCon(true);
    }


    public void setNext(Connectivity nextCon,ExpDim nextExpDim) {
        this.nextCon = nextCon;
        nextExpDim.previousCon=nextCon;
        this.nextExpDim=nextExpDim;
        nextExpDim.previousExpDim=this;
    }

    public ArrayList<Atom> getActiveAtoms(Molecule mol) {
        //cache this as a HashMap of molecule vs. activeAtoms? If not containsKey...
        ArrayList<Atom> activeAtoms=new ArrayList<>();
        return activeAtoms;
    }

    public Nuclei getNucleus() {
        return nucleus;
    }

    public List<Atom> getConnected(Atom atom) {
        //also worth caching per molecule. Frequently lots of the same experiment type added at once. Not worth persisting across saves though
        List<Atom> connected = new ArrayList<>();
        return connected;
    }
}
