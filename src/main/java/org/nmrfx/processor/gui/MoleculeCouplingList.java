package org.nmrfx.processor.gui;

import org.nmrfx.structure.chemistry.*;
import org.nmrfx.structure.chemistry.search.MNode;
import org.nmrfx.structure.chemistry.search.MTree;

import java.util.*;

public class MoleculeCouplingList {

    private Molecule mol;

    MTree bondTree;
    MTree tocsyTree;
    Boolean tocsyProcessed=false;
    public HashMap<Atom, Integer> atomNode = new HashMap<Atom, Integer>();
    public HashMap<Integer,List<JCoupling>> couplingMap=new HashMap<>();
    public HashMap<Integer,List<JCoupling>> homoCouplingMap=new HashMap<>();
    public HashMap<Integer,List<JCouplingPath>> transferMap=new HashMap<>();

    public HashMap<Atom,HashMap<Integer,ArrayList<Atom>>> couplingMap2=new HashMap<>();
    public HashMap<Atom,HashMap<Integer,ArrayList<Atom>>> homoCouplingMap2=new HashMap<>();
    public HashMap<Atom,HashMap<Integer,ArrayList<LinkedList<Atom>>>> transferMap2=new HashMap<>();

    public MoleculeCouplingList(Molecule mol) {
        this.mol = mol;
    }

    public void initBondsAndTransfers() {
        initAllBondPaths(5);
        initAllTocsyPaths(5);
    }


    void initAllBondPaths(int maxLength) {
        for (MNode node : getBondTree().getNodes()) {
            depthFirst(node,maxLength);
        }
    }

    void initAllTocsyPaths(int maxLength,int maxWeightProduct) {
        for (MNode node : getTocsyTree(3).getNodes()) {
            depthFirstTransfer(node,maxLength,maxWeightProduct);
        }
    }

    void initAllTocsyPaths(int maxLength) {
        for (MNode node : getTocsyTree(3).getNodes()) {
            depthFirstTransfer(node,maxLength);
        }
    }

    void addPath(int length, LinkedList<MNode> path) {
        //ArrayList<Atom> atoms=new ArrayList<>();
        Atom atom1=path.getFirst().getAtom();
        //atoms.add(atom1);
        Atom atom2= path.getLast().getAtom();
        //atoms.add(atom2);
        //JCoupling jCoupling=JCoupling.couplingFromAtoms(atoms,path.size());
        //addCoupling(length,jCoupling);
        if (!couplingMap2.containsKey(atom1)) {
            HashMap<Integer,ArrayList<Atom>> map = new HashMap<>();
            couplingMap2.put(atom1,map);
        }
        if (!couplingMap2.get(atom1).containsKey(length)) {
            ArrayList<Atom> list = new ArrayList<>();
            couplingMap2.get(atom1).put(length,list);
        }
        couplingMap2.get(atom1).get(length).add(atom2);

        if (atom1.getElementName()==atom2.getElementName()) {
            if (!homoCouplingMap2.containsKey(atom1)) {
                HashMap<Integer,ArrayList<Atom>> map = new HashMap<>();
                homoCouplingMap2.put(atom1,map);
            }
            if (!homoCouplingMap2.get(atom1).containsKey(length)) {
                ArrayList<Atom> list = new ArrayList<>();
                homoCouplingMap2.get(atom1).put(length,list);
            }
            homoCouplingMap2.get(atom1).get(length).add(atom2);
            //addHomoCoupling(atom1.getElementName(),length,jCoupling);
            //addHomoCoupling(length,jCoupling);
        }
    }
    
    void addCoupling(int length, JCoupling jCoupling) {
        if (!couplingMap.containsKey(length)) {
            List<JCoupling> couplings=new ArrayList<>();
            couplingMap.put(length,couplings);
        }
        couplingMap.get(length).add(jCoupling);
    }
    void addHomoCoupling(int length, JCoupling jCoupling) {
        if (!homoCouplingMap.containsKey(length)) {
            List<JCoupling> couplings=new ArrayList<>();
            homoCouplingMap.put(length,couplings);
        }
        homoCouplingMap.get(length).add(jCoupling);
    }

    void depthFirst(MNode parent, int maxLength) {
        LinkedList<MNode> path=new LinkedList<>();
        depthFirst(parent,maxLength,path);
    }

    void depthFirst(MNode parent, int maxLength, LinkedList<MNode> path) {
        if (path.contains(parent)) {
            //add any other coupling breaking logic here - Oxygen? Changing ring? Will see how it looks on the peaklist I guess!
            return;
        }
        path.add(parent);
        int length=path.size()-1;
        if (length > 0 && length<=maxLength) {
            addPath(length,path);
        }
        if (length<maxLength) {
            List<MNode> next = parent.getNodes();
            for (MNode child : next) {
                LinkedList<MNode> next_path=(LinkedList) path.clone();
                depthFirst(child, maxLength, next_path);
            }
        }
    }

    void addPathTransfer(int length, LinkedList<MNode> path) {
        LinkedList<Atom> atoms=new LinkedList();
        for (MNode mNode : path) {
            atoms.add(mNode.getAtom());
        }
        //JCouplingPath jCoupling=new JCouplingPath(atoms,path);
        addTransfer(length,atoms);
    }

