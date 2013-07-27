package cascade.learn;

import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.NOrderModel;
import cascade.programs.Options;

public class NOrderSSG extends UpdateRule {

	@Override
	public void init(Options opts) {
		// TODO Auto-generated method stub

	}

	@Override
	public double updateWeights(Lattice lattice, Weights w, double alpha) {

	NOrderModel model = (NOrderModel) lattice.model;
		
		boolean oldval = model.addHammingLoss;
		model.addHammingLoss = true;
		// FIXME: only necessary to compute alpha, not all marginals!!
		// really?  I think you need all of them in order to be able to computeGuesses. 
		model.computeEdgeMarginals(lattice, w);

		// include updates along the final edge (X,-1)

		int [] guessEdgeIdx = lattice.getArgmaxEdgeIdx(model.alphaArgs, model.marginalVals, true);
		int [] truthEdgeIdx = lattice.findEdgeIdx(model.getTruthStates(lattice.seq), true);

		double gscore = 0, tscore = 0;
		for (int idx : guessEdgeIdx)
			gscore += lattice.edgeScores[idx];
		for (int idx : truthEdgeIdx)
			tscore += lattice.edgeScores[idx];

		model.addHammingLoss = oldval;

		//System.out.printf("%.3g vs. %.3g\n", tscore, gscore);

		if (tscore <= gscore) {
			for (int idx : guessEdgeIdx)
				lattice.model.increment(lattice, idx, w, -learnRate);
			for (int idx : truthEdgeIdx)
				lattice.model.increment(lattice, idx, w, learnRate);			
			return 1;			
		} else
			return 0;

	}

}
