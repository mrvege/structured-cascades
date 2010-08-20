package cascade.lattice;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import cascade.features.FeatureVector;
import cascade.io.Sequence;

/**
 * Defines the functionality of a forward-backward algorithm; 
 * this allows plug and play of max-sum vs. sum-product marginal computations. 
 * 
 */
public abstract class Viterbi {

	/**
	 * Compute forward-backward marginals over each edge in a specified lattice.
	 * Assumes memory exists to store results.
	 * 
	 * @param lattice
	 * Lattice to use
	 * @param alphaVals
	 * Already computed forward-pass values
	 * @param betaVals
	 * Already computed backward-pass values
	 * @param marginalVals
	 * Pre-allocated array to hold results
	 */
	public abstract void computeEdgeMarginals(Lattice lattice, 
			double [] alphaVals, double [] betaVals, double [] marginalVals);
			
	/**
	 * Computes forward pass of the algorithm on a given lattice. Assumes memory exists
	 * to store results.
	 * 
	 * @param lattice
	 * Lattice to use
	 * @param alphaVals
	 * Array to hold values of forward pass for each state
	 * @param alphaArgs
	 * Array to hold argmax of forward pass for each state
	 */
	public abstract void computeAlpha(Lattice lattice, double alphaVals[], int [] alphaArgs);
	
	/**
	 * Computes backward pass of the algorithm on a given lattice. Assumes memory exists
	 * to store results. see computeAlpha.
	 */
	public abstract void computeBeta(Lattice lattice, double betaVals[], int [] betaArgs);
}