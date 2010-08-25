package cascade.features;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 
 * An extension to the basic {@link cascade.features.Weights Weights} class that adds the capability to keep track of a running average
 * of all training iterations with only a constant increase in overhead.
 * 
 * 
 */
public class AveragingWeights extends Weights implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	
	private double wupdates[];
	
	@Override
	public String toString() {
		return "AveragingWeights [averaged=" + averaged + ", scale=" + scale
				+ ", t=" + t + ", wupdates=" + Arrays.toString(wupdates)
				+ ", w=" + Arrays.toString(w) + "]";
	}

	private double scale;
	private int t;
	private boolean averaged;
	
	public AveragingWeights(int l) {
		w = new double[l];
		wupdates = new double[l];
		
		scale = 1.0;
		t = 0;
		averaged = false;
	}
	
	/**
	 * Increments the averaging denominator by one.
	 */
	public void nextIteration() {t++;}
	
	/** 
	 * Returns a copy of the AVERAGED weights. Will average if not already averaged before copying.
	 */
	public Weights getCopy() {
		double[] wnew = new double[w.length];
		
		boolean didAverage = false;
		if (!averaged) {
			average();
			didAverage = true;
		}
		
		for (int i = 0; i < w.length; i++)
			wnew[i] = w[i]*scale;
		
		if (didAverage)
			unaverage();
		
		return new Weights(wnew);
	}
	
	/**
	 * Adds the sum of all updates to each weight and divides by the number of iterations,
	 * thereby computing the average thus far.  
	 */
	public void average() {
		if (averaged)
			throw new AssertionError("can't average twice!!");
		
		scale = 1.0/(double)t;
		
		for (int i = 0; i < w.length; i++)
			w[i] = (t+1.0)*w[i] - wupdates[i];
		
		averaged = true;
	
	}
	/**
	 * Subtracts the sum of all updates and rescales, thereby restoring the unaveraged weight values.
	 */
	public void unaverage() {
		if (!averaged)
			throw new AssertionError("can't unaverage twice!!");
		
		for (int i = 0; i < w.length; i++)
			w[i] = (w[i] + wupdates[i])/(t+1.0);
		
		scale = 1.0;
		averaged = false;
	}
	
	@Override
	public double getNorm() {
		return super.getNorm()*scale;
	}

	@Override
	public void increment(FeatureVector fv, double rate) {
		super.increment(fv, rate);
		fv.increment(wupdates, t*rate);
	}

	@Override
	public void increment(FeatureVector fv, int offset, double rate) {
		super.increment(fv, offset, rate);
		fv.increment(wupdates, offset, t*rate);
	}

	@Override
	public void incrementMixed(FeatureVector fv, int offset, double rate) {
		super.incrementMixed(fv, offset, rate);
		fv.incrementMixed(wupdates, offset, t*rate);
	}

	@Override
	public double score(FeatureVector fv, int offset) {
		return super.score(fv, offset)*scale;
	}

	@Override
	public double score(FeatureVector fv) {
		return super.score(fv)*scale;
	}

	@Override
	public double scoreMixed(FeatureVector fv, int offset) {
		return super.scoreMixed(fv, offset)*scale;
	}
	
	

	
	

}
