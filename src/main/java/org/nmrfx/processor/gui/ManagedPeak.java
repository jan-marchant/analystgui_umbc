package org.nmrfx.processor.gui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import org.nmrfx.processor.datasets.peaks.*;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.util.*;

public class ManagedPeak extends Peak {
    private Set<Noe> noes=new HashSet<>();

    public ManagedPeak(int nDim) {
        super(nDim);
    }

    public ManagedPeak(PeakList peakList, int nDim, Set<Noe> noes, HashMap<Integer,PeakDim> peakDims) {
        super(peakList, nDim);
        this.noes=noes;
        //use NOE array to set resonances of NOE dims. But what about non NOE dims?
        for (int i = 0; i < nDim; i++) {
            peakDims.get(i).copyTo(this.getPeakDim(i));
            this.getPeakDim(i).setResonance(peakDims.get(i).getResonance());
            //TODO: Suggest to bruce this would be better in setResonance (only called in NMRStarReader I think)
            this.getPeakDim(i).getResonance().add(this.getPeakDim(i));
        }

        ((ManagedList) peakList).noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
            while (c.next()) {
                for (Noe removedNoe : c.getRemoved()) {
                    if (this.noes.contains(removedNoe)) {
                        remove();
                    }
                }
            }
        });
    }

    public ManagedPeak(PeakList peakList, Peak peak,Set<Noe> noes) {
        super(peakList, peak.getPeakDims().length);
        this.noes=noes;
        peak.copyTo(this);
        for (int i = 0; i < peak.getPeakDims().length; i++) {
            this.getPeakDim(i).setResonance(peak.getPeakDim(i).getResonance());
            //TODO: Suggest to bruce this would be better in setResonance (only called in NMRStarReader I think)
            peak.getPeakDim(i).getResonance().add(this.getPeakDim(i));
        }
        ((ManagedList) peakList).noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
            while (c.next()) {
                for (Noe removedNoe : c.getRemoved()) {
                    if (noes.contains(removedNoe)) {
                        remove();
                    }
                }
            }
        });
    }

    public ManagedPeak(PeakList peakList, int nDim) {
        super(peakList, nDim);
        ((ManagedList) peakList).noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
            while (c.next()) {
                for (Noe removedNoe : c.getRemoved()) {
                    if (noes.contains(removedNoe)) {
                        remove();
                    }
                }
            }
        });
    }

    public ManagedPeak(PeakList peakList,Peak peak) {
        //Allow for peaks picked with fewer peakdims than peaklist size
        //Eventually needs improvement for situations where we want to add higher dim peak
        //from lower dim. E.g. 2D - 3D NOESY.
        super(peakList,peak.getPeakDims().length);
        //this.initPeakDimContribs();
        peak.copyTo(this);
        for (int i = 0; i < peak.getPeakDims().length; i++) {
            this.getPeakDim(i).setResonance(peak.getPeakDim(i).getResonance());
            //TODO: Suggest to bruce this would be better in setResonance (only called in NMRStarReader I think)
            peak.getPeakDim(i).getResonance().add(this.getPeakDim(i));
        }
        NoeSet noeSet=((ManagedList) peakList).noeSet;
        if (noeSet!=null) {
            noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
                while (c.next()) {
                    for (Noe removedNoe : c.getRemoved()) {
                        if (noes.contains(removedNoe)) {
                            remove();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void setStatus(int status) {
        boolean updateStatus=false;
        if (status < 0) {
            if (((ManagedList) peakList).noeType==null) {
                GUIUtils.warn("Cannot remove peak", "Peak patterns are determined by sample and experiment type only.");
                return;
            }

            if (noes.size()==1) {
                ((ManagedList) peakList).noeSet.get().removeAll(noes);
                updateStatus=true;
            } else {
                //popup if multiple NOE dims
                for (Noe noe: noes) {
                    //Will this fail for relabelled peaks? Need to watch and update. Are the spatial sets set up on loading?
                    //can add an NOE without a peak for the other NOE dims.
                    //fixme: better to have a single window with all possible suggestions
                    boolean result=GUIUtils.affirm("Delete NOE between "+noe.spg1.getAnAtom()+" and "+noe.spg2.getAnAtom()+"?");
                    if (result) {
                        updateStatus=true;
                        ((ManagedList) peakList).noeSet.get().remove(noe);
                    }
                }
            }
        }
        if (updateStatus) {
            super.setStatus(status);
        }
    }

    private void remove() {
        //worried about race condition with setStatus
        Platform.runLater(() -> {
            for (PeakDim peakDim : this.peakDims) {
                peakDim.remove();
                if (peakDim.hasMultiplet()) {
                    Multiplet multiplet = peakDim.getMultiplet();
                }
            }
            peakList.unLinkPeak(this);
            this.markDeleted();
            peakList.peaks().remove(this);
            peakList.reIndex();
        });
    }
}
