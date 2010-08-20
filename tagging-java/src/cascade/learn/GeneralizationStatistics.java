package cascade.learn;

/**
 * A data-storage class that holds the many different error metrics we may compute to asssess
 * the generalization of a filter or classifier.
 * 
 * @author djweiss
 *
 */
public class GeneralizationStatistics {

	// FIXME: get rid of storing both "total" and "avg"
	
	// general pruning performance
	public double alpha;
	
	public int numExamples;
	
	public double totalPruneError;
	public double totalPruneEff; 
	public double totalPruneZEff;
	public double totalPruneZError;
	
	public int numSequences;
	public int numSequencePruneMistakes;
	 
	public double avgPruneZError;
	public double avgPruneError;
	public double avgPruneErrorSequence;
	public double avgPruneEff;
	public double avgPruneZEff;
	
	// classification statistics
	public double totalClassError;
	public int numSequenceMistakes;	
	
	public double avgError;
	public double avgErrorSequence;
	public double avgTestTime;
	
	// inference statistics
	public double numStates;
	public double numEdges;
	public double numPositions;
	public double numPossibleEdges;
	public double numPossibleStates;
	
	public double avgStatesPerPosition;
	public double avgEdgesPerState;
	public double avgEdgeSparsity;
	public double avgStateSparsity;

	// baseline filter error rates
	public double baselineErr, zeroBaselineErr;
	
	public static final String [] writeFields = {"alpha", "baselineErr", "zeroBaselineErr",
		"avgPruneError","avgPruneZError", "avgPruneErrorSequence", "avgPruneEff",
		"avgPruneZEff", "avgTestTime", "avgError", "avgErrorSequence", "avgStatesPerPosition",
		"avgEdgesPerState", "avgEdgeSparsity", "avgStateSparsity"};	
	
	public GeneralizationStatistics(Double alpha) {
		this.alpha = alpha;
	}

	/**
	 * Return a human readable summary of the results. <b>Make sure you call average() first.</b>
	 * 
	 * @param showFiltering
	 * Whether or not to show filtering metrics or just classification metrics
	 * 
	 */
	public String summarize(boolean showFiltering) {
		return (showFiltering ? String.format("\n\tAlpha Used: %.4f, Prune error: %.4f%% [+%.4f%%] / %.4f%% [+%.4f%%] zero, Eff. Loss: %.4f%%, ZEff: %.4f%%, ", 
				alpha, avgPruneError, baselineErr, avgPruneZError, zeroBaselineErr, avgPruneEff, avgPruneZEff) : "") +
				String.format("\n\tError: %.4f%%, Sequence Error: %.4f%%", avgError, avgErrorSequence) +
				String.format("\n\tAvg States Per Position: %.4f, Avg Edges Per State: %.4f", avgStatesPerPosition, avgEdgesPerState);
		
	}
	public String summarize() {
		return summarize(true);
	}

	public void average() {
		
		baselineErr = 100*baselineErr/numSequences;
		zeroBaselineErr = 100*zeroBaselineErr /numSequences;
		
		avgPruneError = 100*(totalPruneError / (double)numSequences) - baselineErr;
		avgPruneZError = 100*(totalPruneZError / (double)numSequences) - zeroBaselineErr;
		
		avgPruneErrorSequence = 100*(double)numSequencePruneMistakes/(double)numSequences; 
		avgPruneEff = 100*totalPruneEff / (double)numSequences;
		avgPruneZEff = 100*totalPruneZEff / (double)numSequences;
		
		avgTestTime /= numSequences;
		
		avgError = 100*totalClassError / numSequences;
		avgErrorSequence = 100*(double)numSequenceMistakes/(double)numSequences;
		
		avgStatesPerPosition = numStates / numPositions;
		avgEdgesPerState = numEdges / numStates;
		
		avgEdgeSparsity = 100*numEdges / numPossibleEdges;
		avgStateSparsity = 100*numStates / numPossibleStates;
	}
	
}
