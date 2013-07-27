package cascade.model;

import cascade.features.Weights;
import cascade.io.Sequence;
import cascade.lattice.Lattice;

/**
 * This model represents an sequential Markov model of order N. Note that in "n-gram" notation, the order is n-1:
 * A first-order markov model is a bi-gram or 2-gram model, and so forth.
 * 
 * This class is used by Lattices   
 */
public abstract class NOrderModel extends CascadeModel {

	/**
	 * Buffer to store argmax parameter of alpha matrix during forward-backward
	 */
	public int[] alphaArgs;
	
	/**
	 * Buffer to store values of alpha matrix during forward-backward 
	 */	
	public double alphaVals[];
	
	/**
	 * Buffer to store scores of all edges in a lattice during forward-backward 
	 */
	public double edgeScores[];
	
	/**
	 * Buffer to store argmax parameter of beta matrix during forward-backward 
	 */
	public int betaArgs[];
	
	/**
	 * Buffer to store marginals computed over edges during forward-backward 
	 */
	public double[] marginalVals;
	
	/**
	 * Whether or not to add hamming loss to scores for loss augmented inference.
	 */
	public boolean addHammingLoss = false;
	
	/**
	 * The Markov order of the model. Note that in "n-gram" notation, order is n-1: 
	 * a FIRST order model (order=1) is a BI-GRAM or 2-gram. 
	 * A SECOND order model is a TRI-GRAM or 3-gram model. And so forth.
	 */
	public int order = -1;

	/**
	 * Buffer to store the number of times a given edge is used in a max marginal argmax sequence,
	 *   a.k.a. a "witness" sequence for each max marginal.
	 */
	public int [] witnessCount; 

	/**
	 * compute the edge marginals
	 * FIXME: This should really be computeMarginals, and should reside in 
	 * CascadeModel. 
	 * @param lattice
	 * @param w
	 * @return time to complete FIXME: this shouldn't be computed inside this method, right?
	 */
	public abstract double computeEdgeMarginals(Lattice lattice, Weights w);

	/**
	 * compute the best guess for the lattice. 
	 * @param lattice
	 * @return
	 */
	public abstract int[] computeGuesses(Lattice lattice);

	/**
	 * get the truth in terms of the model states. 
	 * @param seq
	 * @return
	 */
	public abstract int[] getTruthStates(Sequence seq);

	/**
	 * get the label corresponding to an edge index
	 * and position+1).  
	 * @param lattice
	 * @param idx
	 * @return the label edge corresponding to the edge at the given index or -1 if it's null
	 */
	public abstract int edgeIdx2Label(Lattice lattice, int idx);
	
	/**
	 * get the label corresponding to an state index 
	 * and position+1).  
	 * @param lattice
	 * @param idx
	 * @return the label of the state corresponding to the given index, or -1 if it's null
	 */
	public abstract int stateIdx2Label(Lattice lattice, int idx);
//	public abstract int computeTagFromNGramID(Lattice lattice, int order, int state,
//			int i);


}
