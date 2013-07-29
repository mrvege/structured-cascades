package cascade.model;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

import java.security.InvalidParameterException;
import java.util.List;

import cascade.features.FeatureVector;
import cascade.features.FeatureGenerator;
import cascade.features.Weights;
import cascade.io.Sequence;
import cascade.lattice.Lattice;
import cascade.learn.FilterTradeoffStatistics;
import cascade.learn.GeneralizationStatistics;
import cascade.learn.UpdateRule;
import cascade.programs.Options;
import cascade.util.Alphabet;

/**
 * 
 * @author djweiss
 * 
 *         this class represents a way to create Lattices for a particular level
 *         of the cascade; if this is not the lowest level then a lattice from
 *         the model below this one will be required as input.
 * 
 */
/**
 * @author djweiss
 *
 */
public abstract class CascadeModel  {

	public Alphabet featureAlphabet;
	public FeatureGenerator featureGen;

	public boolean generateLatticesOnly = false;
	
	/**
	 * Returns the number of features that this model will use, given the current alphabet. This should be overridden
	 * if position based features will be used.
	 * 
	 * @return
	 *    number of features used by the model
	 */
	public int getNumberOfFeatures() { return featureAlphabet.size();	}
	
	/**
	 * List of alphas to loop over while training this model.
	 */
	public List<Double> trainingAlphas;	
	
	/**
	 * The class containing the update rule to use to train this model.
	 */
	public UpdateRule update;
	
	/**
	 *  Maximum acceptable filter error rate during tuning of the threshold. 
	 *  100 = 100%, 0.5 = 0.5% (defualt) 
	 */
	public double maxerr = 0.5;
	
	/**
	 *  Maximum acceptable alpha threshold
	 */
	public double maxalpha = 1.0;
	
	public CascadeModel() {}
	
	/**
	 * Initialization routine; this initializes the UpdateRule automatically
	 * and can be overriden if desired.
	 * 
	 * @param opts
	 */
	public void init(Options opts) {
		update.init(opts);
		
		if (trainingAlphas == null)
			throw new InvalidParameterException("trainingAlphas is a required argument to CascadeModel");
	}
	
	/**
	 * Returns an array of ints indicating the sequence of true states for this Sequence object.
	 * 
	 * @param seq
	 * @return
	 */
	public abstract int[] getTruth(Sequence seq);
	
	/**
	 * Computes the decoding (i.e. the model's guess for what the states should be)
	 * @param lattice
	 * @return
	 */
	// FIXME: we should have a method like this; so that we don't have to cast to a particular 
	// type of model at decode time.  Although maybe models should to some extent be application
	// specific.  Thoughts? -- Kuzman
//	public abstract int[] decode(Lattice lattice);

	/**
	 * Takes in a lattice from the associated preceding model and a boolean mask over 
	 * the appropriate marginals from the preceding model; generates a corresponding 
	 * lattice for this model.  If this is the lowest model, throw an exception. 
	 * 
	 * @param lattice
	 * @return
	 */
	public abstract Lattice expandLattice(Lattice lattice, boolean mask []);

	/**
	 * Creates the lattice at the first level for the instance specified by seq. 
	 * @param seq
	 * @return
	 */
	public abstract Lattice createLattice(Sequence seq);
	
	/**
	 * Takes in a base lattice and boolean mask from the preceding model and generates the sets
	 *  of valid states for the current model. Called by the default Lattice constructor.   
	 * 
	 * @param base
	 * @param mask
	 * @param newStateIDs
	 * 	TIntArrayList that will hold the new state IDs
	 * @param newStateScores
	 *  TDoubleArrayList that will hold the new state scores (if any)
	 * @param newStatePosOffsets
	 *  array holding position offsets of the new states
	 */
	public abstract void generateValidStates(Lattice base, boolean mask [], 
			TIntArrayList newStateIDs, TDoubleArrayList newStateScores, 
			int [] newStatePosOffsets);
	
	/**
	 * Returns the interpretable string representation of a given state.
	 * @param state
	 * 
	 * @return
	 */
	public abstract String stateToString(Lattice lattice, int state);

