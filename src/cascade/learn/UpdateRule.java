package cascade.learn;

import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.programs.Options;

/**
 * Defines the functionality of rules for updating weights for a given step of
 * the sub-gradient learning algorithms.
 * 
 */
public abstract class UpdateRule {

	public double learnRate = 1.0;

	/**
	 * Hook to initialize any necessary bookkeeping or grab parameters from the Options object.
	 */
	public abstract void init(Options opts);
	
	/**
	 * Updates the current weights given an example.
	 * 
	 * @param lattice
	 * Lattice representing the example. Should be already scored and marginals computed
	 * using the associated model.
	 * 
	 * @param w
	 * Weights object to update
	 * 
	 * @param alpha
	 * Filtering parameter of the SC framework. May or may not be used by the learner.
	 * 
	 * @return
	 */
	public abstract double updateWeights(Lattice lattice, Weights w, double alpha);
	
}
