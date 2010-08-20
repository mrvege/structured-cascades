package cascade.features;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A basic wrapper to an array of double weights that can be used to 
 * compute dot products with sparse feature vectors (possibly with weight
 * mixing) and vector addition.
 *
 * Use @{AveragingWeights} or @{ScalableWeights} instead.
 */
public class Weights implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * All weights have a dense vector, regardless of representation. 
	 */
	public double w[];

	public Weights() { w = null; }
	public Weights(double w[]) { this.w = w; }
	
	public void increment(FeatureVector fv, double rate) { fv.increment(this.w, rate); }
	public void increment(FeatureVector fv, int offset, double rate) { fv.increment(this.w, offset, rate); }
	public void incrementMixed(FeatureVector fv, int offset, double rate){ fv.incrementMixed(this.w, offset, rate); }

	public double score(FeatureVector fv) { return fv.score(w); } 
	public double score(FeatureVector fv, int offset) { return fv.score(w, offset); }
	public double scoreMixed(FeatureVector fv, int offset) { return fv.scoreMixed(w, offset); } 
	
	public Weights getCopy() { return new Weights(Arrays.copyOf(w, w.length)); }
	
	public double getNorm() {
		double sum = 0;
		for (double v : w)
			sum += v*v;
		
		return Math.sqrt(sum);
	}
	

}
