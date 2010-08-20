package cascade.model;

import cascade.io.Sequence;
import cascade.lattice.ZeroOrderLattice;

/**
 * This model represents a "flat" or zero-order Markov model, i.e. a standard 
 * multi-class model with no sequential dependencies.
 *
 */
public abstract class ZeroOrderModel extends CascadeModel{

	/**
	 * Return the total number of states at a given position in a sequence.
	 */
	public abstract int getNumberOfStates(Sequence seq, int position);
	
	/**
	 * Return the fixed set of possible states for a given position
	 */
	public abstract int[] possibleStates(Sequence seq, int position);
	

	/**
	 * Computes the multi-class guesses separately at each position in a lattice
	 * based on the scores of the states at each position.
	 */
	public int[] computeGuesses(ZeroOrderLattice lattice) {
		int[] res = new int[lattice.length()];
		for (int pos = 0; pos < lattice.length(); pos++) {
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);
			double max = Double.NEGATIVE_INFINITY;			
			for (int idx = start; idx < end; idx++) {
				if (lattice.stateScores[idx] > max) { 
					max = lattice.stateScores[idx];
					res[pos] = lattice.getStateID(idx);
				}
			}
		}
		return res;
	}


}
