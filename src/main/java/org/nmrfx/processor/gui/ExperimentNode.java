package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.util.ArrayList;
import java.util.HashMap;

public class ExperimentNode {

    private class Edge {
        public Edge(boolean forward, double weight, NoeSet noeSet) {

        }
    }
    private int id;
    private HashMap<ExperimentNode,Double> forwardWeightedEdges = new HashMap<>();
    private HashMap<ExperimentNode,Double> backwardWeightedEdges = new HashMap<>();
    private Atom atom;
    private ExpDim expDim;

    public ExperimentNode(int id) {
        this.id=id;
    }
    public ExperimentNode(int id,ExpDim expDim) {
        this.id=id;
        this.expDim=expDim;
    }

    public int getId() {
        return id;
    }

    public HashMap<ExperimentNode, Double> getForwardWeightedEdges() {
        return forwardWeightedEdges;
    }

    public HashMap<ExperimentNode, Double> getBackwardWeightedEdges() {
        return backwardWeightedEdges;
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

    public void copyTo(ExperimentNode destination) {
        destination.atom=atom;
        destination.expDim=expDim;
    }

}
