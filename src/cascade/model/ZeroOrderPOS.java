package cascade.model;

import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Pattern;


import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import cascade.features.FeatureGenerator;
import cascade.features.FeatureVector;
import cascade.features.Weights;
import cascade.features.ZeroOrderPOSFeatures;
import cascade.io.Corpus;
import cascade.io.SentenceInstance;
import cascade.io.Sequence;
import cascade.lattice.Lattice;
import cascade.lattice.ZeroOrderLattice;
import cascade.learn.FilterTradeoffStatistics;
import cascade.learn.GeneralizationStatistics;
import cascade.learn.LossFunctions;
import cascade.programs.Options;
import cascade.util.Alphabet;
import cascade.util.ArrayUtil;
import cascade.util.RunTimeEstimator;

/**
 * Instantiation of the ZeroOrder model for POS tagging but applicable to any multi-class problem
 * with a fixed set of possible labels.
 * 
 * If it is used as a higher order model (to expand or generate anything besides the initial lattice)
 * it will return RuntimeExceptions to avoid bugs.
 * 
 */

public class ZeroOrderPOS extends ZeroOrderModel implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// FIXME: this doesn't seem to be used anywhere.. maybe it should go away?
	private int maxPrefixLength = 4; 
	public boolean useCoarseTags = false;

	public Alphabet POSAlphabet;

	// temporary buffers to avoid re-allocation:
	double [] scores = null; // scores / marginals buffer
	boolean [] mask = null;
	
	public void init(Options opts){
		super.init(opts);
		
		Corpus c = opts.corpus;
		POSAlphabet = new Alphabet();
		featureAlphabet = new Alphabet();

		if (featureGen == null)
			featureGen = new ZeroOrderPOSFeatures();
		
		featureGen.setComputeOnly(true);

		System.out.println("computing features on " + c.train[c.train.length-1].length + " examples");
		for(Sequence s: c.train[c.train.length-1]){
		  
			String[] tags = (useCoarseTags ? s.getInstance().cpostags :
					 s.getInstance().postags);

			for (int i = 0; i < (tags.length-1); i++) {
				POSAlphabet.lookupIndex(tags[i+1]);	
			}
			String [] words = s.getInstance().forms;
			
			for (int pos = 0; pos < (words.length-1); pos++) {
				featureGen.computePositionFeatures(this, s.getInstance(), pos);
			}
		}
		POSAlphabet.stopGrowth();
		featureAlphabet.stopGrowth();	
		
		System.out.println("Model " + this.getClass().getCanonicalName() + " initialized:");
		System.out.println("Tags: " + POSAlphabet.size() + " Features: " + featureAlphabet.size() + " Total Features: " + POSAlphabet.size()*featureAlphabet.size());
	}
	
	public int getNumberOfStates(Sequence seq, int pos) {
		return POSAlphabet.size();
	}

	
	@Override
	public Lattice createLattice(Sequence seq) {
		Lattice l = new ZeroOrderLattice(seq, this);
		l.fv = getPositionFeatures(l);
		
		return l;
	}

	@Override
	public Lattice expandLattice(Lattice lattice, boolean [] mask) {
		throw new UnsupportedOperationException("This is a bottom level model, and can't expand existing lattices");
	}

	@Override
	public FeatureVector[] getEdgeFeatures(Lattice lattice) {
		throw new UnsupportedOperationException("This is a bottom level model, and cannot make edge features");
	}

	@Override
	public FeatureVector[] getStateFeatures(Lattice lattice) {
		throw new UnsupportedOperationException("This model does not support state features");
		
//		featureGen.setComputeOnly(false);
//		
//		FeatureVector [] fv = new FeatureVector[lattice.getNumStates()];
//		
//		DependencyInstance inst = lattice.seq.getInstance();
//		
//		for (int pos = 0; pos < lattice.length(); pos++){
//			int start = lattice.getStateOffset(pos);
//			int end = lattice.getStateOffset(pos+1);
//			
//			for (int idx = start; idx < end; idx++) {
//				
//				featureGen.computeFeatures(this, inst, pos, lattice.getStateID(idx));
//				fv[idx] = featureGen.finalizeFeatureVector();
//				
//			}
//			
//		}
//		return fv;
	}

	/** 
	 * This cuts off the first state ("root") which is not necessary to include in the model.
	 */
	@Override
	public int[] getTruth(Sequence seq) {

		int truth [] = new int[seq.length()];
		
		String[] tags = (useCoarseTags ? seq.getInstance().cpostags : seq.getInstance().postags);
		
		for (int i = 1; i < tags.length; i++) {
			truth[i-1] = POSAlphabet.lookupIndex(tags[i]);
		}
		
		return truth;
	}

	@Override
	public String stateToString(Lattice l, int state) {
		return POSAlphabet.reverseLookup(state); 
	}

	@Override
	public void addGeneralizationStats(Lattice lattice, Weights w,
			GeneralizationStatistics stats, double alpha) {
		
		// tally lattice statistics
		stats.numSequences++;
		
		stats.numPositions += lattice.length();
		stats.numStates += lattice.getNumStates();
		stats.numPossibleStates += POSAlphabet.size()*lattice.length();				
		
		long startTime = 0;
		
		// score lattice
		scores = ArrayUtil.ensureCapacity(scores, lattice.getNumStates());
		lattice.stateScores = scores;
		scoreLatticeStates(w, lattice);
		
		stats.avgTestTime += (System.nanoTime()-startTime)/1e6;
		
		int [] labels = getTruth(lattice.seq);
					
		// tally classification and pruning errors
		int [] truthIdx = lattice.findStateIdx(labels);
		
		// compute flat classifier mistakes
		double mistakes = LossFunctions.computeFlatClassifierError(lattice, truthIdx);
		stats.numSequenceMistakes += mistakes > 0 ? 1 : 0;
		stats.totalClassError += mistakes;
		
		double [] mean = new double[lattice.length()];
		double [] max = new double[lattice.length()];

		// compute mean and max at each position
		lattice.computePerPositionStateMeanMax(mean, max);
		
		// comput pruning thresholds at each position
		double [] thresholds = new double[lattice.length()];
		for (int pos = 0; pos < max.length; pos++) 
			thresholds[pos] = (1-alpha)*mean[pos] + alpha*max[pos];

		stats.totalPruneEff += LossFunctions.computeStateEfficiencyLoss(lattice, lattice.stateScores, thresholds);
		
		double pruningMistakes = LossFunctions.computeFilterLoss(lattice, lattice.stateScores, thresholds, truthIdx);
		stats.numSequencePruneMistakes += (pruningMistakes > 0) ? 1 : 0;
		stats.totalPruneError += pruningMistakes;
	}

	@Override
	public void addTradeoffStats(Lattice lattice, Weights w,
			FilterTradeoffStatistics stats) {

		int [] labels = lattice.model.getTruth(lattice.seq);
		int [] truthIdx = lattice.findStateIdx(labels);
		
		double scores [] = lattice.stateScores;

		// compute mean and max at each position		
		double [] mean = new double[lattice.length()];
		double [] max = new double[lattice.length()];
		lattice.computePerPositionStateMeanMax(mean, max);
		
		double [] thresholds = new double[max.length];
		
		// for each alpha, recompute thresholds and then recompute efficiency etc.
		for (int i = 0; i < stats.alphas.length; i++) {
			double alpha = stats.alphas[i];
			
			for (int pos = 0; pos < thresholds.length; pos++) 
				thresholds[pos] = (1-alpha)*mean[pos] + alpha*max[pos];

			stats.effs[i] += LossFunctions.computeStateEfficiencyLoss(lattice, scores, thresholds);
			stats.errs[i] += LossFunctions.computeFilterLoss(lattice, scores, thresholds, truthIdx);
		}
		
		stats.numSequences++;
	}

	@Override
	public boolean[] computeFilterMask(Lattice lattice, Weights w,	double alpha, boolean isTraining) {

		mask = ArrayUtil.ensureCapacity(mask, lattice.getNumStates());
		scores = ArrayUtil.ensureCapacity(scores, lattice.getNumStates());
		lattice.stateScores = scores;
		
		// score every state in the lattice
		scoreLatticeStates(w, lattice);

		// compute mean and max at each position		
		double [] mean = new double[lattice.length()];
		double [] max = new double[lattice.length()];
		lattice.computePerPositionStateMeanMax(mean, max);

		int [] truth = getTruth(lattice.seq);
		
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			double threshold = (1-alpha)*mean[pos] + alpha*max[pos];
			for (int idx = start; idx < end; idx++) {
				mask[idx] = (scores[idx] > threshold);
				if (isTraining && (lattice.getStateID(idx) == truth[pos]))
					mask[idx] = true;
			}
		}

