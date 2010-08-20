package cascade.model;

import gnu.trove.PrimeFinder;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import cascade.features.FeatureGenerator;
import cascade.features.FeatureVector;
import cascade.features.NGramFeatures;
import cascade.features.NGramPOSFeatures;
import cascade.features.Weights;
import cascade.io.Corpus;
import cascade.io.ObjectReader;
import cascade.io.Sequence;
import cascade.lattice.Lattice;
import cascade.lattice.Viterbi;
import cascade.lattice.ViterbiMaxSum;
import cascade.learn.FilterTradeoffStatistics;
import cascade.learn.GeneralizationStatistics;
import cascade.learn.LossFunctions;
import cascade.programs.Options;
import cascade.util.Alphabet;
import cascade.util.ArrayUtil;

/**
 * Instantiation of the NOrderModel based on POS tagging but applicable to 
 * any sequential prediction problem with a fixed set of labels.
 *
 */
public class NOrderPOS extends NOrderModel implements Externalizable {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 2L;
	
	/**
	 * Constant to represent the NULL tag, which is necessary to construct higher order Ngrams
	 */
	static public final String NULLTAG = "[NULLTAG]";

	/**
	 * Whether or not to compute only features that are supported by training data
	 */
	public boolean useSupportedFeaturesOnly = false;

	/**
	 * Whether or not to add the marginals of the previous model as features.
	 */
	public boolean addQuintileFeatures = true;
	
	/**
	 * @deprecated Uses the coarse tag set instead of the full tag set as labels
	 */
	public boolean useCoarseTags = false;
	
	/**
	 * Whether or not to compute per-position features (this leads to explosive feature growth)
	 */
	public boolean usePositionFeatures = false;
	
	/**
	 * The maximum number of possible conditional per-position features that will be stored in memory.
	 * If the # of possible of features exceeds this value, weight mixing via hashing is automatically enabled.  
	 */
	public int maxCapacity = 5000000;
	
	/**
	 * Flag that gets automatically set if the number of possible features exceeds the maximum capacity
	 */
	private boolean useMixing = false;
	
	/**
	 * Whether or not to use the mean-max threshold or plug in alpha itself as the threshold 
	 * (if using sum-product marginals).
	 */
	public boolean useAlphaAsThreshold;

	
	/**
	 * Alphabet to store mapping from state #'s to POS tags. 
	 */
	public Alphabet POSAlphabet;
	
	String margLeftString, margRightString;
	
	// buffer variables
	public Viterbi viterbi;
	public double betaVals[];
	public boolean mask[];

	
	@Override	
	public void addGeneralizationStats(Lattice lattice, Weights w,
			GeneralizationStatistics stats, double alpha) {
		
		// tally lattice statistics
		stats.numSequences++;
		
		stats.numPositions += lattice.length();
		stats.numStates += lattice.getNumStates();
		stats.numEdges += lattice.getNumEdges();
		stats.numPossibleStates += getNumberOfTags()*lattice.length();				
		
		// run all viterbi to compute marginals etc.
		stats.avgTestTime += computeEdgeMarginals(lattice, w);
		
		// tally classification results
		
		int [] guess = computeGuesses(lattice);
		int [] truth = getTruth(lattice.seq);
		
		double mistakes = 0;
		for (int i = 0; i < truth.length; i++) {
			if (truth[i] != guess[i])
				mistakes++;
		}
		stats.totalClassError += mistakes/guess.length;
		stats.numSequenceMistakes += mistakes > 0 ? 1 : 0;

		// compute mean and max
		lattice.edgeScores = marginalVals;
		lattice.computeEdgeMeanMax();
				
		// tally pruning results: use a single threshold
		double thresholds[] = new double[1];
		thresholds[0] = useAlphaAsThreshold ? alpha : ((1-alpha)*lattice.meanEdgeScore + alpha*lattice.maxEdgeScore);
		
		int [] truthEdgeIdx = lattice.findEdgeIdx(getTruthStates(lattice.seq));
		
		stats.baselineErr += LossFunctions.computeFilterLoss(lattice, truthEdgeIdx);
		stats.zeroBaselineErr += LossFunctions.computeZeroOrderFilterLoss(lattice);

		double pruneError = LossFunctions.computeFilterLoss(lattice, marginalVals, thresholds, truthEdgeIdx);		
		
		stats.totalPruneError += pruneError;
		stats.totalPruneZError += LossFunctions.computeZeroOrderEdgeFilterLoss(lattice, marginalVals, thresholds);		
		stats.totalPruneEff += LossFunctions.computeEdgeEfficiencyLoss(lattice, marginalVals, thresholds); 
		stats.totalPruneZEff += LossFunctions.computeZeroOrderEdgeEfficiencyLoss(lattice, marginalVals, thresholds);
		
		if (pruneError > 0) stats.numSequencePruneMistakes++;
	}
	
