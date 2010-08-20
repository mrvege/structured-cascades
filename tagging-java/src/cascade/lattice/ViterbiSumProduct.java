package cascade.lattice;

import cascade.io.Sequence;
import cascade.util.MathUtil;

public class ViterbiSumProduct extends Viterbi {

	@Override
	public void computeAlpha(Lattice lattice, double[] alphaVals, int[] alphaArgs) {
		
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);
			
			for (int idx = start; idx < end; idx++) {
					
				double sum = -1;
				for (int edgeIdx : lattice.getLeftEdges(pos, idx-start)) {
					
					int leftIdx = lattice.getLeftStateIdx(edgeIdx);

					double val = lattice.edgeScores[edgeIdx];
					if (lattice.stateScores != null)
						val += lattice.stateScores[idx];					

					// if not the start state, accumulate value in alpha table
					if (leftIdx != Lattice.NULL_IDX)
						val += alphaVals[leftIdx];

					sum = (sum < 0) ? val : MathUtil.logsumexp(sum, val);
				}
				
				
				alphaVals[idx] = sum;
			}
		} // end loop over position	
	}

	@Override
	public void computeBeta(Lattice lattice, double[] betaVals, int[] betaArgs) {
			
		// initialize last rows to zero.
		for (int pos = lattice.length()-1; pos >= 0; pos--) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			for (int idx = start; idx < end; idx++) {
			
				double sum = -1;
				for (int edgeIdx : lattice.getRightEdges(pos, idx-start)) {

					// there can be no rightward transition to null, so don't check
					int rightIdx = lattice.getRightStateIdx(edgeIdx);
					
					double val = lattice.edgeScores[edgeIdx];
					
					if (rightIdx != Lattice.NULL_IDX)
						val += betaVals[rightIdx] + (lattice.stateScores != null ? lattice.stateScores[rightIdx] : 0);
				
					sum = (sum < 0) ? val : MathUtil.logsumexp(sum, val);
				}

				
				betaVals[idx] = sum;
			}
		}
	}

	@Override
	public void computeEdgeMarginals(Lattice lattice, double[] alphaVals,
			double[] betaVals, double[] marginalVals) {
		
		// compute partition function:
		int start = lattice.getStateOffset(lattice.length-1);
		int end = lattice.getStateOffset(lattice.length);
		
		double logZ = alphaVals[start];
		for (int idx = (start+1); idx < end; idx++) 
			logZ = MathUtil.logsumexp(logZ, alphaVals[idx]);

		// compute edge marginals:
		
		for (int pos = 0; pos <= lattice.length(); pos++) {
	
			start = lattice.getEdgeOffset(pos);
			end = lattice.getEdgeOffset(pos+1);

			double sumProb = 0;
			for (int edgeIdx = start; edgeIdx < end; edgeIdx++) {
				int leftIdx = lattice.getLeftStateIdx(edgeIdx);
				int rightIdx = lattice.getRightStateIdx(edgeIdx);
				
				double val = 0;
				
				if (leftIdx != Lattice.NULL_IDX)
					val += alphaVals[leftIdx];
				if (rightIdx != Lattice.NULL_IDX) 
					val += betaVals[rightIdx] +
					(lattice.stateScores != null ? lattice.stateScores[rightIdx] : 0);
				
				val += lattice.edgeScores[edgeIdx];
				
				marginalVals[edgeIdx] = Math.exp(val - logZ);

				sumProb += marginalVals[edgeIdx];
			}
			
			System.out.println("sumProb: " + sumProb);
		}

	}

//	public ScoredLattice sumProductBackward(Weights weights, Sequence seq) {
//	
//			Lattice lattice = seq.getLattice();
//			Features feat = seq.getFeatures();
//			
//			ScoredLattice scores = lattice.toScored();
//			
//			// Initializes scored lattice to ZERO (log(1))
//			int L = seq.length()-1;
//			for (int state : lattice.validStates(L)) 
//				scores.setScore(L, state, 0.0);
//			
//			// Start at end of sequence
//			for (int l = seq.length()-2; l >= 0; l--) {
//			
//				// Loop over un-pruned states
//				for (int state : lattice.validStates(l)) {
//					
//					double sum = 0;
//	
//					// We will score transitions from all next states
//					for (int nextState : getNextStates(state)) {									
//						if (lattice.isValid(l+1, nextState))
//							sum = CRFLoss.logsumexp(sum, scores.getScore(l+1, nextState)+this.scoreTransition(weights, feat, l+1, state, nextState));
//					}
//	
//	//				System.out.printf("backward: pos %d, state %d = %g\n", l, state, sum);
//					scores.setScore(l, state, sum);
//						
//				}
//			
//				// end loop over the whole sequence
//			}
//			
//			return scores;
//		}
//
//	public ScoredLattice sumProductForward(Weights weights, Sequence seq) {
//		Lattice lattice = seq.getLattice();
//		Features feat = seq.getFeatures();
//		
//		// Initializes scored lattice to zero...
//		ScoredLattice scores = lattice.toScored();		
//		numPositionsSearched += lattice.length() -1;
//	
//		// Start by scoring start states...
//		for (int state : this.startStates) {
//			if (lattice.isValid(0, state)) {
//				scores.setScore(0, state, this.scoreTransition(weights, feat, 0, this.nullState, state));
//			} 
//		}
//		
//		// Continue...
//		for (int l = 1; l < seq.length(); l++) {
//	
//			// Loop over un-pruned states
//			for (int state : lattice.validStates(l)) {
//				numStatesSearched++;
//	
//				double sum = 0;
//				
//				int [] prevStates = this.getPreviousStates(state);
//				
//				// We will score transitions from all VALID previous states
//				for (int prevState : prevStates) {
//					if (lattice.isValid(l-1, prevState)) {
//						numEdgesSearched++;
//						sum = CRFLoss.logsumexp(sum, scores.getScore(l-1,prevState)+this.scoreTransition(weights, feat, l, prevState, state));
//					}
//				}
//	
//				//System.out.printf("forward: pos %d, state %d = %g\n", l, state, sum);
//				scores.setScore(l, state, sum);
//			}
//		
//			// end loop over the whole sequence
//		}
//						
//		//scores.print();		
//		return scores;		
//	}

}
