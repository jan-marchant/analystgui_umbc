package org.nmrfx.processor.gui;

import org.nmrfx.processor.operations.Exp;
import org.nmrfx.structure.chemistry.Atom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class ExperimentTree {

    public ArrayList<ExperimentNode> nodes = null;
    private HashMap<ExpDim, HashMap<Atom, ExperimentNode>> dimAtomNodeMap = new HashMap<>();
    private ExperimentNode firstNode;
    private ExperimentNode lastNode;

    public ExperimentTree() {
        firstNode=addNode();
        lastNode=addNode();
    }

    public ExperimentNode addNode() {
        int id = nodes.size();
        ExperimentNode node = new ExperimentNode(id);
        nodes.add(node);
        return node;
    }

    public ExperimentNode addNode(Atom atom,ExpDim expDim) {
        ExperimentNode node;
        if (!dimAtomNodeMap.containsKey(expDim)) {
            HashMap<Atom, ExperimentNode> nodeMap=new HashMap<>();
            dimAtomNodeMap.put(expDim,nodeMap);
        }
        if (!dimAtomNodeMap.get(expDim).containsKey(atom)) {
            int id = nodes.size();
            node = new ExperimentNode(id,expDim);
            dimAtomNodeMap.get(expDim).put(atom,node);
            nodes.add(node);
        } else {
            node=dimAtomNodeMap.get(expDim).get(atom);
        }
        return node;
    }

    public ExperimentTree copy() {
        ExperimentTree copy=new ExperimentTree();
        copyNodes(copy);
        return copy;
    }

    public void copyNodes (ExperimentTree destination) {
        HashMap<ExperimentNode, ExperimentNode> nodeMap = new HashMap<>();
        for (ExperimentNode node : this.getNodes()) {
            ExperimentNode copyNode = new ExperimentNode(node.getId());
            node.copyTo(copyNode);
            destination.nodes.add(copyNode);
            destination.dimAtomNodeMap.putIfAbsent(node.getExpDim(),new HashMap<>());
            destination.dimAtomNodeMap.get(node.getExpDim()).put(node.getAtom(),copyNode);
            nodeMap.put(node, copyNode);
        }
        copyNodeConnections(destination,nodeMap);
    }

    public void copyNodeConnections(ExperimentTree destination, HashMap<ExperimentNode, ExperimentNode> nodeMap) {
        destination.firstNode=nodeMap.get(firstNode);
        destination.lastNode=nodeMap.get(lastNode);
        for (ExperimentNode node : this.getNodes()) {
            ExperimentNode copyNode=nodeMap.get(node);
            for (ExperimentNode connectedNode : node.getForwardWeightedEdges().keySet()) {
                copyNode.getForwardWeightedEdges().put(nodeMap.get(connectedNode), node.getForwardWeightedEdges().get(connectedNode));
            }
            for (ExperimentNode connectedNode : node.getBackwardWeightedEdges().keySet()) {
                copyNode.getBackwardWeightedEdges().put(nodeMap.get(connectedNode), node.getBackwardWeightedEdges().get(connectedNode));
            }
        }
    }

    public Collection<ExperimentNode> getNodes() {
        return this.nodes;
    }

    public Collection<ExperimentNode> getNodes(ExpDim expDim) {
        return dimAtomNodeMap.get(expDim).values();
    }

    public ExperimentNode getNode(ExpDim expDim,Atom atom) {
        //fixme: use Optional?
        try {
            return dimAtomNodeMap.get(expDim).get(atom);
        } catch (Exception e) {
            return null;
        }
    }

    public void addEdge(int i, int j,double weight) {
        addEdge(i,j,true,weight);
    }

    public void addEdge(int i, int j, boolean sym, double weight) {
        ExperimentNode iNode = nodes.get(i);
        ExperimentNode jNode = nodes.get(j);
        addEdge(iNode,jNode,sym,weight);
    }

    public void addEdge(ExperimentNode iNode, ExperimentNode jNode,double weight) {
        addEdge(iNode,jNode,true,weight);
    }

    public void addEdge(ExperimentNode iNode, ExperimentNode jNode, boolean sym, double weight) {
        iNode.getForwardWeightedEdges().put(jNode,weight);
        if (sym) {
            jNode.getBackwardWeightedEdges().put(iNode,weight);
        }
    }
    public void addLeadingEdge (ExperimentNode iNode) {
        addEdge(firstNode,iNode,1.0);
    }

    public void addTrailingEdge (ExperimentNode iNode,double weight) {
        addEdge(iNode,lastNode,weight);
    }

}
