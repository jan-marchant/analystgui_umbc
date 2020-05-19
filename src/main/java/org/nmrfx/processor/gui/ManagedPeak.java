package org.nmrfx.processor.gui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.*;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.PPMv;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.util.*;

import static org.nmrfx.processor.datasets.peaks.PeakDim.resFactory;

public class ManagedPeak extends Peak {
    private Set<Noe> noes=new HashSet<>();

    public ManagedPeak(int nDim) {
        super(nDim);
    }

    public ManagedPeak(PeakList peakList, int nDim, Set<Noe> noes, HashMap<Integer, Atom> atoms) {
        super(peakList, nDim);
        this.noes=noes;

        float scale=1f;
        for (int i = 0; i < nDim; i++) {
            PeakDim peakDim0=null;
            AtomResonance resonance=null;
            for (Noe noe : noes) {
                if (noe.getResonance1().getAtom()==atoms.get(i)) {
                    resonance=noe.getResonance1();
                    for (PeakDim peakDim : noe.peak.getPeakDims()) {
                        if (peakDim.getResonance()==resonance) {
                            peakDim0=peakDim;
                        }
                    }
                }
                if (noe.getResonance2().getAtom()==atoms.get(i)) {
                    resonance=noe.getResonance2();
                    for (PeakDim peakDim : noe.peak.getPeakDims()) {
                        if (peakDim.getResonance()==resonance) {
                            peakDim0=peakDim;
                        }
                    }
                }
            }
            if (resonance==null && atoms.get(i).getResonance()!=null) {
                resonance=atoms.get(i).getResonance();
            }
            if (resonance==null) {
                resonance = (AtomResonance) resFactory().build();
            }
            resonance.add(this.getPeakDim(i));
            this.getPeakDim(i).setLabel(atoms.get(i).getShortName());
            atoms.get(i).setResonance(resonance);


            Dataset dataset=((ManagedList) peakList).getAcquisition().getDataset();
            /*float width=(float) dataset.ptWidthToPPM(i,2);
            if (width<0.01f) {width=0.01f;}
            */
            float width;
            switch (atoms.get(i).getElementName()) {
                case "C":
                    width= 0.15f;
                    scale=3f;
                    break;
                case "N":
                    width= 0.3f;
                    scale=3f;
                    break;
                default:
                    width= 0.01f;
            }


            if (peakDim0!=null) {
                PeakDim thisPeakDim=this.getPeakDim(i);
                if (thisPeakDim.getSpectralDim()==peakDim0.getSpectralDim()) {
                    Dataset dataset0=Dataset.getDataset(peakDim0.getPeakList().getDatasetName());
                    if (dataset0!=null) {
                        //width = (float) (peakDim0.getLineWidthValue()*dataset.ptWidthToPPM(i,2)/dataset0.ptWidthToPPM(peakDim0.getSpectralDim(),2));
                    }
                }
                Float pickedShift = peakDim0.getChemShift();

                List<PeakDim> peakDims = peakDim0.getResonance().getPeakDims();
                Set<PeakDim> updateMe = new HashSet<>();
                updateMe.add(thisPeakDim);
                Boolean freezeMe = false;

                for (PeakDim peakDim : peakDims) {
                    if (peakDim == peakDim0 || peakDim==thisPeakDim) {
                        continue;
                    }
                    if (peakDim.getSampleConditionLabel().equals(thisPeakDim.getSampleConditionLabel())
                    && peakDim.getSampleLabel().equals(thisPeakDim.getSampleLabel())) {
                        if (peakDim.isFrozen()) {
                            pickedShift = peakDim.getChemShift();
                            freezeMe = true;
                        } else {
                            updateMe.add(peakDim);
                        }
                        if (thisPeakDim.getSpectralDimObj()==peakDim.getSpectralDimObj()) {
                            width = peakDim.getLineWidthValue();
                        }
                    }
                }
                for (PeakDim peakDim : updateMe) {
                    peakDim.setChemShift(pickedShift);
                }
                thisPeakDim.setFrozen(freezeMe);
            } else {
                PPMv ppm;
                ppm = atoms.get(i).getPPM(((ManagedList) getPeakList()).getPpmSet());
                if (ppm == null) {
                    ppm = atoms.get(i).getRefPPM(((ManagedList) getPeakList()).getRPpmSet());
                }
                if (ppm != null) {
                    this.getPeakDim(i).setChemShift((float) ppm.getValue());
                    this.getPeakDim(i).setChemShiftErrorValue((float) ppm.getError());
                }
            }

            this.getPeakDim(i).setLineWidthValue(width);
        }
        for (PeakDim peakDim : getPeakDims()) {
            peakDim.setLineWidthValue(peakDim.getLineWidthValue()*scale);
            peakDim.setBoundsValue(peakDim.getLineWidthValue()*1.5f);
        }

        if (((ManagedList) peakList).noeSet!=null) {
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
    }

    public ManagedPeak(PeakList peakList, int nDim) {
        super(peakList, nDim);
    }

    @Override
    public void setStatus(int status) {
        boolean updateStatus=false;
        if (status < 0) {
            if (((ManagedList) peakList).noeSet==null) {
                GUIUtils.warn("Cannot remove peak", "Peak patterns are determined by sample and experiment type only.");
                return;
            }

            if (noes.size()==1) {
                ((ManagedList) peakList).noeSet.get().removeAll(noes);
                updateStatus=true;
            } else {
                //popup if multiple NOE dims
                for (Noe noe: noes) {
                    //TODO: Will this fail for relabelled peaks? Need to watch and update. Are the spatial sets set up on loading?
                    // can add an NOE without a peak for the other NOE dims.
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

    public Set<Noe> getNoes() {
        return noes;
    }
}
