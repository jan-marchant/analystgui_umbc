package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.Atom;

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
    private List<Atom> activeAtoms;
    private String pattern;
    private Boolean observed;
    private ExpDim nextExpDim;
    private Connectivity nextCon;

    public ExpDim(Boolean observed) {
        this.observed=observed;
    }

    public Boolean isObserved() {
        return observed;
    }

    public ExpDim getNextExpDim() {
        return nextExpDim;
    }

    public Connectivity getNextCon() {
        return nextCon;
    }

    public void setNext(Connectivity nextCon,ExpDim nextExpDim) {
        this.nextCon = nextCon;
        this.nextExpDim=nextExpDim;
    }
}
