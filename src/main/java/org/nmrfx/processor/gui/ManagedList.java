package org.nmrfx.processor.gui;

import javafx.collections.SetChangeListener;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.datasets.Nuclei;
import org.nmrfx.processor.datasets.peaks.*;

import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.constraints.Noe;
import org.nmrfx.structure.chemistry.constraints.NoeSet;
import org.nmrfx.utils.GUIUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

//todo set sample label
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
    public NoeSet noeSet=null;
    private Noe addedNoe=null;
    //private ManagedPeak addedPeak=null;

    public ManagedList(Acquisition acquisition, String name, int ppmSet, int rPpmset,NoeSet noeSet, HashMap<ExpDim,Integer> dimMap) {
        super(name, acquisition.getDataset().getNDim());
        this.setSampleConditionLabel(acquisition.getSample().getCondition().toString());
        this.setSlideable(true);
        this.acquisition = acquisition;
        this.ppmSet = ppmSet;
        this.rPpmSet = rPpmset;
        this.noise = acquisition.getDataset().guessNoiseLevel();
        //this.highestSignal=acquisition.getDataset().extremeValue();
        this.noeSet=noeSet;
        acquisition.getAcqTree().addNoeSet(noeSet);
        //fixme - implement expDim mapping (during acquisition setup - popup if not clear on experiment choice)
        int i = 0;
        this.dimMap=dimMap;
        initializeList(acquisition.getDataset());
        addPeaks();
        acquisition.getAcqTree().getEdges().addListener((SetChangeListener.Change<? extends AcqTree.Edge> c) -> {
            if (c.wasAdded()) {
                addEdgeToList(c.getElementAdded(),true);
            }
        });
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
    public ManagedPeak addPeak(Peak pickedPeak) {
        if (noeSet==null) {
            GUIUtils.warn("Cannot add peak","Peak patterns are determined by sample and experiment type only.");
            return null;
        }
        //fixme: what about when getNewPeak called? need to be careful

        //TODO: repick from existing NOEs taking new detection limit into account
        if (pickedPeak.getIntensity()<pickThreshold()) {
            detectionLimit=0.9*pickedPeak.getIntensity(); //*highestSignal/noise;
        }

        pickedPeak.initPeakDimContribs();
        /*
        Boolean showPicker=false;

        AtomResonance resonance;
        for (PeakDim peakDim : pickedPeak.getPeakDims()) {
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
            peakAtomPicker.showAndWait(300, 300, pickedPeak);
            //peakpicker doAssign() only sets labels - possible fixme
            for (PeakDim peakDim : pickedPeak.getPeakDims()) {
                resonance=(AtomResonance) peakDim.getResonance();
                Atom atom=acquisition.getSample().getMolecule().findAtom(peakDim.getLabel());
                resonance.setAtom(atom);
                atom.setResonance(resonance);
            }
        }
        List<ManagedPeak> addedPeaks = acquisition.addNoes(this,pickedPeak);
        */
        //Use AcqNodeChooser with picked peak
        AcqNodeChooser chooser = new AcqNodeChooser(this,pickedPeak);
        chooser.create();
        chooser.showAndWait(300,300);
        List<ManagedPeak> addedPeaks=new ArrayList<>();
        this.idLast--;
        if (addedNoe!=null) {
            addedPeaks.addAll(addNoeToList(addedNoe));
        }
        if (addedPeaks.size() > 0) {
            addedNoe.setPeak(addedPeaks.get(addedPeaks.size() - 1));
        } else {
            System.out.println("Error adding NOE "+addedNoe);
            noeSet.get().remove(addedNoe);
        }
        for (PeakDim peakDim : pickedPeak.peakDims) {
            peakDim.remove();
            if (peakDim.hasMultiplet()) {
                Multiplet multiplet = peakDim.getMultiplet();
            }
        }
        this.unLinkPeak(pickedPeak);
        pickedPeak.markDeleted();
        addedNoe=null;

        if (addedPeaks.size()>0) {
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
        detailArray.add(peaks().size()+" peaks");
        detailArray.add("rPPM Set: "+rPpmSet);
        if (noeSet!=null) {
            Optional<Map.Entry<String, NoeSet>> optionalEntry = acquisition.getProject().noeSetMap.entrySet().stream().filter(ap -> ap.getValue().equals(noeSet)).findFirst();
            if (optionalEntry.isPresent()) {
            detailArray.add("NOE Set: "+optionalEntry.get().getKey());
            }
        }
        return detailArray;
    }

    private void addEdgeToList(AcqTree.Edge edge, boolean check) {
        if (check && edge.noe==addedNoe) {
            return;
        }
        addPeaks(edge);
        return;
    }

    public List<ManagedPeak> addNoeToList(Noe noe) {
        List<ManagedPeak> addedPeaks = new ArrayList<>();
        for (AcqNode node : acquisition.getAcqTree().getNodes(noe.spg1.getAnAtom())) {
            for (AcqTree.Edge edge : node.getEdges(noeSet,noe)) {
                addedPeaks.addAll(addPeaks(edge));
            }
        }
        return addedPeaks;
    }

    private List<ManagedPeak> addPeaks() {
        return addPeaks(null);
    }
    private List<ManagedPeak> addPeaks(AcqTree.Edge firstEdge) {
        ArrayList<ManagedPeak> addedPeaks=new ArrayList<>();
        AcqNode startNode;
        if (firstEdge==null) {
            startNode=null;
        } else {
            startNode=firstEdge.getNode();
        }
        for (HashMap<ExpDim, AcqTree.Edge> path :
                acquisition.getAcqTree().getPathEdgesMiddleOut(firstEdge,true,startNode,startNode,new HashMap<>(),new ArrayList<>(), this.noeSet)) {
            addedPeaks.add(addPeakFromPath(path));
        }
        return addedPeaks;
    }

    private ManagedPeak addPeakFromPath(HashMap<ExpDim, AcqTree.Edge> path) {
        Double peakIntensity=1.0;
        Set<Noe> noes=new HashSet<>();
        HashMap<Integer,Atom> atoms=new HashMap<>();
        for (ExpDim expDim : acquisition.getExperiment().expDims) {
            AcqTree.Edge edge=path.get(expDim);
            peakIntensity*=edge.weight;
            if (expDim.isObserved()) {
                atoms.put(dimMap.get(expDim),edge.getNode().getAtom());
            }
            if (expDim.getNextCon()!=null && expDim.getNextCon().type==Connectivity.TYPE.NOE) {
                noes.add(edge.noe);
            }
        }
        ManagedPeak newPeak = new ManagedPeak(this, this.nDim, noes, atoms);
        peaks().add(newPeak);
        this.reIndex();
        newPeak.setIntensity(peakIntensity.floatValue());
        return newPeak;
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

    public int getRPpmSet() {
        return rPpmSet;
    }

    public Acquisition getAcquisition() {
        return acquisition;
    }

    public Noe getAddedNoe() {
        return addedNoe;
    }

    @Override
    public void writeSTAR3Header(FileWriter chan) throws IOException {
        char stringQuote = '"';
        chan.write("save_" + getName() + "\n");
        chan.write("_Spectral_peak_list.Sf_category                   ");
        chan.write("spectral_peak_list\n");
        chan.write("_Spectral_peak_list.Sf_framecode                  ");
        chan.write(getName() + "\n");
        chan.write("_Spectral_peak_list.ID                            ");
        chan.write(getId() + "\n");
        chan.write("_Spectral_peak_list.Data_file_name                ");
        chan.write(".\n");
        chan.write("_Spectral_peak_list.Sample_ID                     ");
        chan.write(acquisition.getSample().getId()+"\n");
        chan.write("_Spectral_peak_list.Sample_label                  ");
        if (getSampleLabel().length() != 0) {
            chan.write("$" + getSampleLabel() + "\n");
        } else {
            chan.write(".\n");
        }
        chan.write("_Spectral_peak_list.Sample_condition_list_ID      ");
        chan.write(acquisition.getSample().getCondition().getId()+"\n");
        chan.write("_Spectral_peak_list.Sample_condition_list_label   ");
        String sCond = getSampleConditionLabel();
        if ((sCond.length() != 0) && !sCond.equals(".")) {
            chan.write("$" + sCond + "\n");
        } else {
            chan.write(".\n");
        }
        chan.write("_Spectral_peak_list.Slidable                      ");
        String slidable = isSlideable() ? "yes" : "no";
        chan.write(slidable + "\n");

        chan.write("_Spectral_peak_list.Experiment_ID                 ");
        chan.write(".\n");
        chan.write("_Spectral_peak_list.Experiment_name               ");
        if (fileName.length() != 0) {
            chan.write("$" + fileName + "\n");
        } else {
            chan.write(".\n");
        }
        chan.write("_Spectral_peak_list.Experiment_type               ");
        chan.write("$" + acquisition.getExperiment().toString() + "\n");
        chan.write("_Spectral_peak_list.Experiment_class              ");
        chan.write("$" + acquisition.getExperiment().toCode() + "\n");
        chan.write("_Spectral_peak_list.Number_of_spectral_dimensions ");
        chan.write(String.valueOf(nDim) + "\n");
        chan.write("_Spectral_peak_list.Details                       ");
        if (getDetails().length() != 0) {
            chan.write(stringQuote + getDetails() + stringQuote + "\n");
        } else {
            chan.write(".\n");
        }
        chan.write("\n");
    }

}