	/**
	 * Using the local alphabet, compute features over all states in the
	 * Lattice, using the lattice state ordering.
	 * 
	 * @param lattice
	 * @return
	 */
	public abstract FeatureVector[] getStateFeatures(Lattice lattice);

	/**
	 * Using the local alphabet, compute features over all edges in the
	 * Lattice, using the lattice edge ordering.
	 * 
	 * @param lattice
	 * @return
	 */
	public abstract FeatureVector[] getEdgeFeatures(Lattice lattice);

	/**
	 * Using the local alphabet, compute features over all positions in the lattice,
	 * which can later be conditioned on the current state/edge.
	 *  
	 * @param lattice
	 * @return
	 */
	public abstract FeatureVector[] getPositionFeatures(Lattice lattice);
	
	/**
	 * Computes the trade-off in filter vs. efficiency loss for a given lattice, and adds
	 * the statistics to a running tally stored in stats. 
	 * 
	 * ASSUMES that lattice has ALREADY had scores computed.
	 * 
	 * 
	 * @param lattice
	 * @param w
	 * the weights to use for inference
	 * @param tradeoff
	 */

	public abstract void addTradeoffStats(Lattice lattice, Weights w, FilterTradeoffStatistics stats);

	
	
	/**
	 * Computes the test set generalization performance of the model on the given lattice and 
	 * adds the statistics to a running tally stored in stats.
	 * 
	 * This is computed differently for different models (e.g., see ZeroOrderModel vs NOrderModel).
	 * 
	 * Since scoring is part of the test procedure that we want to measure, 
	 * this should COMPUTE scores for the lattice and measure the time taken.
	 * 
	 * @param lattice
	 * @param w
	 * the weights to use for inference
	 * @param gen
	 * @param alpha
	 */
	public abstract void addGeneralizationStats(Lattice lattice, Weights w, 
			GeneralizationStatistics stats, double alpha);

	/**
	 * Computes the mask indicating which parts of the lattice are pruned given the 
	 * weight vector w, alpha parameter alpha, and a boolean indicating whether or not this 
	 * example belongs to the training set (to avoid pruning the truth when training.) 
	 * 
	 * This is computed differently for different models (e.g., see ZeroOrderModel vs NOrderModel).
	 * 
	 * @param lattice
	 * @param w
	 * @param alpha
	 * @param isTraining
	 * @return
	 */
	public abstract boolean[] computeFilterMask(Lattice lattice, Weights w,
			double alpha, boolean isTraining);


	public abstract int[] getPreviousStates(Sequence seq, int pos, int state);

	public abstract int[] getNextStates(Sequence seq, int pos, int state);

	/**
	 * Prepares the model for handling a given lattice. By default, it just makes sure
	 * that the lattice model pointer points to THIS model. However, for a model with 
	 * paring information, it's critical 
	 * that the model can know the length of the lattice it's parsing so that state ID and 
	 * ngram IDs are computed properly.
     *
	 * @param lattice
	 */
	public void prepareForLattice(Lattice lattice) {
		// FIXME: shouldn't this be an assertion?  I'm confused. -- Kuzman
		lattice.model = this;
	}

	
	/**
	 * Fills in the stateScores field of the lattice appropriately.
	 *  
	 * @param lattice
	 */
	public abstract void scoreLatticeStates(Weights w, Lattice lattice);
	
	
	/**
	 * Fills in the edgeScores field of the lattice appropriately.
	 * @param lattice
	 */
	public abstract void scoreLatticeEdges(Weights w, Lattice lattice);

	/**
	 * Increments the weights according to the index idx into a given Lattice, which is up to the CascadeModel
	 * how to interpret it accordingly.
	 * 
	 * Default: assume idx indexes directly into the lattice FeatureVector[] array fv.
	 *
	 * @param lattice
	 * @param idx
	 * @param w
	 * @param d
	 */
	public void increment(Lattice lattice, int idx, Weights w, double rate) {
		w.increment(lattice.fv[idx], rate);
	}

	@Override
	public String toString() {
		return this.getClass().getCanonicalName() + " [" + getNumberOfFeatures() + " features]";
	}
	
}
