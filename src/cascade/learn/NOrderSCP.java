package cascade.learn;

import cascade.features.Weights;
import cascade.lattice.Lattice;
import cascade.model.NOrderModel;
import cascade.model.NOrderPOS;
import cascade.programs.Options;

/**
 * Update rules for the structured cascade subgradient step (or structured cascade perception = SCP).
 * 
 * See David Weiss & Ben Taskar, Structured Prediction Cascades, AISTATS 2010 for more information.
 */
public class NOrderSCP extends UpdateRule {
	
	@Override
	public void init(Options opts) {

	}

	@Override
	public double updateWeights(Lattice lattice, Weights w, double alpha) {

		NOrderModel model = (NOrderModel) lattice.model;
		
		model.computeEdgeMarginals(lattice, w);
		
		int [] argmaxEdgeIdx = lattice.getArgmaxEdgeIdx(model.alphaArgs, model.marginalVals, true);
		int [] truthEdgeIdx = lattice.findEdgeIdx(model.getTruthStates(lattice.seq), true);
		
		double argmaxScore = 0;
		for (int idx : argmaxEdgeIdx) argmaxScore += model.edgeScores[idx];
		
		double truthScore = 0;
		for (int idx : truthEdgeIdx) truthScore += model.edgeScores[idx];
		
		lattice.edgeScores = model.marginalVals;
		lattice.computeEdgeMeanMax();
		
		double threshold = (1-alpha)*lattice.meanEdgeScore + alpha*lattice.maxEdgeScore;
		
		if ((float)lattice.maxEdgeScore != (float)argmaxScore) {
			
			int l = lattice.length();
			int start = lattice.getEdgeOffset(l);
			int end = lattice.getEdgeOffset(l+1);
			for (int idx = start; idx < end; idx++) {
				
				System.out.printf("[%d] = %g, [alpha=%g]\n", idx, lattice.edgeScores[idx],
						model.alphaVals[lattice.getLeftStateIdx(idx)]);
				
			}
			throw new RuntimeException("inference is broken for some reason: " + 
					lattice.maxEdgeScore + " != " + argmaxScore);
		}
		
		double mistakes = 0;
		if (truthScore <= (threshold + lattice.length()) ) {

			// -----------------------------------------
			// update truth
			for (int idx : truthEdgeIdx) lattice.model.increment(lattice, idx, w, learnRate);
			
			// -----------------------------------------
			// update argmax
			if (alpha > 0) 
				for (int idx : argmaxEdgeIdx) lattice.model.increment(lattice, idx, w, -alpha*learnRate);

			// -----------------------------------------
			// update mean
			double scaled = ((1.0-alpha)*learnRate)/lattice.getNumEdges();
			
			// compute # of times each marginal takes part of a witness
			lattice.computeEdgeWitnesses(model.alphaArgs, model.betaArgs, model.witnessCount);

			// update each edge based on # of witnesses it was included in
			
			for (int idx = 0; idx < lattice.getNumEdges(); idx++)
				if (model.witnessCount[idx] > 0)
					lattice.model.increment(lattice, idx, w,-scaled*(double)model.witnessCount[idx]);
					//totalchange += witnessCount.getQuick(idx)*scaledLearnRate;
			
			mistakes = 1;
		}
	
		return mistakes;
	}

}
