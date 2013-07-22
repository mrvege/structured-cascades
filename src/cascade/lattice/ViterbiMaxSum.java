package cascade.lattice;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import cascade.features.FeatureVector;
import cascade.io.Sequence;


public class ViterbiMaxSum extends Viterbi {


	@Override
	public void computeAlpha(Lattice lattice, double[] alphaVals,int[] alphaArgs) {

		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			for (int idx = start; idx < end; idx++) {
					
				// compute max over transitions for this state: max stores LINEAR indexing
				double max = Double.NEGATIVE_INFINITY;
				int argmax = -1;

				for (int edgeIdx : lattice.getLeftEdges(pos, idx-start)) {
					
					int leftIdx = lattice.getLeftStateIdx(edgeIdx);

					double val = lattice.edgeScores[edgeIdx];
					//System.out.printf("%d: val = %g\n", idx, val);

//					if (lattice.stateScores != null)
//						val += lattice.stateScores[idx];					

					//System.out.printf("%d: val = %g\n", idx, val);
					// if not the start state, accumulate value in alpha table
					if (leftIdx != Lattice.NULL_IDX)
						val += alphaVals[leftIdx];
					
					//System.out.printf("%d: val = %g\n", idx, val);
					if (val > max) {
						argmax = edgeIdx;
						max = val;
						
						//System.out.printf("%d: maxval = %g\n", idx, max);
					}					
					
				}
				if (argmax == -1)
					throw new RuntimeException("computing Alpha failed: invalid argmax");

				// assign max, argmax for this state
				alphaVals[idx] = max;
				alphaArgs[idx] = argmax;
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
				
				// compute max over transitions for this state
				double max = Double.NEGATIVE_INFINITY;
				int argmax = -1;

				for (int edgeIdx : lattice.getRightEdges(pos, idx-start)) {

					// there can be no rightward transition to null, so don't check
					int rightIdx = lattice.getRightStateIdx(edgeIdx);
					
					double val = lattice.edgeScores[edgeIdx];
					
					if (rightIdx != Lattice.NULL_IDX)
						val += betaVals[rightIdx]; // + (lattice.stateScores != null ? lattice.stateScores[rightIdx] : 0);
					
					if (val > max) {
						argmax = edgeIdx;
						max = val;
					}
				}

				if (argmax == -1)
					throw new RuntimeException("computing Beta failed: invalid argmax");

				// assign max, argmax for this state
				betaVals[idx] = max;
				betaArgs[idx] = argmax;
			}
		} // end loop over position	
	}

	@Override
	public void computeEdgeMarginals(Lattice lattice, double[] alphaVals,
			double[] betaVals, double[] marginalVals) {
		int N = lattice.getNumEdges();
	
		for (int idx = 0; idx < N; idx++) { 

			int leftIdx = lattice.getLeftStateIdx(idx);
			int rightIdx = lattice.getRightStateIdx(idx);
			
			double val = 0;
			
			if (leftIdx != Lattice.NULL_IDX)
				val += alphaVals[leftIdx];
			if (rightIdx != Lattice.NULL_IDX) 
				val += betaVals[rightIdx];
				//(lattice.stateScores != null ? lattice.stateScores[rightIdx] : 0);
			
			
			val += lattice.edgeScores[idx];
			
			marginalVals[idx] = val;
			
		}
	}
	
		
}

