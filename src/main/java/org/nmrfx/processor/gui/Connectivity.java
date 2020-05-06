package org.nmrfx.processor.gui;

import org.apache.commons.lang3.StringUtils;
import org.nmrfx.processor.operations.Exp;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.*;
import org.nmrfx.structure.chemistry.constraints.Noe;
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

    public double getTransfers (List<Atom> atoms1,List<Atom> atoms2) {
        /**
         * Does Atom1 have a valid connection to Atom2 (Fn - Fn+1)?
         * A connection here is an edge on the graph describing the possible paths of magnetization transfer
         * where the nodes are Atom objects associated with a specific ExpDim
         * Experiment takes care that the requested Atoms meet the pattern. This takes care that they are feasibly connected by the experiment type
         */
        double intensity=0.0;;
        List<Atom> atoms=new ArrayList<>();
        if (type==TYPE.NOE) {
            for (Noe noe : getNoes(noeSet,atoms1,atoms2,true)) {
                //will have to play with this
                intensity+=noe.getIntensity();
            }
        } else if (type==TYPE.J) {

        } else if (type==TYPE.HBOND) {

        } else if (type==TYPE.TOCSY) {

        }
        return intensity;
    }

    public synchronized List<Noe> getNoes(NoeSet noeSet,List<Atom> atoms1, List<Atom> atoms2, boolean requireActive) {
        List listCopy = new ArrayList();
        if (noeSet.isDirty()) {
            //hmmm
            boolean useDistances=false;
            noeSet.updateContributions(useDistances, requireActive);
        }
        if (atoms1.size() == 0 || atoms2.size()==0) {
            return listCopy;
        } else {
            for (Noe noe : noeSet.get()) {
                if (requireActive && !noe.isActive()) {
                    continue;
                }
                if (
                        (atoms1.contains(noe.spg1.getAnAtom()) && atoms2.contains(noe.spg2.getAnAtom())) ||
                        (atoms2.contains(noe.spg1.getAnAtom()) && atoms1.contains(noe.spg2.getAnAtom()))
                ) {
                    listCopy.add(noe);
                }
            }
        }
        return listCopy;
    }

    public HashMap<Atom, Set<Atom>> getJConnections (UmbcProject project,Set<Atom> atoms1, Set<Atom> atoms2) {
        //fixme: MoleculeCouplings to Molecule class
        project.getMoleculeCouplingList().checkMol();
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
        project.getMoleculeCouplingList().checkMol();
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