	@Override
	public void addTradeoffStats(Lattice lattice, Weights w,
			FilterTradeoffStatistics stats) {
		
		double thresholds[] = new double[1];

		int [] truthEdgeIdx = lattice.findEdgeIdx(getTruthStates(lattice.seq));
		
		stats.baselineErr += LossFunctions.computeFilterLoss(lattice, truthEdgeIdx);
		stats.zeroBaselineErr += LossFunctions.computeZeroOrderFilterLoss(lattice);
		stats.zeroBaselineEff += LossFunctions.computeZeroOrderEfficiencyLoss(lattice);
		
		// for each alpha, recompute thresholds and then recompute efficiency etc.
		for (int i = 0; i < stats.alphas.length; i++) {
			double alpha = stats.alphas[i];
		
			thresholds[0] = useAlphaAsThreshold ? alpha : ((1-alpha)*lattice.meanEdgeScore + alpha*lattice.maxEdgeScore);
			
			stats.effs[i] += LossFunctions.computeEdgeEfficiencyLoss(lattice, marginalVals, thresholds); 
			stats.errs[i] += LossFunctions.computeFilterLoss(lattice, marginalVals, thresholds, truthEdgeIdx);		
			stats.zeroEffs[i] += LossFunctions.computeZeroOrderEdgeEfficiencyLoss(lattice, marginalVals, thresholds); 
			stats.zeroErrs[i] += LossFunctions.computeZeroOrderEdgeFilterLoss(lattice, marginalVals, thresholds);
		}
		
		stats.numSequences++;
	}
	
	public int getNumberOfTags() { return POSAlphabet.size(); }

	@Override
	public int[] computeGuesses(Lattice lattice) {
		
		int [] states = lattice.getArgmaxStates(alphaArgs, marginalVals);
		for (int i = 0; i < states.length; i++)
			states[i] = computeTagFromNGramID(lattice, order, states[i], 0);
		
		return states;
	}
	
	@Override
	public double computeEdgeMarginals(Lattice lattice, Weights w) {
		long start = System.nanoTime();

		edgeScores = ArrayUtil.ensureCapacity(edgeScores, lattice.getNumEdges());
		marginalVals = ArrayUtil.ensureCapacity(marginalVals, lattice.getNumEdges());
		witnessCount = ArrayUtil.ensureCapacityReset(witnessCount, lattice.getNumEdges());
		
		alphaVals = ArrayUtil.ensureCapacity(alphaVals, lattice.getNumStates());
		betaVals = ArrayUtil.ensureCapacity(betaVals, lattice.getNumStates());
		alphaArgs = ArrayUtil.ensureCapacity(alphaArgs, lattice.getNumStates());
		betaArgs = ArrayUtil.ensureCapacity(betaArgs, lattice.getNumStates());
		
		lattice.edgeScores = edgeScores;
		this.scoreLatticeEdges(w, lattice);
		
		viterbi.computeAlpha(lattice, alphaVals, alphaArgs);
		viterbi.computeBeta(lattice, betaVals, betaArgs);
		viterbi.computeEdgeMarginals(lattice, alphaVals, betaVals, marginalVals);
		
		return (System.nanoTime()-start)/1e6;
	}
	