//		if (!isTraining) {
//			System.out.println("Mean: " + Arrays.toString(mean));
//			System.out.println("Max: " + Arrays.toString(max));
//			((ZeroOrderLattice)lattice).print(mask);
//		}
		return mask;
	}
	

	//@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);

		POSAlphabet = (Alphabet) in.readObject();
		featureAlphabet = (Alphabet) in.readObject();
		maxPrefixLength = in.readInt();
		
		featureGen = new ZeroOrderPOSFeatures();
		featureGen.setComputeOnly(true);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeObject(POSAlphabet);
		out.writeObject(featureAlphabet);
		out.writeInt(maxPrefixLength);

	}

	@Override
	public void generateValidStates(Lattice base, boolean[] mask,
			TIntArrayList newStateIDs, TDoubleArrayList newStateScores,
			int[] newStatePosOffsets) {
		throw new UnsupportedOperationException("This is a base model and does not expand lattices");
	}

	@Override
	public int[] getNextStates(Sequence seq, int pos, int state) {
		throw new UnsupportedOperationException("This is a base model does not have transitions");		
	}

	@Override
	public int[] getPreviousStates(Sequence seq, int pos, int state) {
		throw new UnsupportedOperationException("This is a base model does not have transitions");		
	}

	@Override
	public FeatureVector[] getPositionFeatures(Lattice lattice) {
		featureGen.setComputeOnly(false);
		
		FeatureVector [] fv = new FeatureVector[lattice.length()];
		
		SentenceInstance inst = lattice.seq.getInstance();
		
		for (int pos = 0; pos < lattice.length(); pos++){
			featureGen.computePositionFeatures(this, inst, pos);
			fv[pos] = featureGen.finalizeFeatureVector();
		}					

		return fv;
	}

	@Override
	public void scoreLatticeEdges(Weights w, Lattice lattice) {
		throw new UnsupportedOperationException("This is a base model does not have transitions");
	}

	@Override
	public void scoreLatticeStates(Weights w, Lattice lattice) {

		int N = featureAlphabet.size();
		
		for (int pos = 0; pos < lattice.length(); pos++){
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);
			for (int idx = start; idx < end; idx++) {

				int state = lattice.getStateID(idx);
				int offset = state*N;
				
				lattice.stateScores[idx] = w.score(lattice.fv[pos], offset);
			}
		}
	}

	@Override
	public void increment(Lattice lattice, int idx, Weights w, double rate) {
		
		int pos = lattice.findStatePosOffset(idx);
		
		w.increment(lattice.fv[pos], lattice.getStateID(idx)*featureAlphabet.size(), rate);
	}

	@Override
	public int getNumberOfFeatures() {
		return POSAlphabet.size()*featureAlphabet.size();
	}

	@Override
	public String toString() {
		return "Zero Order POS Model: " + POSAlphabet.size() + 
		" tags, " + featureAlphabet.size() + 
		" features, " + getNumberOfFeatures() + " total features";
	}

	int[] cacheOfPossibleStates;
	@Override
	public int[] possibleStates(Sequence seq, int position) {
		if(cacheOfPossibleStates==null || cacheOfPossibleStates.length!=getNumberOfStates(seq, position)){
			cacheOfPossibleStates=new int[getNumberOfStates(seq, position)];
			for (int i = 0; i < cacheOfPossibleStates.length; i++) {
				cacheOfPossibleStates[i] = i;
			}
		}
		return cacheOfPossibleStates;
	}
}


