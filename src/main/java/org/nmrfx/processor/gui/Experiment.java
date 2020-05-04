package org.nmrfx.processor.gui;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Experiment {
    private StringProperty name=new SimpleStringProperty();
    private ExpDim first;
    private ExpDim last;
    private int size;
    private IntegerProperty numObsDims=new SimpleIntegerProperty();

    public int getNumObsDims() {
        return numObsDims.get();
    }

    public IntegerProperty numObsDimsProperty() {
        return numObsDims;
    }

    public Experiment(String name){
        setName(name);
        size=0;
        numObsDims.set(0);
    }

    public String toString() {
        return getName();
    }

    public void remove (boolean prompt) {

    }

    public static void addNew() {

    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
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
}
