package org.nmrfx.processor.gui;

import org.apache.commons.lang3.StringUtils;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.structure.chemistry.constraints.NoeSet;

import java.util.*;

public class Connectivity {

    public enum TYPE {
        NOE,
        J,
        TOCSY,
        HBOND;
    }
    public enum NOETYPE {
        PEAKS("From Peaks"),
        ATTRIBUTES("Attributes-based"),
        STRUCTURE("From Structure");

        private String label;

        NOETYPE(String label) {
            this.label = label;
        }
        public String toString() {
            return label;
        }
    }
    //this doesn't belong here
    private NoeSet noeSet;
    public TYPE type;
    /**Rough idea at the moment - best not to expose to the world, can set up lots of prototype experiments instead
     **/
    private int minTransfers;
    private int maxTransfers;
    private String numBonds;

    public Connectivity(int minTransfers, int maxTransfers) {
        this.type=TYPE.TOCSY;
        this.minTransfers=minTransfers;
        this.maxTransfers=maxTransfers;
    }
    public Connectivity(String numBonds) {
        this.type=TYPE.J;
        this.numBonds=numBonds;
    }
    public Connectivity(Connectivity.TYPE type) {
        this.type=type;
        switch (type) {
            case J:
                numBonds="1";
                break;
            case TOCSY:
                minTransfers=1;
                maxTransfers=1;
                break;
        }
    }

    public Set<Atom> getConnections(Atom atom) {
        Set<Atom> atoms=new HashSet<>();
        switch (type) {
            case NOE:
                //these are added in the peaklist tree
                break;
            case J:
                atoms.addAll(getJConnections(atom));
                break;
            case TOCSY:
                atoms.addAll(getTocsyConnections(atom));
                break;
            case HBOND:
                System.out.println("HBOND not yet implemented");
                break;
        }
        return atoms;
    }

    public Set<Atom> getJConnections(Atom atom) {
        Molecule mol=atom.getTopEntity().molecule;
        Set<Atom> atoms=new HashSet<>();

        String[] bondsString = StringUtils.split(numBonds, ",");
        List<Integer> nBondsList = new ArrayList<Integer>();
        for (String number : bondsString) {
            nBondsList.add(Integer.parseInt(number.trim()));
        }
        for (int bonds : nBondsList) {
            atoms.addAll(UmbcProject.getMoleculeCouplingList(mol).couplingMap2.get(atom).get(bonds));
        }

        return atoms;
    }

    public Set<Atom> getTocsyConnections(Atom atom) {
        Molecule mol=atom.getTopEntity().molecule;
        Set<Atom> atoms=new HashSet<>();

        for (int transfers = minTransfers; transfers<= maxTransfers; transfers++) {
            for (LinkedList<Atom> list : UmbcProject.getMoleculeCouplingList(mol).transferMap2.get(atom).get(transfers)) {
                //fixme: some way to check on labeling of intermediates in TOCSY transfer path
                atoms.add(list.getLast());
            }
        }

        return atoms;
    }


    public HashMap<Atom, Set<Atom>> getJConnections (UmbcProject project,Set<Atom> atoms1, Set<Atom> atoms2) {
        //fixme: MoleculeCouplings to Molecule class
        project.getMoleculeCouplingList(project.activeMol).reset();
        HashMap<Atom, Set<Atom>> connections = new HashMap<>();

        String[] bondsString = StringUtils.split(numBonds, ",");
        List<Integer> nBondsList = new ArrayList<Integer>();
        for (String number : bondsString) {
            nBondsList.add(Integer.parseInt(number.trim()));
        }
        Set<Atom> connected = new HashSet<>();
        for (Atom atom : atoms1) {
            for (int bonds : nBondsList) {
                //for (LinkedList<Atom> bondPath : project.getMoleculeCouplingList().getBondPaths(atom, bonds)) {
                 //   connected.add(bondPath.getLast());
                //}
            }
            connected.retainAll(atoms2);
            connections.put(atom,connected);
            connected.clear();
        }
        return connections;
    }

    public HashMap<Atom, Set<Atom>> getTocsyConnections (UmbcProject project,Set<Atom> atoms1, Set<Atom> atoms2) {
        project.getMoleculeCouplingList(project.activeMol).reset();
        HashMap<Atom, Set<Atom>> connections = new HashMap<>();

        Set<Atom> connected = new HashSet<>();
        for (Atom atom : atoms1) {
            for (int transfers = minTransfers; transfers<= maxTransfers; transfers++) {
                //for (LinkedList<Atom> transferPath : project.getMoleculeCouplingList().getTocsyPaths(atom, transfers,3)) {
                //    connected.add(transferPath.getLast());
                //}
            }
            //fixme: this ignores whether intervening nuclei are active in the labeling scheme
            connected.retainAll(atoms2);
            connections.put(atom,connected);
            connected.clear();
        }
        return connections;
    }

    public TYPE getType() {
        return type;
    }

    public int getMinTransfers() {
        return minTransfers;
    }

    public int getMaxTransfers() {
        return maxTransfers;
    }

    public String getNumBonds() {
        return numBonds;
    }
}
