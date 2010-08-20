package cascade.model;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import cascade.features.FeatureGenerator;
import cascade.lattice.Lattice;
import cascade.programs.Options;
import cascade.util.Alphabet;

/**
 * 
 * Takes advantage of the fact that all states 
 * are connected to all other states, to greatly simplify
 * logic.
 * 
 * It is necessary since creating a first order lattice from a zero order lattice
 * involves only adding edges, not converting edges->states and adding new edges, as is
 * what happens when we map from a n-order to n+1 order lattice in general. (see NOrderPOS)
 * 
 */
public class FirstOrderPOS extends NOrderPOS {

	@Override
	public void generateValidStates(Lattice base, boolean[] mask,
			TIntArrayList newStateIDs, TDoubleArrayList newStateScores,
			int[] newStatePosOffsets) {

		// just add only the states that weren't pruned in the old model
		for (int pos = 0; pos < base.length(); pos++) {
			
			int start = base.getStateOffset(pos);
			int end = base.getStateOffset(pos+1);
			
			boolean allPruned = true;
			
			for (int idx = start; idx < end; idx++) 
				if (mask[idx]) {
					newStateScores.add(base.stateScores[idx]);
					newStateIDs.add(base.getStateID(idx));
					allPruned = false;
				}

			if (allPruned) {
				System.out.println("All states at position " + pos + " would be pruned:");
				base.print();
				for (int idx = start; idx < end; idx++)
					System.out.printf("score[%d] = %g [%s]", idx, base.stateScores[idx], Boolean.toString(mask[idx]));
				throw new RuntimeException("Broken lattice after pruning");
			}
			newStatePosOffsets[pos+1] = newStateIDs.size();
			
		}
	}

	@Override
	public void init(Options opts) {
		order = 1; // fix order so it can't be overridden
		super.init(opts);
	}

	@Override
	public String stateToString(Lattice l, int state) {
		return POSAlphabet.reverseLookup(state);
	}
	
	
}
