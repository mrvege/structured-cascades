package cascade.learn;

import java.util.Arrays;

import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.model.NOrderModel;
import cascade.model.NOrderPOS;

public class LossFunctions  {	
		
	/**
	 * compute the state efficiency loss when we're dealing with a zero-order model. 
	 * @param lattice 
	 * @param marginals state marginals (one double for each state)
	 * @param thresholds position thresholds (one double for each position) of an 
	 * array of length 1 (i.e. just one double)
	 * @return the average fraction of unpruned states. 
	 */
	public static double computeStateEfficiencyLoss(Lattice lattice, double [] marginals, double thresholds[]) {
		
		double loss = 0;
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);
	
			double threshold = (thresholds.length > 1) ? thresholds[pos] : thresholds[0];
			
			double numPruned = 0; 
			for (int idx = start; idx < end; idx++) {
				if (marginals[idx] <= threshold)
					numPruned++;
			}
			loss += (1.0 - numPruned/(double)(end-start));
		}
		
		return loss / (double)(lattice.length());
	}
	
	/**
	 * compute the edge efficiency loss (i.e. when we're dealing with a non-zero order model
	 * @param lattice
	 * @param marginals one double for each edge in the lattice
	 * @param thresholds one double for each position in the lattice, or one double for the entire lattice
	 * @return
	 */
	public static double computeEdgeEfficiencyLoss(Lattice lattice, double [] marginals, double thresholds[]) {
		
		double loss = 0;
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getEdgeOffset(pos);
			int end = lattice.getEdgeOffset(pos+1);
	
			double threshold = (thresholds.length > 1) ? thresholds[pos] : thresholds[0];
			
			double numPruned = 0; 
			for (int idx = start; idx < end; idx++) {
				if (marginals[idx] <= threshold)
					numPruned++;
			}
			loss += (1.0 - numPruned/(double)(end-start));
		}
		
		return loss / (double)(lattice.length());
	}
	
	/**
	 * compute the fraction of entries in truthIdx that are both != Lattice.NULL_IDX and also have
	 * marginals >= threshold.  This should work with both state and edge marginals, but truthIdx 
	 * would have to be different. 
	 * @param lattice
	 * @param marginals either state or edge marginals (truthIdx indexes into this)
	 * @param thresholds either one entry for each entry in truth or one entry
	 * @param truthIdx the index (in marginals) for each entry of the truth (or Lattice.NULL_IDX) if truth is not in 
	 * marginals (i.e. already pruned). 
	 * @return fraction of positions where either the truth was pruned already, or we will prune it now. 
	 */
	public static double computeFilterLoss(Lattice lattice, double [] marginals, double [] thresholds, int [] truthIdx) {
	
		double mistakes = 0;
		for (int i = 0; i < truthIdx.length; i++) {
			int truth = truthIdx[i];
			if (truth == Lattice.NULL_IDX) // Pruning mistake occured already
				mistakes++;
			else if (marginals[truth] <= (thresholds.length > 1 ? thresholds[i] : thresholds[0]))
				mistakes++;
		}

		return mistakes / (double)lattice.length();
	}	
	
	
	public static double computeZeroOrderEdgeEfficiencyLoss(Lattice lattice, double [] marginals, double thresholds []) {
		if(lattice.model instanceof NOrderPOS){
			 return computeZeroOrderEdgeEfficiencyLoss(((NOrderPOS)lattice.model),lattice, marginals, thresholds);
		}else{
			throw new UnsupportedOperationException("Not defined for this model: " + lattice.model.getClass().getCanonicalName());
		}
	}
	
	
	
	/**
	 * Computes the fraction of labels given the edge marginals and thresholds.  
	 * Specific to an NOrderPOS model. 
	 * 
	 * @param lattice
	 * @param marginals edge marginals for the lattice
	 * @param thresholds either one threshold per position or one threshold for the lattice
	 * @return
	 */
	public static double computeZeroOrderEdgeEfficiencyLoss(NOrderPOS model, Lattice lattice, double [] marginals, double thresholds []) {
		
		boolean pruned[] = new boolean[model.getNumberOfTags()];
		
		double loss = 0;
		for (int pos = 0; pos < lattice.length(); pos++) {
			int start = lattice.getEdgeOffset(pos);
			int end = lattice.getEdgeOffset(pos+1);
	
			double threshold = thresholds.length > 1 ? thresholds[pos] : thresholds[0];
			Arrays.fill(pruned, true);

			for (int idx = start; idx < end; idx++) {
				if (marginals[idx] > threshold) {

					int ngram = model.computeNGramIDFromEdge(lattice, idx);
					int label = model.computeTagFromNGramID(lattice, model.order+1, ngram, 0);
					
					pruned[label] = false;
				}
			}
			
			double numPruned = 0;
			for(boolean b: pruned) if(b) numPruned++;
			
			loss += (1.0 - numPruned/(double)(pruned.length));
		}
		return loss / (double)(lattice.length());
	}
	
	
	/**
	 * Computes the fraction of labels given the edge marginals and thresholds.  
	 * Specific to an NOrderPOS model. 
	 * 
	 * @param lattice
	 * @param marginals edge marginals for the lattice
	 * @param thresholds either one threshold per position or one threshold for the lattice
	 * @return
	 */