	@Override
	public Lattice createLattice(Sequence seq) {
		throw new UnsupportedOperationException("This is a higher order model that cannot create lattices from scratch");
	}

	
	@Override
	public FeatureVector[] getEdgeFeatures(Lattice lattice) {

		FeatureVector fv [] = new FeatureVector[lattice.getNumEdges()];
		
		double quintiles[][] = new double[lattice.length()][featureGen.getNumMarginalQuintiles()];
		
		// NB: loop using <=, not <
		for (int pos = 0; pos <= lattice.length(); pos++) {
			
			int start = lattice.getEdgeOffset(pos);
			int end = lattice.getEdgeOffset(pos+1);
			
			// compute marginal quintiles
			if (pos < lattice.length())
				featureGen.computeQuintiles(lattice.stateScores, lattice.getStateOffset(pos), lattice.getStateOffset(pos+1), quintiles[pos]);
			
			for (int idx = start; idx < end; idx++) {

				if (pos < lattice.length()) { 
					
					int rightIdx = lattice.getRightStateIdx(idx);
					int leftIdx = lattice.getLeftStateIdx(idx);
					
					if (rightIdx != Lattice.NULL_IDX && addQuintileFeatures)   
						featureGen.addQuintileFeatures(margRightString, quintiles[pos], lattice.stateScores[rightIdx]);
					
//					if (leftIdx != Lattice.NULL_IDX)
//						featureGen.addQuintileFeatures(margLeftString, quintiles[pos-1], lattice.stateScores[leftIdx]);
					
					featureGen.computeFeatures(this, lattice.seq.getInstance(), pos, computeNGramIDFromEdge(lattice, idx));
				}			

				fv[idx] = featureGen.finalizeFeatureVector();
			}
		} 
		
		return fv;
	}
	
	@Override
	public FeatureVector[] getStateFeatures(Lattice lattice) {
		throw new UnsupportedOperationException("This model does not generate state-wise features");
	}

