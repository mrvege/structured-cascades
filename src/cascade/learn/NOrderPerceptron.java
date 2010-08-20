package cascade.learn;

import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.NOrderModel;
import cascade.programs.Options;

/**
 * Update rules for a structured perception. 
 *
 */
public class NOrderPerceptron extends UpdateRule {

	
	@Override
	public void init(Options opts) {

	}

	@Override
	public double updateWeights(Lattice lattice, Weights w, double alpha) {

		NOrderModel model = (NOrderModel) lattice.model;
		
		
		// FIXME: only necessary to compute alpha, not all marginals!!
		// really?  I think you need all of them in order to be able to computeGuesses. 
		model.computeEdgeMarginals(lattice, w);
		
		int [] guess = model.computeGuesses(lattice);
		int [] truth = model.getTruth(lattice.seq);
		
		double mistakes = 0;
		for (int i = 0; i < truth.length; i++) {
			if (guess[i] != truth[i])
				mistakes++;
		}

		if (mistakes > 0) {
			// include updates along the final edge (X,-1)
			int [] guessEdgeIdx = lattice.getArgmaxEdgeIdx(model.alphaArgs, model.marginalVals, true);
			int [] truthEdgeIdx = lattice.findEdgeIdx(model.getTruthStates(lattice.seq), true);
			
			for (int idx : guessEdgeIdx)
				lattice.model.increment(lattice, idx, w, -learnRate);
			for (int idx : truthEdgeIdx)
				lattice.model.increment(lattice, idx, w, learnRate);			
		}
		
		return mistakes;
	}

}