//	public static double computeZeroOrderEdgeEfficiencyLoss(NOrderSuperTag model, Lattice lattice, double [] marginals, double thresholds []) {
//
//		boolean pruned[] = new boolean[model.getNumberOfTags()];
//		
//		double loss = 0;
//		for (int pos = 1; pos < lattice.length(); pos++) {
//			int start = lattice.getEdgeOffset(pos);
//			int end = lattice.getEdgeOffset(pos+1);
//	
//			double threshold = thresholds.length > 1 ? thresholds[pos] : thresholds[0];
//			Arrays.fill(pruned, true);
//
//			for (int idx = start; idx < end; idx++) {
//				if (marginals[idx] > threshold) {	
//					int leftIdx = lattice.getLeftStateIdx(idx);
//					int leftStateId = lattice.getStateID(leftIdx);
//					int[] ngram = model.ngramDictionary.getNgram(leftStateId);
//					int label = ngram[ngram.length-1];
//					pruned[label] = false;
//				}
//			}
//			
//			double numPruned = 0;
//			for(boolean b: pruned) if(b) numPruned++;
//			
//			loss += (1.0 - numPruned/(double)(pruned.length));
//		}
//		return loss / (double)(lattice.length());
//	}
	
	public static double computeZeroOrderEfficiencyLoss(Lattice lattice) {
		if(lattice.model instanceof NOrderPOS){
			return computeZeroOrderEfficiencyLoss(((NOrderPOS) lattice.model),  lattice) ;
		}else{
			throw new UnsupportedOperationException("method not defined for class: " + lattice.model.getClass().getCanonicalName());
		}
	}
	
	/**
	 * Computes the fraction of labels that have not already been pruned.  Specific to 
	 * NOrderPOS models. 
	 * 
	 * @param lattice
	 * @return
	 */
	public static double computeZeroOrderEfficiencyLoss(NOrderPOS model, Lattice lattice) {
		
		

		int numLabels = model.getNumberOfTags()-1; // discount the "null" tag 
		boolean pruned [] = new boolean[numLabels];

		for (int i = 0; i < pruned.length; i++) pruned[i] = true;
		
		double loss = 0;
		for (int pos = 0; pos < lattice.length(); pos++) {

			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			for (int idx = start; idx < end; idx++) {
				int state = lattice.getStateID(idx);
				int guess = model.computeTagFromNGramID(lattice, model.order, state, 0);
				
				pruned[guess] = false;
			}
			
			double numPruned = 0;
			for (int i = 0; i < pruned.length; i++)
				if (pruned[i]) numPruned++;
			
			loss += (1.0 - numPruned/(double)(pruned.length));

			for (int i = 0; i < pruned.length; i++) pruned[i] = true;
		}
		
		return loss / lattice.length();
	}

	
	/**
	 * Computes the fraction of labels that have not already been pruned.  Specific to 
	 * NOrderPOS models. 
	 * 
	 * @param lattice
	 * @return
	 */
