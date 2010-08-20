package cascade.learn;

import cascade.features.FeatureVector;
import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.programs.Options;
import cascade.util.ArrayUtil;

/**
 * 
 * Update rules for a zero-order structured cascade filter update.
 * 
 * @author djweiss
 *
 */
public class ZeroOrderSCP extends UpdateRule {

	double [] scores = null;
	
	@Override
	public void init(Options opts) {
		// doesn't matter here
	}

	@Override
	public double updateWeights(Lattice lattice, Weights w, double alpha) {
	
		scores = ArrayUtil.ensureCapacity(scores, lattice.getNumStates());
		
		int [] labels = lattice.model.getTruth(lattice.seq);		
		
		// score every state in the lattice
		lattice.stateScores = scores;
		lattice.model.scoreLatticeStates(w, lattice);

		double mistakes = 0;
		
		// loop through each lattice position and find argmax state
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			double max = Double.NEGATIVE_INFINITY;
			double mean = 0;
			
			int argmax = -1; 
			
			int truthIdx = -1;
			
			
			for (int idx = start; idx < end; idx++) {
				if (scores[idx] > max) { 
					max = scores[idx];
					argmax = idx;
				} 
				if (lattice.getStateID(idx) == labels[pos]) 
					truthIdx = idx;
				
				mean += scores[idx]; 
			}
			
			mean /= (end-start);
			
			double threshold = (1-alpha)*mean + alpha*max;
			double truthScore = scores[truthIdx];
			
			// check for mistake
			if (truthScore < (threshold + 1)) {
				mistakes++;
				
				// update max
				lattice.model.increment(lattice, argmax, w, -learnRate*alpha);
				
				// update mean
				double scaled = ((1-alpha)*learnRate)/(end-start);

				for (int idx = start; idx < end; idx++) 
					lattice.model.increment(lattice, idx, w, -scaled);
				
				// update truth
				lattice.model.increment(lattice, truthIdx, w, learnRate);
			}
		}

		return mistakes;
	}

}