//public void init(Options opts){
//	super.init(opts);
//	
//	Corpus c = opts.corpus;
//	POSAlphabet = new Alphabet();
//	featureAlphabet = new Alphabet();
//
//	if (featureGen == null)
//		featureGen = new ZeroOrderPOSFeatures();
//	
//	featureGen.setComputeOnly(true);
//
//	System.out.println("computing features on " + c.train[c.train.length-1].length + " examples");
//	for(Sequence s: c.train[c.train.length-1]){
//	  
//	        String[] tags = (useCoarseTags ? s.getInstance().cpostags :
//				 s.getInstance().postags);
//
//		int [] truth = new int[tags.length];
//		
//		for (int i = 0; i < (tags.length-1); i++) {
//			truth[i] = POSAlphabet.lookupIndex(tags[i+1]);	
//		}
//		String [] words = s.getInstance().forms;
//		
//		
//		for (int pos = 0; pos < (words.length-1); pos++) {
//			featureGen.computeFeatures(s.getInstance(), pos, truth[pos]);
//		}
//	}
//	POSAlphabet.stopGrowth();
//	
//	
//	if (generateAllPossibleFeatures) {
//		System.out.println("# features: " + featureAlphabet.size());
//		System.out.println("Generating ALL POSSIBLE features:");
//		
//		Sequence [] train =c.train[c.train.length-1];
//		RunTimeEstimator est = new RunTimeEstimator(train.length, 0.1);
//		
//		for(Sequence s: train){
//		  String[] tags = (useCoarseTags ? s.getInstance().cpostags :
//				   s.getInstance().postags);
//
//			String [] words = s.getInstance().forms;
//			for (int pos = 0; pos < (words.length-1); pos++) {
//				for (int state = 0; state < POSAlphabet.size(); state++)
//					featureGen.computeFeatures(s.getInstance(), pos, state);
//			}				
//			if (est.report())
//				System.out.println("# features: " + featureAlphabet.size());
//		}
//	}
//	
//	featureAlphabet.stopGrowth();	
//	
//	System.out.println("Model " + this.getClass().getCanonicalName() + " initialized:");
//	System.out.println("Tags: " + POSAlphabet.size() + " Features: " + featureAlphabet.size());
//}
