package org.nmrfx.processor.gui;

import javafx.collections.ListChangeListener;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.peaks.*;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.structure.chemistry.search.MNode;
import org.nmrfx.structure.chemistry.search.MTree;
import org.nmrfx.utils.GUIUtils;

import java.util.*;


public class ManagedList extends PeakList {
    private LabelDataset labelDataset;
    //SNR required for picking peak - useful when adding breakthrough labeling percentages
    private static double detectionLimit =3;
    private Double noise;
    private double pickThreshold=0.0;
    //TODO: persist across saves - have to think of how to do for sample, experiment, acquisition anyway
    private HashMap<ExpDim,Integer> dimMap = new HashMap<>();
    private Acquisition acquisition;
    private int ppmSet;
    private int rPpmSet;
    //probably don't need this type - just the noeSet which can be passed by managedListSetup
    public Connectivity.NOETYPE noeType;
    public NoeSet noeSet;
    private MTree peakListTree;
    private Noe addedNoe=null;
    private ManagedPeak addedPeak=null;

    public ManagedList(Acquisition acquisition, String name, int ppmSet, int rPpmset,Connectivity.NOETYPE noeType) {
        //might regret this! Adding one peakdim for every expDim - whether obs or not. Can update PeakDim with flag, and update GUI code to take into account.
        //but probably Bruce will reject this idea!
        //super(name,acquisition.getExperiment().getSize());
        //no - impliment this in managedpeak - add nonObsPeakDim or something
        super(name, acquisition.getDataset().getNDim());
        this.setSampleConditionLabel(acquisition.getSample().getCondition().toString());
        this.setSlideable(true);
        this.acquisition = acquisition;
        this.ppmSet = ppmSet;
        this.rPpmSet = rPpmset;
        this.noeType = noeType;
        this.noise = acquisition.getDataset().guessNoiseLevel();
        //fixme - implement expDim mapping (during acquisition setup - popup if not clear on experiment choice)
        int i = 0;
        for (ExpDim expDim : acquisition.getExperiment().obsDims) {
            dimMap.put(expDim, i);
            i++;
        }
        this.pickThreshold = noise * detectionLimit;
        initPeakListTree();
        noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
            for (Noe addedNoe : c.getAddedSubList()) {
                addNoeToTree(addedNoe);
            }
        });
    }

    public ManagedList(LabelDataset labelDataset) {
        super(labelDataset.getManagedListName(),labelDataset.getDataset().getNDim());
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
        this.noise=labelDataset.getDataset().guessNoiseLevel();
        this.pickThreshold=noise*detectionLimit;
    }
    public ManagedList(LabelDataset labelDataset, int n) {
        super(labelDataset.getManagedListName(),n);
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
        this.noise=labelDataset.getDataset().guessNoiseLevel();
        this.pickThreshold=noise*detectionLimit;
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
        this.noise=labelDataset.getDataset().guessNoiseLevel();
        this.pickThreshold=noise*detectionLimit;
    }

    @Override
    public ManagedPeak addPeak(Peak newPeak) {
        if (noeType==null) {
            GUIUtils.warn("Cannot add peak","Peak patterns are determined by sample and experiment type only.");
            return null;
        }
        //fixme: what about when getNewPeak called? need to be careful

        //Strategy: add NOE with this peak and relevant peakDims and add to NOESet. Have listener that check whether the peak that added is from this peaklist, if not
        //then add through addNoePeak

        //All NOEs needed (even if not observed) have to be set up. Can still add the first peak even if it doesn't match assignments.

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
        //TODO: Fix assignments for non NOE connected dims?
        if (showPicker) {
            PeakAtomPicker peakAtomPicker = new PeakAtomPicker();
            peakAtomPicker.create();
            peakAtomPicker.showAndWait(300, 300, newPeak);
            //peakpicker doAssign() only sets labels - possible fixme
            for (PeakDim peakDim : newPeak.getPeakDims()) {
                resonance=(AtomResonance) peakDim.getResonance();
                resonance.setAtom(acquisition.getSample().getMolecule().findAtom(peakDim.getLabel()));
            }
        }

        float percent=acquisition.getPeakPercent(newPeak);
        //don't repick existing peaks
        if (percent>0) {
            //Not going to use the originally picked peak
            this.idLast--;
            acquisition.addNoes(this,newPeak);
            ManagedPeak returnPeak=addedPeak;
            addedPeak=null;
            return returnPeak;
            /*ManagedPeak manPeak=new ManagedPeak(this,newPeak);

            peaks().add(manPeak);
            //add diagonal
            ManagedPeak dpeak = new ManagedPeak(this, nDim);
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
             */
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
        if (noise==null) {
            active=true;
        } else if (new_intensity> detectionLimit *noise) {
            //Need to watch peak intensity changes to update I guess
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
        detailArray.add("PPM Set: "+ppmSet);
        detailArray.add("rPPM Set: "+rPpmSet);
        if (noeType!=null) {
            detailArray.add("NOES "+noeType);
        }
        return detailArray;
    }

    public MTree getPeakListTree () {
        //acquisition tree nodes are weighted according to the label scheme of the sample
        if (peakListTree !=null) {
            return peakListTree;
        } else {
            initPeakListTree();
            return peakListTree;
        }
    }

    private List<ManagedPeak> initPeakListTree () {
        //may have multiple peaklists per acquisition (normally not I expect).
        peakListTree=acquisition.getAcquisitionTree().copy();
        List<ManagedPeak> addedPeaks=new ArrayList<>();
        //Need to update NOE dims from NOE set
        if (noeSet!=null) {
            for (Noe noe : noeSet.get()) {
                addedPeaks.addAll(addNoeToTree(noe));
            }
        } else {
            addedPeaks.addAll(addPeaksDepthFirst(acquisition.getDimNodeMap(),null,null,new HashMap<>(),new HashSet<>(),1.0,new ArrayList<>(),null,null,true));
        }
        return addedPeaks;
    }

    private List<ManagedPeak> addNoeToTree(Noe noe) {
        //fixme: this is not appropriate for SpatialSetGroups with more than one atom (i.e. ambiguous peak assignments)
        List<ManagedPeak> addedPeaks=new ArrayList<>();
        if (peakListTree==null) {
            return initPeakListTree();
            //added noe is already in noeSet, and so has been added by above. This should never be reached!
        }
        HashMap<ExpDim,Set<MNode>> localDimNodeMap = new HashMap<>();
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            if (expDim.getNextCon().type==Connectivity.TYPE.NOE) {
                Set<MNode> nodeSet = new HashSet<>();
                localDimNodeMap.put(expDim, nodeSet);

                Set<MNode> nodeSet2 = new HashSet<>();
                localDimNodeMap.put(expDim.getNextExpDim(), nodeSet2);

                for (ExpDim otherExpDim : acquisition.getExperiment().expDims) {
                    if (otherExpDim!=expDim && otherExpDim!=expDim.getNextExpDim()) {
                        //take care not to modify this set
                        localDimNodeMap.put(otherExpDim,acquisition.getDimNodeMap().get(otherExpDim));
                    }
                }

                double intensity = noe.getIntensity();
                MNode mNode = acquisition.getDimAtomNodeMap().get(expDim).get(noe.spg1.getAnAtom());
                MNode mNode2 = acquisition.getDimAtomNodeMap().get(expDim.getNextExpDim()).get(noe.spg2.getAnAtom());
                //add edge weighted by intensity and labelling
                if (mNode != null && mNode2 != null) {
                    //check if already connected
                    if (!mNode.forwardWeightedEdges.containsKey(mNode2)) {
                        mNode.forwardWeightedEdges.put(mNode2, intensity * acquisition.getSample().getAtomFraction(mNode.getAtom()));
                        nodeSet.add(mNode);
                        nodeSet2.add(mNode2);
                    }
                }
                mNode = acquisition.getDimAtomNodeMap().get(expDim).get(noe.spg2.getAnAtom());
                mNode2 = acquisition.getDimAtomNodeMap().get(expDim.getNextExpDim()).get(noe.spg1.getAnAtom());
                //add edge weighted by intensity and labelling
                if (mNode != null && mNode2 != null) {
                    if (!mNode.forwardWeightedEdges.containsKey(mNode2)) {
                        mNode.forwardWeightedEdges.put(mNode2, intensity * acquisition.getSample().getAtomFraction(mNode.getAtom()));
                        nodeSet.add(mNode);
                        nodeSet2.add(mNode2);
                    }
                }
                //add peak based on localDimNodeMap
                addedPeaks.addAll(addPeaksDepthFirst(localDimNodeMap,acquisition.getFirstNode(),null,new HashMap<>(),new HashSet<>(),1.0,new ArrayList<>(),null,null,true));

                localDimNodeMap.clear();
            }
        }
        if (noe==addedNoe) {
            addedPeak=addedPeaks.get(addedPeaks.size()-1);
            addedNoe=null;
        }
        return addedPeaks;
    }

    private List<ManagedPeak> addPeaksDepthFirst(HashMap<ExpDim, Set<MNode>> dimNodeMap, MNode node, ExpDim expDim, HashMap<Integer,Atom> atomMap, Set<Noe> noeArray, double peakIntensity, List<ManagedPeak> addedPeaks,MNode startNode,ExpDim startDim,boolean forward) {
        if (peakIntensity<pickThreshold) {
            return addedPeaks;
        }
        if (startNode==null) {
            //initial setup
            ExpDim smallestDim=null;
            int min=Integer.MAX_VALUE;
            for (ExpDim testExpDim:acquisition.getExperiment().expDims) {
                int size=dimNodeMap.get(testExpDim).size();
                if (size<min) {
                    smallestDim=testExpDim;
                    min=size;
                }
            }
            for (MNode startingNode : dimNodeMap.get(smallestDim)) {
                addPeaksDepthFirst(dimNodeMap, startingNode, smallestDim, new HashMap<>(), new HashSet<>(), 1.0, addedPeaks, startingNode,smallestDim,forward);
            }
            return addedPeaks;
        }
        if (expDim!=null) {
            if (expDim.isObserved()) {
                atomMap.put(dimMap.get(expDim),node.getAtom());
            }
        }
        HashMap<MNode,Double> edges;
        if (forward==true) {
            edges=node.forwardWeightedEdges;
        } else {
            edges=node.backwardWeightedEdges;
        }
        for (MNode nextNode : edges.keySet()) {
            if (nextNode==acquisition.getFirstNode()) {
                //we've completed the loop, check and pick the peak!
                if (atomMap.size()!=this.nDim) {
                    System.out.println("Unexpected error adding peak to "+this.getName()+": "+atomMap);
                    return addedPeaks;
                }

                ManagedPeak newPeak=new ManagedPeak(this,this.nDim,noeArray);
                newPeak.setIntensity((float) peakIntensity);
                for (int i=0;i<this.nDim;i++) {
                    ((AtomResonance) newPeak.getPeakDim(i).getResonance()).setAtom(atomMap.get(i));
                }
                addedPeaks.add(newPeak);
                return addedPeaks;
            }

            ExpDim nextExpDim;
            if (nextNode==acquisition.getLastNode()) {
                peakIntensity*=edges.get(acquisition.getLastNode());
                nextNode=startNode;
                nextExpDim=startDim;
                addPeaksDepthFirst(dimNodeMap, nextNode, nextExpDim, atomMap, noeArray, peakIntensity, addedPeaks, startNode, startDim, false);
            } else {
                nextExpDim = expDim.getNextExpDim(forward);
                HashMap<Integer,Atom> nextAtomMap=new HashMap<>();
                for (int i : atomMap.keySet()) {
                    nextAtomMap.put(i,atomMap.get(i));
                }
                if (dimNodeMap.get(nextExpDim).contains(nextNode)) {
                    double nextPeakIntensity = peakIntensity * node.forwardWeightedEdges.get(nextNode);
                    if (expDim.getNextCon(forward).type == Connectivity.TYPE.NOE) {
                        for (Noe noe : noeSet.getConstraints(node.getAtom() + " " + nextNode.getAtom(), true)) {
                            Set<Noe> nextNoeArray = new HashSet<>();
                            nextNoeArray.addAll(noeArray);
                            addPeaksDepthFirst(dimNodeMap, nextNode, nextExpDim, nextAtomMap, nextNoeArray, nextPeakIntensity, addedPeaks, startNode, startDim, forward);
                        }
                    } else {
                        addPeaksDepthFirst(dimNodeMap, nextNode, nextExpDim, nextAtomMap, noeArray, nextPeakIntensity, addedPeaks, startNode, startDim, forward);
                    }
                }
            }
        }
        return addedPeaks;
    }

    public HashMap<ExpDim, Integer> getDimMap() {
        return dimMap;
    }

    public void setAddedNoe(Noe addedNoe) {
        this.addedNoe = addedNoe;
    }
}
