package org.nmrfx.processor.gui;

import javafx.collections.ListChangeListener;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.processor.datasets.peaks.*;

import org.nmrfx.project.UmbcProject;
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
    private double detectionLimit=3;
    private Double noise;
    //private Double highestSignal;
    private double pickThreshold() {
        return detectionLimit*noise;//highestSignal;
    }
    //TODO: persist across saves - have to think of how to do for sample, experiment, acquisition anyway
    private HashMap<ExpDim,Integer> dimMap = new HashMap<>();
    private Acquisition acquisition;
    private int ppmSet;
    private int rPpmSet;
    //probably don't need this type - just the noeSet which can be passed by managedListSetup
    public Connectivity.NOETYPE noeType;
    public NoeSet noeSet;
    private ExperimentTree peakListTree;
    private Noe addedNoe=null;
    //private ManagedPeak addedPeak=null;

    public ManagedList(Acquisition acquisition, String name, int ppmSet, int rPpmset,Connectivity.NOETYPE noeType) {
        super(name, acquisition.getDataset().getNDim());
        this.setSampleConditionLabel(acquisition.getSample().getCondition().toString());
        this.setSlideable(true);
        this.acquisition = acquisition;
        this.ppmSet = ppmSet;
        this.rPpmSet = rPpmset;
        this.noeType = noeType;
        if (this.noeType!=null) {
            noeSet= UmbcProject.getNoeSet(acquisition.getSample().getMolecule(),noeType,ppmSet);
        }
        this.noise = acquisition.getDataset().guessNoiseLevel();
        //this.highestSignal=acquisition.getDataset().extremeValue();
        //fixme - implement expDim mapping (during acquisition setup - popup if not clear on experiment choice)
        int i = 0;
        for (ExpDim expDim : acquisition.getExperiment().obsDims) {
            dimMap.put(expDim, i);
            i++;
        }
        initializeList(acquisition.getDataset());
        initPeakListTree();
        if (noeSet!=null) {
            noeSet.get().addListener((ListChangeListener.Change<? extends Noe> c) -> {
                while (c.next()) {
                    for (Noe addedNoe : c.getAddedSubList()) {
                        addNoeToTree(addedNoe,true);
                    }
                }
            });
        }
    }

    public ManagedList(LabelDataset labelDataset) {
        super(labelDataset.getManagedListName(),labelDataset.getDataset().getNDim());
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
        this.noise=labelDataset.getDataset().guessNoiseLevel();
        //this.highestSignal=labelDataset.getDataset().extremeValue();
    }
    public ManagedList(LabelDataset labelDataset, int n) {
        super(labelDataset.getManagedListName(),n);
        this.labelDataset=labelDataset;
        this.setSampleConditionLabel(labelDataset.getCondition());
        this.setSlideable(true);
        this.noise=labelDataset.getDataset().guessNoiseLevel();
        //this.highestSignal=labelDataset.getDataset().extremeValue();
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
        //this.highestSignal=labelDataset.getDataset().extremeValue();
    }

    public void initializeList(Dataset dataset) {
        if (dataset!=null) {
            this.fileName = dataset.getFileName();
            for (int i = 0; i < dataset.getNDim(); i++) {
                int dDim = i;
                SpectralDim sDim = getSpectralDim(i);
                sDim.setDimName(dataset.getLabel(dDim));
                sDim.setSf(dataset.getSf(dDim));
                sDim.setSw(dataset.getSw(dDim));
                sDim.setSize(dataset.getSize(dDim));
                double minTol = Math.round(100 * 2.0 * dataset.getSw(dDim) / dataset.getSf(dDim) / dataset.getSize(dDim)) / 100.0;
                double tol = minTol;
                Nuclei nuc = dataset.getNucleus(dDim);
                if (null != nuc) {
                    switch (nuc) {
                        case H1:
                            tol = 0.05;
                            break;
                        case C13:
                            tol = 0.6;
                            break;
                        case N15:
                            tol = 0.2;
                            break;
                        default:
                            tol = minTol;
                    }
                }
                tol = Math.min(tol, minTol);

                sDim.setIdTol(tol);
                sDim.setDataDim(dDim);
                sDim.setNucleus(dataset.getNucleus(dDim).getNumberName());
            }
        }
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
        //TODO: This should really only ask for NOE assignments - other dims are fixed by acquisition params.
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
        //TODO: repick from existing NOEs taking new detection limit into account
        if (newPeak.getIntensity()<pickThreshold()) {
            detectionLimit=0.9*newPeak.getIntensity(); //*highestSignal/noise;
        }
        List<ManagedPeak> addedPeaks = acquisition.addNoes(this,newPeak);
        if (addedPeaks.size()>0) {
            this.idLast--;
            return addedPeaks.get(addedPeaks.size()-1);
        } else {
            //fixme: risk of idLast getting out of sync here - sometimes return value ignored
            return null;
        }
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

    public ExperimentTree getPeakListTree () {
        if (peakListTree == null) {
            initPeakListTree();
        }
        return peakListTree;
    }

    private List<ManagedPeak> initPeakListTree () {
        //may have multiple NOEsets per acquisition (normally not I expect).
        peakListTree=acquisition.getAcquisitionTree().copy();
        //need a local mNode Map!! Otherwise adding edges for all peaklists.
        List<ManagedPeak> addedPeaks=new ArrayList<>();
        //Need to update NOE dims from NOE set
        if (noeSet!=null) {
            for (Noe noe : noeSet.get()) {
                addedPeaks.addAll(addNoeToTree(noe));
            }
        } else {
            addedPeaks.addAll(addPeaksMiddleOut(acquisition.getDimNodeMap(),null,null,new HashMap<>(),new HashSet<>(),null,new ArrayList<>(),null,null,true));
        }
        return addedPeaks;
    }

    private void addNoeToTree(Noe noe,boolean checkNoe) {
        if (checkNoe && noe==addedNoe) {
            addedNoe=null;
            return;
        }
        addNoeToTree(noe);
        return;
    }

    public List<ManagedPeak> addNoeToTree(Noe noe) {
        List<ManagedPeak> addedPeaks=new ArrayList<>();
        if (peakListTree==null) {
            return initPeakListTree();
            //added noe is already in noeSet, and so has been added by above. This should never be reached!
        }
        HashMap<ExpDim,Set<MNode>> localDimNodeMap = new HashMap<>();
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            if (expDim.getNextCon()!=null && (expDim.getNextCon().type==Connectivity.TYPE.NOE)) {
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
                //fixme: this is not appropriate for SpatialSetGroups with more than one atom (i.e. ambiguous peak assignments)
                Atom atom1=noe.spg1.getAnAtom();
                Atom atom2=noe.spg2.getAnAtom();
                double intensity = noe.getIntensity()/noe.getScale();
                //fixme: AWOOGA. Adding edges to acquisitionTree. May as well not have a peakListTree! Need a copy function. But then need a node node mapping
                // for adding resonances.... Maybe just have to say one peaklist type per acquisition :-( (but can still have multiple ppm sets)
                MNode mNode = acquisition.getDimAtomNodeMap().get(expDim).get(atom1);
                MNode mNode2 = acquisition.getDimAtomNodeMap().get(expDim.getNextExpDim()).get(atom2);
                //add edge weighted by intensity and labelling
                if (mNode != null && mNode2 != null) {
                    //check if already connected
                    if (!mNode.forwardWeightedEdges.containsKey(mNode2)) {
                        mNode.forwardWeightedEdges.put(mNode2, intensity * acquisition.getSample().getAtomFraction(mNode.getAtom()));
                        mNode2.backwardWeightedEdges.put(mNode, intensity * acquisition.getSample().getAtomFraction(mNode.getAtom()));
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
                        mNode2.backwardWeightedEdges.put(mNode, intensity * acquisition.getSample().getAtomFraction(mNode.getAtom()));
                        nodeSet.add(mNode);
                        nodeSet2.add(mNode2);
                    }
                }
                //add peak based on localDimNodeMap
                addedPeaks.addAll(addPeaksMiddleOut(localDimNodeMap,acquisition.getFirstNode(),null,new HashMap<>(),new HashSet<>(),null,new ArrayList<>(),null,null,true));

                localDimNodeMap.clear();
            }
        }
        return addedPeaks;
    }

    private List<ManagedPeak> addPeaksMiddleOut(HashMap<ExpDim, Set<MNode>> dimNodeMap, MNode node, ExpDim expDim, HashMap<Integer,PeakDim> peakDimMap, Set<Noe> noeArray, Double peakIntensity, List<ManagedPeak> addedPeaks, MNode startNode, ExpDim startDim, boolean forward) {
        //TODO: deal with pick intensity more carefully
        if (peakIntensity!=null && peakIntensity<pickThreshold()) {
            return addedPeaks;
        }
        //if I had a reverse nodeDimMap wouldn't need to keep track of expDims...
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
                addPeaksMiddleOut(dimNodeMap, startingNode, smallestDim, new HashMap<>(), new HashSet<>(), peakIntensity, addedPeaks, startingNode,smallestDim,forward);
            }
            return addedPeaks;
        }
        if (expDim!=null) {
            if (expDim.isObserved()) {
                //TODO: consider should this be restricted per ppmSet?
                if (!node.ppmSetPeakDimMap.containsKey(ppmSet)) {
                    return addedPeaks;
                }
                peakDimMap.put(dimMap.get(expDim), node.ppmSetPeakDimMap.get(ppmSet));
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
                if (peakDimMap.size()!=this.nDim) {
                    System.out.println("Unexpected error adding peak to "+this.getName());
                    return addedPeaks;
                }

                ManagedPeak newPeak=new ManagedPeak(this,this.nDim,noeArray,peakDimMap);
                peaks().add(newPeak);
                this.reIndex();
                newPeak.setIntensity(peakIntensity.floatValue());
                addedPeaks.add(newPeak);
                return addedPeaks;
            }
            if (peakIntensity==null) {
                peakIntensity=1.0;
            }
            ExpDim nextExpDim;
            if (nextNode==acquisition.getLastNode()) {
                peakIntensity*=edges.get(acquisition.getLastNode());
                nextNode=startNode;
                nextExpDim=startDim;
                addPeaksMiddleOut(dimNodeMap, nextNode, nextExpDim, peakDimMap, noeArray, peakIntensity, addedPeaks, startNode, startDim, false);
            } else {
                nextExpDim = expDim.getNextExpDim(forward);
                HashMap<Integer,PeakDim> nextPeakDimMap=new HashMap<>();
                for (int i : peakDimMap.keySet()) {
                    nextPeakDimMap.put(i,peakDimMap.get(i));
                }
                if (dimNodeMap.get(nextExpDim).contains(nextNode)) {
                    double nextPeakIntensity = peakIntensity * node.forwardWeightedEdges.get(nextNode);
                    if (expDim.getNextCon(forward).type == Connectivity.TYPE.NOE) {
                        for (Noe noe : noeSet.getConstraints(node.getAtom() + " " + nextNode.getAtom(), true)) {
                            Set<Noe> nextNoeArray = new HashSet<>();
                            nextNoeArray.addAll(noeArray);
                            nextNoeArray.add(noe);
                            addPeaksMiddleOut(dimNodeMap, nextNode, nextExpDim, nextPeakDimMap, nextNoeArray, nextPeakIntensity, addedPeaks, startNode, startDim, forward);
                        }
                    } else {
                        addPeaksMiddleOut(dimNodeMap, nextNode, nextExpDim, nextPeakDimMap, noeArray, nextPeakIntensity, addedPeaks, startNode, startDim, forward);
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

    public int getPpmSet() {
        return ppmSet;
    }

    public int getrPpmSet() {
        return rPpmSet;
    }
}
