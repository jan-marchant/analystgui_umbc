package org.nmrfx.processor.gui;

import org.nmrfx.processor.datasets.peaks.*;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.lang.reflect.Array;
import java.util.*;


public class AcqNode {

    private AcqTree acqTree;
    private int id;
    private Atom atom;
    private Set<AcqTree.Edge> edges=new HashSet<>();
    private ExpDim expDim;

    public AcqNode(AcqTree acqTree, int id) {
        this.id=id;
        this.acqTree=acqTree;
    }
    public AcqNode(AcqTree acqTree, int id, ExpDim expDim) {
        this.id=id;
        this.acqTree=acqTree;
        this.expDim=expDim;
    }

    @Override
    public String toString() {
        if (atom==null) {
            return "Node "+id;
        } else {
            return atom.getFullName();
        }
    }

    public ArrayList<Double> getPPMs() {
        ArrayList<Double> ppms=new ArrayList<>();
        ppms.add(atom.getPPM());
        for (ManagedList list : acqTree.getAcquisition().getManagedLists()){
            ppms.addAll(getPPMs(list));
        }
        for (PeakList list : PeakList.getLists()) {
            if (!acqTree.getAcquisition().getManagedLists().contains(list)) {
                ppms.addAll(getPPMs(list));
            }
        }
        while(ppms.remove(null));
        return ppms;
    }

    public ArrayList<Double> getPPMs(PeakList peakList) {
        ArrayList<Double> ppms=new ArrayList<>();
        for (int i = 0; i < peakList.getNDim(); i++) {
            for (Peak sPeak : peakList.peaks()) {
                if (((AtomResonance) sPeak.getPeakDim(i).getResonance()).getAtom()==getAtom()) {
                    ppms.add(new Double(sPeak.getPeakDim(i).getChemShift()));
                }
            }
        }
        return ppms;
    }

    public int getId() {
        return id;
    }

    public void setAtom(Atom atom) {
        this.atom = atom;
    }

    public Atom getAtom() {
        return atom;
    }

    public ExpDim getExpDim() {
        return expDim;
    }

    public ExpDim getNextExpDim(boolean forward) {
        return expDim.getNextExpDim(forward);
    }

    public Connectivity.TYPE getNextConType (boolean forward) {
        if (expDim!=null && expDim.getNextCon(forward)!=null) {
            return expDim.getNextCon(forward).type;
        } else {
            return null;
        }
    }

    public ExpDim getNextExpDim() {
        return getNextExpDim(true);
    }

    public void copyTo(AcqNode destination) {
        destination.atom=atom;
        destination.expDim=expDim;
    }

    public void addEdge(AcqTree.Edge edge) {
        edges.add(edge);
    }

    public Collection<AcqTree.Edge> getEdges(NoeSet noeSet, Noe noe) {
        return getEdges(true, true, noeSet,noe);
    }

    public Collection<AcqTree.Edge> getEdges(boolean forward, boolean backward, NoeSet noeSet) {
        return getEdges(forward, backward, noeSet, null);
    }

    public Collection<AcqTree.Edge> getEdges(boolean forward, boolean backward, NoeSet noeSet, Noe noe) {
        ArrayList<AcqTree.Edge> toReturn = new ArrayList<>();
        for (AcqTree.Edge edge : edges) {
            if (noeSet==null || edge.noeSet==null || noeSet==edge.noeSet) {
                if (noe==null || noe==edge.noe) {
                    if (forward && edge.node1 == this) {
                        toReturn.add(edge);
                    }
                    if (backward && edge.node2 == this) {
                        toReturn.add(edge);
                    }
                }
            }
        }
        return toReturn;
    }

    public Collection<AcqTree.Edge> getEdges(boolean forward, NoeSet noeSet) {
        return getEdges(forward,!forward,noeSet);
    }

    public Collection<AcqNode> getNodes(boolean forward,NoeSet noeSet) {
        return acqTree.getNodes(this,forward,noeSet);
    }

}
