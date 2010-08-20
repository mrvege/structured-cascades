package cascade.learn;

import cascade.features.FeatureVector;
import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.programs.Options;
import cascade.util.ArrayUtil;

/**
 * 
 * Update rules for a zero-order perceptron.
 * 
 *
 */
public class ZeroOrderPerceptron extends UpdateRule {

	double [] scores = null;
	
	@Override
	public void init(Options opts) {
		// doesn't matter here
	}

	@Override
	public double updateWeights(Lattice lattice, Weights w, double alpha) {
	
		if (scores == null)
			scores = new double[lattice.getNumStates()];
		else
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
			int argmax = -1; 
			
			int truthIdx = -1;
			
			for (int idx = start; idx < end; idx++) {
				if (scores[idx] > max) { 
					max = scores[idx];
					argmax = idx;
				}
				if (lattice.getStateID(idx) == labels[pos])
					truthIdx = idx;
			}
			
			// check for mistake
			if (argmax != truthIdx) {
				mistakes++;
				
				// simple perceptron update
				lattice.model.increment(lattice, argmax, w, -learnRate);
//				System.out.printf("incrementing: %g - %s\n", -learnRate, 
//						fv[argmax].toString(lattice.model.featureAlphabet));
				lattice.model.increment(lattice, truthIdx, w, learnRate);
//				System.out.printf("incrementing: %g - %s\n", learnRate, 
//						fv[truthIdx].toString(lattice.model.featureAlphabet));
			}
		}

		return mistakes;
	}

}