	/**
	 * Computes the state ID's of the truth, which might be different than the actual tag #'s.
	 * 
	 * @param seq
	 * @return
	 */
	public int[] getTruthStates(Sequence seq) {
		
		int truth [] = new int[seq.length()];
		
		String[] tags = (useCoarseTags ? seq.getInstance().cpostags : seq.getInstance().postags);
		
		for (int i = 0; i < (tags.length-1); i++) 
			truth[i] = computeNGramIDFromTags(tags, i, order);
				
		return truth;
	}
	
	
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
		return ngramToString(state, order);
	}
	
	public String ngramToString(int ngram, int order) {

		if (ngram > pow(order))
			throw new RuntimeException(ngram + " is not a valid " + order + "-order ngram");
		
		String s = "";
		for (int i = (order-1); i >= 0; i--) {
			String tag = POSAlphabet.reverseLookup(computeTagFromNGramID(null, order, ngram, i));
			if (i > 0)
				tag += "->";
			s += tag;
		}
		return s; 
	}


	@Override
	public void init(Options opts) {
		super.init(opts);		
		
		if (order == -1)
			throw new RuntimeException("Order is a required parameter for NGram Models!!");
		
		if (viterbi == null)
			viterbi = new ViterbiMaxSum();
		
		Corpus c = opts.corpus;
		
		// get a list of POS tags from the training set
		POSAlphabet = new Alphabet();
		for(Sequence s: c.train[c.train.length-1]){
		  String[] tags = (useCoarseTags ? s.getInstance().cpostags : s.getInstance().postags);
		

			for (int i = 0; i < (tags.length-1); i++)
				POSAlphabet.lookupIndex(tags[i+1]);	
		}
		// add in a null tag
		POSAlphabet.lookupIndex(NULLTAG);
		POSAlphabet.stopGrowth();

		margLeftString = "margLeft_o" + order;
		margRightString = "margRight_o" + order;

		featureAlphabet = new Alphabet();
		if (featureGen == null)
			featureGen = new NGramPOSFeatures();

		featureGen.init(opts);
		featureGen.setWorkingAlphabet(featureAlphabet);
		featureGen.setComputeOnly(true);

		System.out.println("model " + this.getClass().getCanonicalName() + " initialized");
		System.out.println("featureGen: " + this.featureGen.getClass().getCanonicalName());
		
		if (usePositionFeatures) {
			featureAlphabet.allowGrowth();
			
			for(Sequence s: c.train[c.train.length-1]) {
				String[] tags = (useCoarseTags ? s.getInstance().cpostags : s.getInstance().postags);

				for (int pos = 0; pos < (tags.length-1); pos++) 
					featureGen.computePositionFeatures(this, s.getInstance(), pos);
			}
			
		} else {
			
			featureGen.addAllQuintileFeatures(margLeftString, margRightString);		
			if (useSupportedFeaturesOnly) {
				int n = 0;
				for(Sequence s: c.train[c.train.length-1]) {
					String[] tags = (useCoarseTags ? s.getInstance().cpostags : s.getInstance().postags);
					
//					s.print();
					// NB: we are computing features over EDGES so we ADD ONE to the order
					for (int pos = 0; pos < (tags.length-1); pos++) 
						featureGen.computeFeatures(this, s.getInstance(), pos, computeNGramIDFromTags(tags, pos, order+1));
					
//					if (n++ > 2)
//						break;
				}
				featureAlphabet.stopGrowth();
			}			
		}
		featureGen.setComputeOnly(false);

		System.out.println("Number of features pre-computed: " + featureAlphabet.size());
		System.out.println("Number of possible tags pre-computed: " + POSAlphabet.size() + " = " + pow(order+1) + " grams");
	}
	
	/**
	 * Returns (\# of labels)^p; for use in state mappings.
	 * 
	 * @param p
	 * @return
	 */
	public int pow(int p) {
		int r = 1;
		for (int i = 0; i < p; i++)
			r *= POSAlphabet.size();			
		return r;
	}
	
	/**
	 * Computes the numeric ID associated with a n-gram from the tags at position pos
	 * 
	 * @param tags
	 * @param pos
	 * @param n
	 * @return
	 */
	public int computeNGramIDFromTags(String [] tags, int pos, int n) {

		// N.B. since original dependency includes "<root>" we increment position
		pos++;
		
		if (n == 0)
			throw new RuntimeException("0-gram is an undefined N-gram!");
		
		int ngramID = 0;
		
		// compute n-gram id 
		for (int p = 0; p < n; p++) {					
			
			int symbol;
			if ( pos-p <= 0 || (pos-p) >= tags.length ) symbol = POSAlphabet.lookupIndex(NULLTAG); // very last symbol is "null" 
			else symbol = POSAlphabet.lookupIndex(tags[pos-p]);
			
			// given that symbol at point in history p....
			ngramID += symbol * pow(n-p-1); 
		}
		
		return ngramID;
	}

	/**
	 * Computes corresponding ngram id over a lower order ngram embedded within a higher order ngram.
	 */
	public int computeLowerOrderNGramID(int n, int state) {
				
		if (n >= order)
			throw new UnsupportedOperationException("n = " + n + " is not lower than order " + order);
		
		return state / pow(order-n); // note: integer division means floor
	}
	
	/**
	 * Computes the ngramID corresponding to conjunction of endpoints of a lattice edge
	 */
	public int computeNGramIDFromEdge(Lattice lattice, int idx) {

		int leftIdx = lattice.getLeftStateIdx(idx);
		int rightIdx = lattice.getRightStateIdx(idx);
		
		int leftState = (leftIdx == -1) ? computeNullState(order) : lattice.getStateID(leftIdx);
		int rightState = (rightIdx == -1) ? computeNullState(order) : lattice.getStateID(rightIdx);
				
		int numLabels = POSAlphabet.size();
		int suffix = leftState % numLabels;
	
		int id = rightState * numLabels + suffix;
		
		return id; 
	}


	/**
	 * Computes the pure NULL ngram id for a given order
	 */
	public int computeNullState(int o) {

		int symbol = POSAlphabet.lookupIndex(NULLTAG);
		int id = 0;
		
		for (int i = 0; i < o; i++) 
			id += symbol * pow(o-i-1);
		
		return id;
	}

	/**
	 * Returns the embedded labeling (tag #) at a given time offset in an n-gram.
	 * 
	 * @param n 
	 * 	 order of the ngram (e.g. 1 == UNIGRAM)
	 * @param ngramID
	 *   ID of the ngram
	 * @param offset
	 *  offset in time (t=0 -> current state)
	 * @return
	 */
	public int computeTagFromNGramID(Lattice l, int n, int ngramID, int offset) {
		
		if (n < 2)
			return ngramID;
		
		int state;
	
		offset = n - offset - 1;
		
		// current state
		if (offset == 0)
			state = ngramID % pow(1);
		else {
			int id = (int)(ngramID / pow(offset));
			state = id % pow(1);
		}
			
		return state;
		
	}
	
	@Override
	public boolean[] computeFilterMask(Lattice lattice, Weights w, double alpha, boolean isTraining) {

		mask = ArrayUtil.ensureCapacity(mask, lattice.getNumEdges());
		
		// assume marginalVals have already been computed
		computeEdgeMarginals(lattice, w);
		
		lattice.edgeScores = marginalVals;
		lattice.computeEdgeMeanMax();
		
		double threshold = useAlphaAsThreshold ? alpha : ((1-alpha)*lattice.meanEdgeScore + alpha*lattice.maxEdgeScore);
		if (Double.isNaN(threshold))
			throw new RuntimeException("threshold has become NaN, cannot proceed");
		
		boolean singleEdgePruned = false;
		for (int pos = 0; pos <= lattice.length(); pos++) {
			int start = lattice.getEdgeOffset(pos);
			int end = lattice.getEdgeOffset(pos+1);

			// only if there's more than one edge at a given position can we prune
			int numEdges = end-start;
			if (numEdges == 1) {
				mask[start] = true; //(lattice.edgeScores[start] >= threshold); 
			} else {
				for (int idx = start; idx < end; idx++)  
					mask[idx] = lattice.edgeScores[idx] > threshold;
			}
				
		}

		// don't prune truth during training
		if (isTraining) {
			for (int idx : lattice.findEdgeIdx(getTruthStates(lattice.seq), true))
				mask[idx] = true;
		}
		
		return mask;
	}

	@Override
	public Lattice expandLattice(Lattice lattice, boolean[] mask) {
		
		Lattice newLattice = new Lattice(lattice, this, mask);
		if (usePositionFeatures)
			newLattice.fv = getPositionFeatures(newLattice);
		else
			newLattice.fv = getEdgeFeatures(newLattice);
		
		return newLattice;
	}
	
	public int[] getNextStates(int state) {
		
		int numLabels = POSAlphabet.size();
			
		// for previous state, we need to know the suffix n-gram						
		int nextStates[] = new int[numLabels];
		
		for (int l = 0; l < numLabels; l++) 
			nextStates[l] = l * pow(order-1) + computeLowerOrderNGramID(this.order-1, state);										
		
		return nextStates;
	
	}
	
	public int[] getPreviousStates(int state) {				

		int numLabels = POSAlphabet.size();
		
		// for previous state, we need to know the suffix n-gram						
		int prevStates[] = new int[numLabels];

		int p = pow(order-1);
		int prefixBlock = state / p; // was floor
		int suffixId = state - prefixBlock*p;	

		for (int l = 0; l < numLabels; l++)
			prevStates[l] = suffixId*numLabels + l; 

		return prevStates;
	}
	
	public int numPossibleStates() {return pow(order);}

	public int[] getStartStates() {return getNextStates(computeNullState(order));}

	public Alphabet getPOSAlphabet() {return POSAlphabet;}
	public void setPOSAlphabet(Alphabet pOSAlphabet) {POSAlphabet = pOSAlphabet;}
		
	//@Override
	// FIXME: need to update read/write appropriately
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);

		order = in.readInt();
		useSupportedFeaturesOnly = in.readBoolean();
		
		POSAlphabet = (Alphabet) in.readObject();
		POSAlphabet.stopGrowth();
		featureAlphabet = (Alphabet) in.readObject();
		
		int l = in.readInt();
		trainingAlphas = new ArrayList<Double>(l);
		for (int i = 0; i < l; i++)
			trainingAlphas.add(i, in.readDouble());
		
		featureGen = (FeatureGenerator) ObjectReader.newInstance((String) in.readObject());
		featureGen.setWorkingAlphabet(featureAlphabet);
		
		viterbi = (Viterbi) ObjectReader.newInstance((String) in.readObject());
		margLeftString = (String) in.readObject();
		margRightString = (String) in.readObject();		
	}

	//@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(serialVersionUID);
		
		out.writeInt(order);
		out.writeBoolean(useSupportedFeaturesOnly);
		
		out.writeObject(POSAlphabet);
		out.writeObject(featureAlphabet);
		
		out.writeInt(this.trainingAlphas.size());
		for (double a : this.trainingAlphas)
			out.writeDouble(a);
		
		out.writeObject(featureGen.getClass().getCanonicalName());
		out.writeObject(viterbi.getClass().getCanonicalName());
		
		out.writeObject(this.margLeftString);		
		out.writeObject(this.margRightString);

	}

	@Override
	public void generateValidStates(Lattice base, boolean[] mask,
			TIntArrayList newStateIDs, TDoubleArrayList newStateScores, 
			int[] newStatePosOffsets) {

		// ----------------------------------------------------------------------
		// Pass 1: compute the number of valid states in the graph at each
		// position in the new graph,
		// including the filtering.
		
		NOrderPOS baseModel = (NOrderPOS) base.model;
		
		// note <= here! this is because there are edges from the null state '-1' at the END
		// of the lattice which count as being at position L (length of lattice)
		for (int pos = 0; pos <= base.length(); pos++) {
			
			boolean allPruned = true;

			int start = base.getEdgeOffset(pos);
			int end = base.getEdgeOffset(pos + 1);

			// add any unpruned edges as new states; 
			// also check for all states at a given position being pruned
			for (int idx = start; idx < end; idx++) {
				if (mask[idx]) {
					newStateIDs.add(baseModel.computeNGramIDFromEdge(base, idx));
					newStateScores.add(base.edgeScores[idx]);
					allPruned = false;
				} 
			}
			
			if (allPruned) {
				for (int pos1 = 0; pos1 <= base.length(); pos1++) {
					start = base.getEdgeOffset(pos1);
					end = base.getEdgeOffset(pos1 + 1);
					for (int idx = start; idx < end; idx++) {
						System.out.printf("%d:[%d] = %g\n",pos1,idx,base.edgeScores[idx]);
					}
				}
				throw new RuntimeException("lattice is broken; all edges at position " + pos + " have been pruned");
			}
			
			if (pos < base.length())
				newStatePosOffsets[pos + 1] = newStateIDs.size();
		}
		
	}

	@Override
	public int[] getNextStates(Sequence seq, int pos, int state) {
		return getNextStates(state);
	}

	@Override
	public int[] getPreviousStates(Sequence seq, int pos, int state) {
		return getPreviousStates(state);
	}

	@Override
	public FeatureVector[] getPositionFeatures(Lattice lattice) {
			
		FeatureVector fv [] = new FeatureVector[lattice.length()];
		
		double quintiles[][] = new double[lattice.length()][featureGen.getNumMarginalQuintiles()];
		
		featureGen.setWorkingAlphabet(featureAlphabet);
		
		// NB: loop using <=, not <
		for (int pos = 0; pos < lattice.length(); pos++) {
									
			// compute marginal quintiles
			if (pos < lattice.length())
				featureGen.computeQuintiles(lattice.stateScores, lattice.getStateOffset(pos), lattice.getStateOffset(pos+1), quintiles[pos]);
			
			featureGen.addPositionalQuintileFeatures(this, lattice, pos, quintiles[pos]);			
				
			featureGen.computePositionFeatures(this, lattice.seq.getInstance(), pos);

			fv[pos] = featureGen.finalizeFeatureVector();
		} 
		
		return fv;
	}

	@Override
	public void scoreLatticeEdges(Weights w, Lattice lattice) {
	
		if (usePositionFeatures){
			for (int pos = 0; pos < lattice.length(); pos++) {
				
				int start = lattice.getEdgeOffset(pos);
				int end = lattice.getEdgeOffset(pos+1);
				
				for (int edgeIdx = start; edgeIdx < end; edgeIdx++) {
					
					int offset = computeNGramIDFromEdge(lattice, edgeIdx)*featureAlphabet.size();

					if (useMixing)
						lattice.edgeScores[edgeIdx] = w.scoreMixed(lattice.fv[pos], offset);
					else
						lattice.edgeScores[edgeIdx] = w.score(lattice.fv[pos], offset);
				}
			}
		} else {
			for (int i = 0; i < lattice.fv.length; i++)
				lattice.edgeScores[i] = w.score(lattice.fv[i]);
		}
	}

	@Override
	public void scoreLatticeStates(Weights w, Lattice lattice) {
		throw new UnsupportedOperationException(
				"Method not supported by class: "
						+ this.getClass().getCanonicalName());
	}
	
	/**
	 * get the label corresponding to an edge index
	 * and position+1).  
	 * @param lattice
	 * @param idx
	 * @return the label edge corresponding to the edge at the given index or -1 if it's null
	 */
	public int edgeIdx2Label(Lattice lattice, int idx) {
		int state = lattice.getStateID(lattice.getRightStateIdx(idx));
		return computeTagFromNGramID(lattice, order, state, 0);
	}
	
	/**
	 * get the label corresponding to an state index 
	 * and position+1).  
	 * @param lattice
	 * @param idx
	 * @return the label of the state corresponding to the given index, or -1 if it's null
	 */
	public int stateIdx2Label(Lattice lattice, int idx) {
		return computeTagFromNGramID(lattice, order, lattice.getStateID(idx), 0);
	}


	@Override
	public int getNumberOfFeatures() {
		
		if (usePositionFeatures) {
			if ( ((long)featureAlphabet.size()*(long)pow(order+1)) > (long)maxCapacity) {
				useMixing = true;
				return PrimeFinder.nextPrime(maxCapacity);
			} else
				useMixing = false;
			
			return featureAlphabet.size()*pow(order+1);
		} else return featureAlphabet.size();
	}

	@Override
	public void increment(Lattice lattice, int idx, Weights w, double rate) {

		if (usePositionFeatures) {

			int pos = lattice.findEdgePosOffset(idx);
			if (pos >= lattice.length())
				return;
			
			int offset = computeNGramIDFromEdge(lattice, idx)*featureAlphabet.size();
			if (useMixing)
				w.incrementMixed(lattice.fv[pos], offset, rate);
			else
				w.increment(lattice.fv[pos], offset, rate);
		}
		else
			super.increment(lattice, idx, w, rate);
	}

	@Override
	public String toString() {
		return order + " Order POS Model: " + POSAlphabet.size() + 
		" tags, " + getNumberOfFeatures() + 
		" features " + (usePositionFeatures ? ("," + pow(order+1) + " grams, " + featureAlphabet.size() + " positional features (" + 
		pow(order+1)*featureAlphabet.size() + " possible)") : "");
	}

}