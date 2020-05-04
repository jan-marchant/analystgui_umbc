package org.nmrfx.processor.gui;

import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.*;

import org.nmrfx.processor.gui.spectra.PeakDisplayParameters;
import org.nmrfx.processor.gui.spectra.PeakListAttributes;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.utils.GUIUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ManagedList extends PeakList {
    private LabelDataset labelDataset;
    //SNR required for picking peak - useful when adding breakthrough labeling percentages
    private static double detectionLimit =3;

    public ManagedList(Acquisition acquisition, String name,int ppmSet,int rPpmset,Connectivity.NOETYPE noeType, String nBonds, String minTransfers, String maxTransfers) {
        super(name,acquisition.getDataset().getNDim());
    }

    public ManagedList(LabelDataset labelDataset) {
        super(labelDataset.getManagedListName(),labelDataset.getDataset().getNDim());
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
    }
    public ManagedList(LabelDataset labelDataset, int n) {
        super(labelDataset.getManagedListName(),n);
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
    }
    public ManagedList(LabelDataset labelDataset,PeakList peakList) {
        super(labelDataset.getManagedListName()+"temp",peakList.getNDim());

        this.searchDims.addAll(peakList.searchDims);
        this.fileName = peakList.fileName;
        this.scale = peakList.scale;
        this.setDetails(peakList.getDetails());
        this.setSampleLabel(peakList.getSampleLabel());
        //this.setSampleConditionLabel(peakList.getSampleConditionLabel());
        this.setSampleConditionLabel(labelDataset.getCondition());

        for (int i = 0; i < nDim; i++) {
            this.setSpectralDim(peakList.getSpectralDim(i).copy(this),i);
        }

        for (int i = 0; i < peakList.peaks().size(); i++) {
            Peak peak = peakList.peaks().get(i);
            ManagedPeak newPeak = new ManagedPeak(this,peak);
            peaks().add(newPeak);
        }
        this.idLast = peakList.idLast;
        this.reIndex();

        //update charts which match
        List<PolyChart> activeChartList = new ArrayList<>();

        for (PolyChart chart : PolyChart.CHARTS) {
            chart.getPeakListAttributes().forEach((peakListAttr) -> {
                if (peakListAttr.getPeakList()==peakList) {
                    activeChartList.add(chart);
                }
            });
        }

        peakList.remove();
        this.setName(labelDataset.getManagedListName());
        for (PolyChart chart : activeChartList) {
            chart.setupPeakListAttributes(this);
            //peakAttr.setLabelType(PeakDisplayParameters.LabelTypes.SglResidue);
        }
        this.labelDataset=labelDataset;
        this.setSlideable(true);
    }

    @Override
    public ManagedPeak addPeak(Peak newPeak) {
        //will this need to be a ManagedPeak class to keep proper track of modifications?
        //fixme: what about when getNewPeak called? need to be careful
        //TODO: consider behavior when Resonances are SimpleResonance instead of AtomResonance
        //fixme: Might be better to immediately pick master peak with a call to add peak - only adding exactly as picked, then amend this logic to pick on all non master lists
        // including diagonals, when they match

        newPeak.initPeakDimContribs();
        Boolean showPicker=false;
        AtomResonance resonance;
        for (PeakDim peakDim : newPeak.getPeakDims()) {
            resonance=(AtomResonance) peakDim.getResonance();
            if (resonance.getAtom()==null) {
                showPicker=true;
            }
        }
        //TODO: Check compatibility of Bruce's peak assigner - does it filter by labeling? I guess I filter next anyway?
        if (showPicker) {
            PeakAtomPicker peakAtomPicker = new PeakAtomPicker();
            peakAtomPicker.create();
            peakAtomPicker.showAndWait(300, 300, newPeak);
            //peakpicker doAssign() only sets labels - possible fixme
            for (PeakDim peakDim : newPeak.getPeakDims()) {
                resonance=(AtomResonance) peakDim.getResonance();
                //fixme: only considers active molecule
                resonance.setAtom(Molecule.getAtomByName(peakDim.getLabel()));
            }
        }

        float percent=labelDataset.getPeakPercent(newPeak);
        //don't repick existing peaks
        if (percent>0) {
            for (Peak peak : peaks()) {
                    int count=0;
                    for (int i=0;i<nDim;i++) {
                        if (((AtomResonance) newPeak.getPeakDim(i).getResonance()).getAtom()
                            ==((AtomResonance) peak.getPeakDim(i).getResonance()).getAtom()) {
                            count++;
                        }
                    }
                    if (count==nDim) {
                        //handle matching peak. for now just do nothing
                        //deleted peaks are a pain to keep in sync across multiple lists. So I will prevent this from being possible.
                        //peak deletion will immediately compress and degap all non master lists.
                        //For now the master list should retain its peak, but it will be deleted.
                        //if (peak.isDeleted()) {
                        //    peak.setStatus(0);
                        //    newPeak=peak;
                        //} else {
                            return null;
                        //}
                        //break;
                    }
            }
            //Not going to use the originally picked peak
            this.idLast--;
            ManagedPeak manPeak=new ManagedPeak(this,newPeak);

            peaks().add(manPeak);
            //add diagonal
            Boolean diag=false;
            ManagedPeak dpeak = new ManagedPeak(this, nDim);
            if ((nDim==2) &&
                    (((AtomResonance) manPeak.getPeakDim(0).getResonance()).getAtom().getElementNumber()==
                            ((AtomResonance) manPeak.getPeakDim(1).getResonance()).getAtom().getElementNumber()) &&
                    (((AtomResonance) manPeak.getPeakDim(0).getResonance()).getAtom()!=((AtomResonance) manPeak.getPeakDim(1).getResonance()).getAtom())
            ) {
                manPeak.copyTo(dpeak);
                dpeak.getPeakDim(1).setChemShiftValue(manPeak.getPeakDim(0).getChemShiftValue());
                dpeak.getPeakDim(0).setChemShiftValue(manPeak.getPeakDim(1).getChemShiftValue());
                dpeak.getPeakDim(1).setResonance(manPeak.getPeakDim(0).getResonance());
                dpeak.getPeakDim(0).setResonance(manPeak.getPeakDim(1).getResonance());
                manPeak.getPeakDim(0).getResonance().add(dpeak.getPeakDim(1));
                manPeak.getPeakDim(1).getResonance().add(dpeak.getPeakDim(0));
                peaks().add(dpeak);
                diag=true;
            }
            //copy to other appropriate lists - use relative weights to set peak size
            //filter peaks is simply just a copy to the master list!
            //though need to overload with handling for no assignments
            //could be a "skip asking" variable read before bringing up the atompicker
            if (LabelDataset.getMaster()!=labelDataset) {
                LabelDataset.getMasterList().addLinkedPeak(manPeak, percent);
                if (diag) {
                    LabelDataset.getMasterList().addLinkedPeak(dpeak, percent);
                }
            }
            for (LabelDataset ld : LabelDataset.labelDatasetTable) {
                if (ld!=labelDataset && ld.isActive()) {
                    ld.getManagedList().addLinkedPeak(manPeak, percent);
                    if (diag) {
                        ld.getManagedList().addLinkedPeak(dpeak, percent);
                    }
                }
            }
            this.reIndex();
            for (int i=0;i<newPeak.getPeakDims().length;i++) {
                manPeak.getPeakDim(i).setFrozen(newPeak.getPeakDim(i).isFrozen());
            }
            for (PolyChart chart : PolyChart.CHARTS) {
                chart.drawPeakLists(true);
                //chart.refresh();
            }
            return manPeak;
         }
        return null;
    }

    public void addLinkedPeak(Peak manPeak,float percent) {
        //TODO: add check for whether peak already exists
        //TODO: Add diagonal peak
        float intensity=manPeak.getIntensity();
        float new_percent=labelDataset.getPeakPercent(manPeak);
        //this doesn't account for spin diffusion - only for "breakthrough" peaks
        float new_intensity=new_percent*intensity/percent;
        Boolean active=false;
        Dataset ds=Dataset.getDataset(fileName);
        if (ds!=null) {
            Double noise=ds.guessNoiseLevel();
            if (noise==null) {
                active=true;
            } else if (new_intensity> detectionLimit *noise) {
                //Need to watch peak intensity changes to update I guess
                active=true;
            }
        } else {
            //also add if no dataset
            active=true;
        }
        if (active) {
            ManagedPeak newManPeak=new ManagedPeak(this,manPeak);
            newManPeak.setIntensity(new_intensity);
            //TODO: change bounds of newManPeak to reflect new intensity
            peaks().add(newManPeak);
            this.reIndex();
            //ensure resonances match
            //scale intensity
        }
    }
    public void deleteMatchingPeaks(Peak peak) {
        for (Peak matchingPeak : getMatchingPeaks(peak,false)) {
            System.out.println("Trying to remove"+matchingPeak.getName());
            for (PeakDim peakDim : matchingPeak.peakDims) {
                peakDim.remove();
                if (peakDim.hasMultiplet()) {
                    Multiplet multiplet = peakDim.getMultiplet();
                }
            }
            unLinkPeak(matchingPeak);
            matchingPeak.markDeleted();
            peaks().remove(matchingPeak);
        }
        reIndex();
    }

    public Set<Peak> getMatchingPeaks(Peak searchPeak, Boolean includeSelf) {
        Set<Peak> matchingPeaks;
        matchingPeaks = new HashSet<>();
        Set<Peak> matchOneDimPeaks;
        matchOneDimPeaks = new HashSet<>();
        Set<PeakDim> seenPeakDims;
        seenPeakDims = new HashSet<>();

        Boolean first=true;
        for (PeakDim peakDim : searchPeak.getPeakDims()) {
            for (PeakDim linkedPeakDim : peakDim.getLinkedPeakDims()) {
                //only delete matching peaks from this list
                if (linkedPeakDim.getPeakList()==this) {
                    //Only consider each peakDim once.
                    // If a peak has already matched this peakDim, don't include.
                    // Otherwise issues with diagonal peak matching.
                    // This is a bit naff. fixme
                    if (!seenPeakDims.contains(linkedPeakDim) && !matchOneDimPeaks.contains(linkedPeakDim.getPeak())) {
                        matchOneDimPeaks.add(linkedPeakDim.getPeak());
                        seenPeakDims.add(linkedPeakDim);
                    }
                }
            }
            if (first) {
                matchingPeaks.addAll(matchOneDimPeaks);
                matchOneDimPeaks.clear();
                first=false;
            } else {
                matchingPeaks.retainAll(matchOneDimPeaks);
                matchOneDimPeaks.clear();
            }
        }
        if (!includeSelf) {
            matchingPeaks.remove(searchPeak);
        }
        return matchingPeaks;
    }

    public ArrayList<String> getDetailText() {
        ArrayList<String> detailArray=new ArrayList<>();
        detailArray.add("PPM Set: ");
        detailArray.add("NOE Set: ");
        detailArray.add("transfers");
        return detailArray;
    }
}
