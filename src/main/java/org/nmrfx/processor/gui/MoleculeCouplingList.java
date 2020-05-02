package org.nmrfx.processor.gui;

import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.structure.chemistry.search.MNode;
import org.nmrfx.structure.chemistry.search.MTree;

import java.util.*;

public class MoleculeCouplingList {

    private UmbcProject project;
    private Molecule mol;

    //HashMap<Atom, HashMap<Integer, Set<LinkedList<Atom>>>> jMap = new HashMap();
    //HashMap<Atom, HashMap<Integer, Set<LinkedList<Atom>>>> tocsyMap = new HashMap();

    MTree bondTree;
    MTree tocsyTree;
    Boolean tocsyProcessed=false;
    public HashMap<Atom, Integer> atomNode = new HashMap<Atom, Integer>();
    public HashMap<Integer,List<JCoupling>> couplingMap=new HashMap<>();
    public HashMap<Integer,List<JCoupling>> homoCouplingMap=new HashMap<>();
    public HashMap<Integer,List<JCouplingPath>> transferMap=new HashMap<>();

    //HashMap<String,HashMap<Integer,List<JCoupling>>> homoCouplingMaps=new HashMap<>();

    public MoleculeCouplingList(UmbcProject project) {
        this.project = project;
        //this.mol=project.activeMol;
        //initBondsAndTransfers();

    }

    public void initBondsAndTransfers() {
        this.mol=project.activeMol;
        initAllBondPaths(5);
        initAllTocsyPaths(5);
    }


    void initAllBondPaths(int maxLength) {
        for (MNode node : getBondTree().nodes) {
            depthFirst(node,maxLength);
        }
    }

    void initAllTocsyPaths(int maxLength) {
        for (MNode node : getTocsyTree(3).nodes) {
            depthFirstTransfer(node,maxLength);
        }
    }

    void addPath(int length, LinkedList<MNode> path) {
        ArrayList<Atom> atoms=new ArrayList();
        Atom atom1=path.getFirst().getAtom();
        atoms.add(atom1);
        Atom atom2= path.getLast().getAtom();
        atoms.add(atom2);
        JCoupling jCoupling=JCoupling.couplingFromAtoms(atoms,path.size());
        addCoupling(length,jCoupling);
        if (atom1.getElementName()==atom2.getElementName()) {
            //addHomoCoupling(atom1.getElementName(),length,jCoupling);
            addHomoCoupling(length,jCoupling);
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
            return;
        }
        path.add(parent);
        int length=path.size()-1;
        if (length > 0 && length<=maxLength) {
            addPath(length,path);
        }
        if (length<maxLength) {
            List<MNode> next = parent.nodes;
            for (MNode child : next) {
                LinkedList<MNode> next_path=(LinkedList) path.clone();
                depthFirst(child, maxLength, next_path);
            }
        }
    }

    void addPathTransfer(int length, LinkedList<MNode> path) {
        ArrayList<Atom> atoms=new ArrayList();
        Atom atom1=path.getFirst().getAtom();
        atoms.add(atom1);
        Atom atom2= path.getLast().getAtom();
        atoms.add(atom2);
        JCouplingPath jCoupling=new JCouplingPath(atoms,path);
        addTransfer(length,jCoupling);
    }

    void addTransfer(int length, JCouplingPath jCoupling) {
        if (!transferMap.containsKey(length)) {
            List<JCouplingPath> couplings=new ArrayList<>();
            transferMap.put(length,couplings);
        }
        transferMap.get(length).add(jCoupling);
    }

    void depthFirstTransfer(MNode parent, int maxLength) {
        LinkedList<MNode> path=new LinkedList<>();
        depthFirstTransfer(parent,maxLength,path);
    }

