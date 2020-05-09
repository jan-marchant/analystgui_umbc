package org.nmrfx.processor.gui;


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.*;

public class Experiment {
    class ExpDims implements Iterable<ExpDim> {
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

    private StringProperty name=new SimpleStringProperty();
    private int size;
    private IntegerProperty numObsDims=new SimpleIntegerProperty();

    private ExpDim first;
    private ExpDim last;

    public Experiment(String name){
        setName(name);
        size=0;
        numObsDims.set(0);
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name.get();
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

    public void remove (boolean prompt) {

    }

    public static void addNew() {

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
