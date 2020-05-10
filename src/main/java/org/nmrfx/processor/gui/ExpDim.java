package org.nmrfx.processor.gui;

import com.sun.prism.impl.TextureResourcePool;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.Residue;
import org.omg.CORBA.ARG_IN;
import org.python.antlr.ast.Str;
import org.python.core.PyDictionary;
import org.python.jsr223.ScriptEngineTest;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private HashMap<Molecule, HashMap<Atom,String>> molAtomMap=new HashMap<>();
    private static Pattern matchPattern = Pattern.compile("^(\\*|[A-z]*)(?:\\((\\*|[A-z])\\))?\\.([^,:]*)(?::([0-9\\.]*))?$");
    private static HashMap<String, HashMap<String, ArrayList<String>>> resMap = new HashMap<>();
    private ArrayList<Match> matches=new ArrayList<>();

    public ExpDim(Nuclei nucleus,Boolean observed) {
        this.observed=observed;
        this.nucleus=nucleus;
        this.pattern="*(*).*";
        parsePattern();
    }
    public ExpDim(Boolean observed) {
        this.observed=observed;
        this.nucleus=Nuclei.H1;
        this.pattern="*(*).*";
        parsePattern();
    }
    public ExpDim(Nuclei nucleus,Boolean observed,String pattern) {
        this.observed=observed;
        this.nucleus=nucleus;
        this.pattern=pattern;
        parsePattern();
    }
    public ExpDim(Boolean observed,String pattern) {
        this.observed=observed;
        this.nucleus=Nuclei.H1;
        this.pattern=pattern;
        parsePattern();
    }

    private void initMolAtomMap(Molecule mol) {
        HashMap<Atom,String> activeAtoms=new HashMap<>();
        molAtomMap.put(mol,activeAtoms);
        for (Atom atom : mol.getAtoms()) {
            if (atom.getElementName()!=nucleus.getName()) {
                continue;
            }
            Entity entity=atom.getEntity();
            String oneLetter;
            if (entity instanceof Residue) {
                oneLetter=String.valueOf(((Residue) entity).getOneLetter());
            } else {
                oneLetter=null;
            }
            for (Match match : matches) {
                if (match.resType.equalsIgnoreCase("*") || match.resType.equalsIgnoreCase(entity.getName()) || match.resType.equalsIgnoreCase(oneLetter)) {
                    //resId irrelevant here but probably worth saving
                    String testPat;
                    if (match.atomPat.substring(0,nucleus.getName().length()).equalsIgnoreCase(nucleus.getName())) {
                        testPat = match.atomPat.substring(nucleus.getName().length());
                    } else {
                        testPat=match.atomPat;
                    }
                    if (testPat.equalsIgnoreCase("*") || match.atomPat.equalsIgnoreCase(atom.getName()) || patternShortcuts(entity.getName(),atom.getName(),testPat)) {
                        activeAtoms.put(atom,match.resId);
                    }
                }
            }
        }
    }

    public Nuclei getNucleus() {
        return nucleus;
    }

    public Boolean isObserved() {
        return observed;
    }

    public void setNext(Connectivity nextCon,ExpDim nextExpDim) {
        this.nextCon = nextCon;
        nextExpDim.previousCon=nextCon;
        this.nextExpDim=nextExpDim;
        nextExpDim.previousExpDim=this;
    }

    public ExpDim getNextExpDim() {
        return getNextExpDim(true);
    }
    public ExpDim getNextExpDim(boolean forward) {
        if (forward) {
            return nextExpDim;
        } else {
            return previousExpDim;
        }
    }

    public Connectivity getNextCon() {
        return getNextCon(true);
    }
    public Connectivity getNextCon(boolean forward) {
        if (forward) {
            return nextCon;
        } else {
            return previousCon;
        }
    }

    public Set<Atom> getActiveAtoms(Molecule mol) {
        if (!molAtomMap.containsKey(mol)) {
            initMolAtomMap(mol);
        }
        return molAtomMap.get(mol).keySet();
    }
    public List<Atom> getConnected(Atom atom) {
        ArrayList<Atom> atomList=new ArrayList<>();
        if (getNextCon()!=null) {
            for (Atom connectedAtom : getNextCon().getConnections(atom)) {
                if (resPatMatches(atom,connectedAtom)) {
                    atomList.add(connectedAtom);
                }
            }
        }
        return atomList;
    }

    public boolean resPatMatches(Atom atom,Atom connectedAtom) {
        return (resPat(atom) == "*" || nextExpDim.resPat(connectedAtom) == "*" ||
                (resPat(atom).equalsIgnoreCase(nextExpDim.resPat(connectedAtom)) && atom.getEntity()==connectedAtom.getEntity())
                || (!resPat(atom).equalsIgnoreCase(nextExpDim.resPat(connectedAtom)) && atom.getEntity()!=connectedAtom.getEntity()));
    }

    private class Match {
        String resType;
        String resId;
        String atomPat;
        double fraction;
        public Match (String resType,String resId,String atomPat,double fraction) {
            this.resType=resType;
            this.resId=resId;
            this.atomPat=atomPat;
            this.fraction=fraction;
        }
    }

    private void parsePattern() {
        for (String group : pattern.split(",")) {
            Matcher matcher = matchPattern.matcher(group.trim());
            if (!matcher.matches()) {
                System.out.println("Group " + group + " can't be parsed - skipping");
                continue;
            }
            String resType = matcher.group(1);
            String resId = matcher.group(2);
            if (resId==null) {
                resId="*";
            }
            String atomPat = matcher.group(3);
            String fractionPat = matcher.group(4);
            double fraction;
            try {
                fraction=Double.parseDouble(fractionPat);
            } catch (Exception e) {
                fraction=1.0;
            }
            matches.add(new Match(resType,resId,atomPat,fraction));
        }
    }

    private String resPat(Atom atom) {
        Molecule mol=atom.getTopEntity().molecule;
        if (!molAtomMap.containsKey(mol)) {
            initMolAtomMap(mol);
        }
        return molAtomMap.get(mol).get(atom);
    }

    public static boolean patternShortcuts(String entityName,String atomName,String testPat) {
        if (resMap.containsKey(entityName.toLowerCase()) &&
                resMap.get(entityName.toLowerCase()).containsKey(testPat.toLowerCase()) &&
                resMap.get(entityName.toLowerCase()).get(testPat.toLowerCase()).contains(atomName.toLowerCase())) {
            return true;
        } else {
            return false;
        }
    }

    static {
        String[][] shortCodes = {
                {"ala", "ali", "ha", "hb1", "hb2", "hb3", "ca", "cb"},
                {"arg", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "hd2", "hd3", "ca", "cb", "cg", "cd"},
                {"asn", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"asp", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"cys", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"gln", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "ca", "cb", "cg"},
                {"glu", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "ca", "cb", "cg"},
                {"gly", "ali", "ha2", "ha3", "ca"},
                {"his", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"his", "arom", "hd2", "he1", "cg", "cd2", "ce1"},
                {"ile", "ali", "ha", "hb", "hg21", "hg22", "hg23", "hg12", "hg13", "hd11", "hd12", "hd13", "ca", "cb", "cg1", "cg2", "cd1"},
                {"lys", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "hd2", "hd3", "he2", "he3", "ca", "cb", "cg", "cd", "ce"},
                {"leu", "ali", "ha", "hb2", "hb3","hg", "hd11", "hd12", "hd13", "hd21", "hd22", "hd23", "ca", "cb", "cg", "cd1", "cd2"},
                {"met", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "ca", "cb", "cg"},
                {"phe", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"phe", "arom", "hd1", "hd2", "he1", "he2", "hz", "cg", "cd1", "cd2", "ce1", "ce2", "cz"},
                {"pro", "ali", "ha", "hb2", "hb3", "hg2", "hg3", "hd2", "hd3", "ca", "cb", "cg", "cd"},
                {"ser", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"thr", "ali", "ha", "hb", "hg21", "hg22", "hg23", "ca", "cb", "cg2"},
                {"trp", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"trp", "arom", "hd1", "he3", "hz2", "hz3", "hh2", "cg", "cd1", "cd2", "ce2", "ce3", "cz2", "cz3", "ch2"},
                {"tyr", "ali", "ha", "hb2", "hb3", "ca", "cb"},
                {"tyr", "arom", "hd1", "hd2", "he1", "he2", "cg", "cd1", "cd2", "ce1", "ce2"},
                {"val", "ali", "ha", "hb", "hg11", "hg12", "hg13", "hg21", "hg22", "hg23", "ca", "cb", "cg1", "cg2"},
                {"dade", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"dade", "n", "h2", "h8"},
                {"dcyt", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"dgua", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"dthy", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"da", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"da", "n", "h2", "h8"},
                {"dc", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"dg", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"dt", "d", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"rade", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"rade", "n", "h2", "h8"},
                {"rcyt", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"rgua", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"rura", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"ra", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"ra", "n", "h2", "h8"},
                {"rc", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"rg", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"ru", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"a", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"a", "n", "h2", "h8"},
                {"c", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"g", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"},
                {"u", "r", "h1'", "h2'", "h3'", "h4'", "h5'", "h5''"}
        };
        for (String[] strings : shortCodes) {
            HashMap<String, ArrayList<String>> codeMap;
            String res = strings[0];
            if (!resMap.containsKey(res)) {
                codeMap = new HashMap<>();
                resMap.put(res, codeMap);
            } else {
                codeMap = resMap.get(res);
            }
            String code = strings[1];
            ArrayList<String> atoms = new ArrayList<>(Arrays.asList(strings).subList(2, strings.length));
            codeMap.put(code, atoms);
        }
    }
}