    void depthFirstTransfer(MNode parent, int maxLength, LinkedList<MNode> path) {
        if (path.contains(parent)) {
            return;
        }
        path.add(parent);
        int length=path.size()-1;
        if (length > 0 && length<=maxLength) {
            addPathTransfer(length,path);
        }
        if (length<maxLength) {
            List<MNode> next = parent.nodes;
            for (MNode child : next) {
                LinkedList<MNode> next_path=(LinkedList) path.clone();
                depthFirstTransfer(child, maxLength, next_path);
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
            for (int j = 1; j <= maxCouplingDistance; j++) {
                if (homoCouplingMap.containsKey(j)) {
                    for (JCoupling jCoupling : homoCouplingMap.get(j)) {
                        Integer iNodeBegin = (Integer) atomNode.get(jCoupling.getAtom(0));
                        Integer iNodeEnd = (Integer) atomNode.get(jCoupling.getAtom(1));
                        if ((iNodeBegin != null) && (iNodeEnd != null)) {
                            tocsyTree.addEdge(iNodeBegin.intValue(), iNodeEnd.intValue(), false);
                        }
                    }
                }
            }
            tocsyProcessed=true;
        }
        return tocsyTree;
    }


    public void checkMol() {
        if (mol!=project.activeMol) {
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

    /*
    //fixme: Set not required here - probably adds overhead
    public Set<LinkedList<Atom>> getBondPaths(Atom atom, int length) {
        //fixme: this approach doesn't take advantage of symmetry of couplings. Can we do better?
        if (length<1) {
            return null;
        }
        if (!jMap.containsKey(atom)) {
            jMap.put(atom,new HashMap<>());
        }
        if (!jMap.get(atom).containsKey(length)) {
            Set<LinkedList<Atom>> bondedAtomLists = new HashSet<>();
            Set<Atom> allBondedAtomList = new HashSet<>();
            Atom bondedAtom;
            for (Bond bond : atom.bonds) {
                if (bond.begin == atom) {
                    bondedAtom=bond.end;
                } else if (bond.end == atom) {
                    bondedAtom=bond.begin;
                } else {
                    System.out.println("Check bonds for atom " + atom.getName());
                    continue;
                }
                allBondedAtomList.add(bondedAtom);
                if (length==1) {
                    LinkedList<Atom> bondedAtomList = new LinkedList<>();
                    bondedAtomList.add(atom);
                    bondedAtomList.add(bondedAtom);
                    bondedAtomLists.add(bondedAtomList);
                }
            }
            if (length>1) {
                for (Atom bondedAtom2 : allBondedAtomList) {
                    for (LinkedList list : getBondPaths(bondedAtom2, length - 1)) {
                        if (!list.contains(atom)) {
                            list.addFirst(atom);
                            bondedAtomLists.add(list);
                        }
                    }
                }
            }
            jMap.get(atom).put(length,bondedAtomLists);
        }
        return jMap.get(atom).get(length);
    }

    public Set<LinkedList<Atom>> getTocsyPaths(Atom atom, int length, int couplingDistance) {
        //fixme: this approach doesn't take advantage of symmetry of couplings. Can we do better?
        if (length<1) {
            return null;
        }
        if (!tocsyMap.containsKey(atom)) {
            tocsyMap.put(atom,new HashMap<>());
        }
        if (!tocsyMap.get(atom).containsKey(length)) {
            Set<LinkedList<Atom>> transferPathLists = new HashSet<>();
            Set<Atom> allTransferAtoms = new HashSet<>();
            Set<Atom> transferAtoms = new HashSet<>();
            for (LinkedList<Atom> bondPath : getBondPaths(atom,couplingDistance)) {
                for (Atom transferAtom : bondPath) {
                    if (atom.getElementNumber()==transferAtom.getElementNumber()) {
                        allTransferAtoms.add(transferAtom);
                        if (length==1) {
                            LinkedList<Atom> transferAtomList = new LinkedList<>();
                            transferAtomList.add(atom);
                            transferAtomList.add(transferAtom);
                            transferPathLists.add(transferAtomList);
                        }
                    }
                }
            }
            if (length>1) {
                for (Atom transferAtom2 : allTransferAtoms) {
                    for (LinkedList list : getTocsyPaths(transferAtom2, length - 1,couplingDistance)) {
                        if (!list.contains(atom)) {
                            list.addFirst(atom);
                            transferPathLists.add(list);
                        }
                    }
                }
            }
            tocsyMap.get(atom).put(length,transferPathLists);
        }
        return tocsyMap.get(atom).get(length);
    }*/


/*    public Set<Atom> getTocsyCouplings (Atom atom,int transfers,int couplingDistance) {
        if (transfers<1) {
            return null;
        }
        if (!tocsyMap.containsKey(atom)) {
            tocsyMap.put(atom,new HashMap<>());
        }
        if (!tocsyMap.get(atom).containsKey(transfers)) {
            Set<Atom> transferAtoms = new HashSet<>();
            for (int i=1;i>=couplingDistance;i++) {
                transferAtoms.addAll(getBondPaths(atom,i));
            }
            if (transfers==1) {
                transferAtoms.remove(atom);
                tocsyMap.get(atom).put(transfers,transferAtoms);
            } else {
                Set<Atom> allAtoms = new HashSet<>();
                for (Atom transferAtom2 : transferAtoms) {
                    allAtoms.addAll(getTocsyCouplings(transferAtom2,transfers-1,couplingDistance));
                }
                allAtoms.remove(atom);
                tocsyMap.get(atom).put(transfers,allAtoms);
            }
        }
        return tocsyMap.get(atom).get(transfers);
    }
    */

}