    void addTransfer(int length, LinkedList<Atom> atoms) {
        Atom atom1 = atoms.getFirst();
        if (!transferMap2.containsKey(atom1)) {
            //List<JCouplingPath> couplings=new ArrayList<>();
            HashMap<Integer,ArrayList<LinkedList<Atom>>> list = new HashMap<>();
            transferMap2.put(atom1,list);
        }
        if (!transferMap2.get(atom1).containsKey(length)) {
            ArrayList<LinkedList<Atom>> list2 = new ArrayList<>();
            transferMap2.get(atom1).put(length,list2);
        }
        atoms.removeFirst();
        transferMap2.get(atom1).get(length).add(atoms);
    }

    void depthFirstTransfer(MNode parent, int maxLength,int maxWeightProduct) {
        LinkedList<MNode> path=new LinkedList<>();
        depthFirstTransfer(parent,maxLength,path,1,maxWeightProduct);
    }

    void depthFirstTransfer(MNode parent, int maxLength) {
        LinkedList<MNode> path=new LinkedList<>();
        depthFirstTransfer(parent,maxLength,path,null);
    }

    void depthFirstTransfer(MNode parent, int maxLength, LinkedList<MNode> path,Integer weight,int maxWeightProduct) {
        if (path.contains(parent)) {
            return;
        }
        if (weight>maxWeightProduct) {
            return;
        }
        path.add(parent);
        int length=path.size()-1;
        if (length > 0 && length<=maxLength) {
            addPathTransfer(length,path);
        }
        if (length<maxLength) {
            //List<MNode> next = parent.nodes;
            //for (MNode child : next) {
            for (int i=0;i<parent.getNodes().size();i++) {
                MNode child=parent.getNodes().get(i);
                Integer next_weight=weight;
                /*Integer next_weight=parent.weights.get(i)*weight;
                if (next_weight>maxWeightProduct) {
                    continue;
                }*/
                LinkedList<MNode> next_path=(LinkedList) path.clone();
                depthFirstTransfer(child, maxLength, next_path,next_weight,maxWeightProduct);
            }
        }
    }

    void depthFirstTransfer(MNode parent, int maxLength, LinkedList<MNode> path,Integer weight) {
        if (path.contains(parent)) {
            return;
        }
        path.add(parent);
        int length=path.size()-1;
        if (length > 0 && length<=maxLength) {
            addPathTransfer(length,path);
        }
        if (length<maxLength) {
            //List<MNode> next = parent.nodes;
            //for (MNode child : next) {
            for (int i=0;i<parent.getNodes().size();i++) {
                MNode child=parent.getNodes().get(i);
                Integer nextWeight=weight;
                //Integer nextWeight = parent.weights.get(i);
                //only allow TOCSY transfer for equivalent couplings (as judged by bond distance).
                //if (weight==null || weight==nextWeight) {
                    LinkedList<MNode> next_path = (LinkedList) path.clone();
                    depthFirstTransfer(child, maxLength, next_path, nextWeight);
                //}
            }
        }
    }

    private MTree getBondTree() {
        if (bondTree == null) {
            bondTree = new MTree();
            tocsyTree = new MTree();
            int i = 0;
            for (Atom atom : mol.getAtoms()) {
                atomNode.put(atom, Integer.valueOf(i));
                MNode mNode = bondTree.addNode();
                MNode tNode = tocsyTree.addNode();
                mNode.setAtom(atom);
                tNode.setAtom(atom);
                i++;
            }
            for (Atom atom : mol.getAtoms()) {
                for (int iBond = 0; iBond < atom.bonds.size(); iBond++) {
                    Bond bond = atom.bonds.get(iBond);
                    Integer iNodeBegin = (Integer) atomNode.get(bond.begin);
                    Integer iNodeEnd = (Integer) atomNode.get(bond.end);

                    if ((iNodeBegin != null) && (iNodeEnd != null) && (iNodeBegin.intValue()<iNodeEnd.intValue())) {

                        bondTree.addEdge(iNodeBegin.intValue(), iNodeEnd.intValue(),true);
                    }
                }
            }
        }
        return bondTree;
    }

    private MTree getTocsyTree(int maxCouplingDistance) {
        if (tocsyProcessed==false) {
            //for (int j = 1; j <= maxCouplingDistance; j++) {
             //   if (homoCouplingMap.containsKey(j)) {
             //       for (JCoupling jCoupling : homoCouplingMap.get(j)) {

            for (Atom atom1 : mol.getAtoms()) {
                for (int j = 1; j <= maxCouplingDistance; j++) {
                    try {
                        for (Atom atom2 : homoCouplingMap2.get(atom1).get(j)) {
                            Integer iNodeBegin = (Integer) atomNode.get(atom1);
                            Integer iNodeEnd = (Integer) atomNode.get(atom2);
                            if ((iNodeBegin != null) && (iNodeEnd != null)) {
                                //tocsyTree.addEdge(iNodeBegin.intValue(), iNodeEnd.intValue(), false,j);
                                tocsyTree.addEdge(iNodeBegin.intValue(), iNodeEnd.intValue(), false);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            tocsyProcessed=true;
        }
        return tocsyTree;
    }


    public void reset() {
            bondTree=null;
            tocsyTree=null;
            tocsyProcessed=false;
            atomNode.clear();
            couplingMap.clear();
            homoCouplingMap.clear();
            transferMap.clear();
            initBondsAndTransfers();
    }
}