//	public static double computeZeroOrderEfficiencyLoss(NOrderSuperTag model, Lattice lattice) {
//		
//		
//
//		int numLabels = model.getNumberOfTags()-1; // discount the "null" tag 
//		boolean pruned [] = new boolean[numLabels];
//
//		for (int i = 0; i < pruned.length; i++) pruned[i] = true;
//		
//		double loss = 0;
//		for (int pos = 0; pos < lattice.length(); pos++) {
//
//			int start = lattice.getStateOffset(pos);
//			int end = lattice.getStateOffset(pos+1);
//
//			for (int idx = start; idx < end; idx++) {
//				int state = lattice.getStateID(idx);
//				int guess = model.ngramDictionary.getNgram(state)[model.order-1];
//				
//				pruned[guess] = false;
//			}
//			
//			double numPruned = 0;
//			for (int i = 0; i < pruned.length; i++)
//				if (pruned[i]) numPruned++;
//			
//			loss += (1.0 - numPruned/(double)(pruned.length));
//
//			for (int i = 0; i < pruned.length; i++) pruned[i] = true;
//		}
//		
//		return loss / lattice.length();
//	}
	
	/**
	 * Computes baseline filter loss (the fraction of entries in truthEdgeIdx!=NULL_IDX). 
	 * Does not assume anything about model. 
	 * 
	 * @param lattice
	 * @param truthEdgeIdx
	 * @return
	 */
	public static double computeFilterLoss(Lattice lattice, int [] truthEdgeIdx) {

		double mistakes = 0;
		for (int pos = 0; pos < lattice.length(); pos++)
			if (truthEdgeIdx[pos] == Lattice.NULL_IDX) mistakes++;
		
		return mistakes / (double)lattice.length();
	}
	
	/**
	 * Computes the baseline zero-order filter loss. Requires NOrderPOS model.
	 * FIXME: this should be made generic for NOrderModel
	 * @param seq
	 * @param model
	 * @return
	 */
	public static double computeZeroOrderFilterLoss(Lattice lattice) {
		double mistakes = 0;
		NOrderModel model = (NOrderModel) lattice.model;

		int[] labels = model.getTruth(lattice.seq);
		
		for (int pos = 0; pos < lattice.length(); pos++) {

			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);
			
			boolean pruned = true;
			for (int idx = start; idx < end; idx++) {
				int guess = model.stateIdx2Label(lattice, idx);
				if (guess < 0) throw new AssertionError("guess < 0? probably a bug");
				
				if (guess == labels[pos]) {
					pruned = false;
					break;
				}
			}
			if (pruned) mistakes++;
		}
		
		return mistakes / (double)lattice.length();
	}
	
	/**
	 * Computes cumulative zero-order filter loss.
	 * 
	 * @param seq
	 * @param model
	 * @param marginals
	 * @param thresholds
	 * @return
	 */
	public static double computeZeroOrderEdgeFilterLoss(Lattice lattice,  double [] marginals, double [] thresholds) {
		double mistakes = 0;
		
		NOrderModel model = (NOrderModel) lattice.model;
		
		int [] labels = model.getTruth(lattice.seq);
		
		for (int pos = 0; pos < lattice.length(); pos++) {

			int start = lattice.getEdgeOffset(pos);
			int end = lattice.getEdgeOffset(pos+1);
			
			double threshold = thresholds.length > 1 ? thresholds[pos] : thresholds[0];
			
			boolean pruned = true;
			for (int edgeIdx = start; edgeIdx < end; edgeIdx++) {
				int guess = model.edgeIdx2Label(lattice, edgeIdx);
				if (guess < 0) throw new AssertionError("guess < 0? probably a bug");
				
				if (guess == labels[pos] && marginals[edgeIdx] > threshold) {
					pruned = false;
					break;
				}
			}
			if (pruned)
				mistakes++;
		}
		
		return mistakes / (double)lattice.length();
	}	

	/**
	 * compute the classification error for a zero-order model (we look at state scores only). 
	 * @param lattice
	 * @param truthIdx
	 * @return
	 */
	public static double computeFlatClassifierError(Lattice lattice, int[] truthIdx) {
	
		double [] scores = lattice.stateScores;
		
		double mistakes = 0;

		// loop through each lattice position and find argmax state
		for (int pos = 0; pos < lattice.length(); pos++) {
			
			int start = lattice.getStateOffset(pos);
			int end = lattice.getStateOffset(pos+1);

			double max = Double.NEGATIVE_INFINITY;
			int argmax = -1; 
			
			for (int idx = start; idx < end; idx++) {
				if (scores[idx] > max) { 
					max = scores[idx];
					argmax = idx;
				}			
			}
			
			// check for mistake
			if (argmax != truthIdx[pos])
				mistakes++;
		}

		return mistakes/lattice.length();
	}

	
}
