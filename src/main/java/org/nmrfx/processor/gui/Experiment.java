package org.nmrfx.processor.gui;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nmrfx.processor.operations.Exp;
import org.nmrfx.project.Project;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.utils.GUIUtils;

import java.io.*;
import java.util.*;

public class Experiment {
    public class ExpDims implements Iterable<ExpDim> {
        boolean obs;
        public ExpDims(boolean obs) {
            this.obs=obs;
        }
        @Override
        public Iterator<ExpDim> iterator() {
            return new Iterator<ExpDim>() {

                private ExpDim following=first;
                {
                    if (obs) {
                        while (following != null && !following.isObserved()) {
                            following = following.getNextExpDim();
                        }
                    }
                }

                @Override
                public boolean hasNext() {
                    return following!=null;
                }

                @Override
                public ExpDim next() {
                    if (following == null) {
                        throw new NoSuchElementException();
                    }
                    ExpDim toReturn = following;
                    if (obs) {
                        following = following.getNextExpDim();
                        while (following != null && !following.isObserved()) {
                            following = following.getNextExpDim();
                        }
                    } else {
                        following = following.getNextExpDim();
                    }
                    return toReturn;
                }
            };
        }
    }
    public ExpDims expDims = new ExpDims(false);
    public ExpDims obsDims = new ExpDims(true);

    //public Iterator<ExpDim> obsIterator = obsDims.iterator();
    private StringProperty name=new SimpleStringProperty();
    private int size;
    private IntegerProperty numObsDims=new SimpleIntegerProperty();
    private StringProperty description=new SimpleStringProperty();


    private ExpDim first;
    private ExpDim last;

    public Experiment(String name){
        setName(name);
        size=0;
        numObsDims.set(0);
        UmbcProject.experimentList.add(this);
    }

    public Experiment(String name,String code){
        setName(name);
        size=0;
        numObsDims.set(0);
        //todo implement as name:experiment map to make replacing easier
        String[] codeStrings=code.split("\\|");
        for (int i=0;i<codeStrings.length;) {
            Connectivity connectivity=createConnectivity(codeStrings[i++]);
            ExpDim expDim=new ExpDim(codeStrings[i++]);
            this.add(connectivity,expDim);
        }
        UmbcProject.experimentList.add(this);
    }

    public static Experiment get(String name) {
        for (Experiment experiment : UmbcProject.getActive().experimentList) {
            if (experiment.getName().equals(name)) {
                return experiment;
            }
        }
        return null;
    }

    public Connectivity createConnectivity(String code) {
        if (code.equals("")) {
            return null;
        } else {
            return new Connectivity(code);
        }
    }

    public String toCode() {
        String toReturn="";
        for (ExpDim expDim : expDims) {
            toReturn+=expDim.getNextCon(false)==null?"":"|"+expDim.getNextCon(false).toCode();
            toReturn+="|";
            toReturn+=expDim.toCode();
        }
        return toReturn;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name.get();
    }

    public String describe() {
        String toReturn="";
        for (ExpDim expDim : expDims) {
            toReturn+=expDim.getNextCon(false)==null?"":expDim.getNextCon(false).toString()+"→";
            toReturn+=expDim.toString() + "("+expDim.getNucleus().getNumberName()+"): "+expDim.getPattern();
            toReturn+=expDim.getNextCon(true)==null?"":"→";
        }
        return toReturn;
    }
    public int getNumObsDims() {
        return numObsDims.get();
    }
    public IntegerProperty numObsDimsProperty() {
        return numObsDims;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void remove(boolean prompt) {
        boolean delete=true;
        if (prompt) {
            delete = GUIUtils.affirm("Are you sure you want to delete? This cannot be undone.");
        }
        if (delete) {
            UmbcProject.experimentList.remove(this);
            writePar("data/experiments");
        }
    }

    public static void addNew() {
        new ExperimentSetup();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public static class Pair {
        public final Connectivity connectivity;
        public final ExpDim expDim;
        public Pair(Connectivity connectivity, ExpDim expDim) {
            this.connectivity =connectivity;
            this.expDim = expDim;
        }
    }

    public final boolean add (Pair... values) {
        ExpDim new_first=null;
        ExpDim new_last=null;
        Connectivity new_first_con=null;
        int new_size=size;
        int newNumObsDims=numObsDims.get();
        boolean added = false;
        for (Pair pair : values) {
            if (pair.expDim == null) {
                return false;
            }
            if (new_first == null) {
                new_first = pair.expDim;
                new_first_con=pair.connectivity;
                new_last = pair.expDim;
                added = true;
            } else if (pair.connectivity != null) {
                new_last.setNext(pair.connectivity, pair.expDim);
                new_last=pair.expDim;
                added = true;
            } else {
                return false;
            }
            new_size += 1;
            if ((pair.expDim).isObserved()) {
                newNumObsDims += 1;
            }
        }
        if (added) {
            if (first==null) {
                first=new_first;
            } else {
                if (new_first_con==null) {
                    return false;
                }
                last.setNext(new_first_con,new_first);
            }
            last=new_last;
            size=new_size;
            numObsDims.set(newNumObsDims);
        }
        setDescription(describe());
        return added;
    }

    public boolean add (Connectivity connectivity, ExpDim expDim) {
        boolean added=false;
        if (expDim==null) {return false;}
        if (first==null) {
            first=expDim;
            last=expDim;
            added=true;
        } else if (connectivity!=null) {
            last.setNext(connectivity,expDim);
            last=expDim;
            added=true;
        }
        if (added) {
            size+=1;
            if (expDim.isObserved()) {numObsDims.set(numObsDims.get()+1);}
        }
        setDescription(describe());
        return added;
    }

    public ExpDim getFirst() {
        return first;
    }

    public ExpDim getLast() {
        return last;
    }

    public int getSize() {
        return size;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    //fixme: choose a better location - currently I think user changes will be lost when updating NMRFx
    public static void writePar(String resourceName) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        try (PrintStream pStream = new PrintStream(cl.getResource(resourceName).getPath())) {
            for (Experiment experiment : UmbcProject.experimentList) {
                pStream.printf("%s = %s\n", experiment.getName(),experiment.toCode());
            }
        } catch (IOException ioE) {
            System.out.println("error " + ioE.getMessage());
        }
    }


    public static void readPar(String resourceName) {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            InputStream iStream = cl.getResourceAsStream(resourceName);
            Scanner inputStream = new Scanner(iStream);
            while (inputStream.hasNextLine()) {
                String data = inputStream.nextLine();
                if (!data.isEmpty()) {
                    String[] arrOfStr = data.split("=");
                    if (arrOfStr.length != 2) {
                        System.out.println("Error reading experiment: " + data);
                    } else {
                        String name = arrOfStr[0].trim();
                        String code = arrOfStr[1].trim();
                        new Experiment(name, code);
                    }
                }
            }
            iStream.close();
        } catch (IOException e) {
            System.out.println("Couldn't read "+resourceName);
        }
    }
}
