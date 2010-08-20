package cascade.features;

import java.util.Arrays;

import cascade.util.ArrayUtil;

/**
 * An extension of the basic class that allows the weights to be 
 * efficiently rescaled in constant time. This class will support the 
 * future development of Pegasos style projection steps in the learning algorithm.
 *
 * It also adds support for batch algorithms by delaying updates until the commit
 * method is called.
 */
public class ScalableWeights extends Weights {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static double RESCALE_TRIGGER = 1e100;
	
	private int numRescales = 0;
	private int u = 0;
	private int updateKeys[];
	private double updateVals[];
	
	private double scale, sumSquares;

	private final int hash(int i, int max) { return (int) (((long)i*31L) % max); } 

	public ScalableWeights(int n) {
		this(new double[n]);
	}
	
	public ScalableWeights(double[] w) {
		super(w);
		scale = 1.0;
		for (double v : w)
			sumSquares += v*v;
	}

	@Override
	public Weights getCopy() {

		double wnew [] = Arrays.copyOf(w, w.length);
		for (int i = 0; i < wnew.length; i++)
			wnew[i] *= scale;

		return new Weights(wnew);
	}

	@Override
	public double getNorm() { //
		//return scale*super.getNorm();
		return scale*Math.sqrt(sumSquares); 
	}
	
	public String toString() { return "ScalableWeights=[scale=" + scale + ", sumSquares=" + sumSquares + ", len=" + w.length + ", numRescales=" + numRescales + "]"; }

	public void multiply(double a) {scale *= a; }
	
	private void ensureCapacity(FeatureVector fv) {
		updateKeys = ArrayUtil.ensureCapacity(updateKeys, u+fv.size());
		updateVals = ArrayUtil.ensureCapacity(updateVals, u+fv.size());
	}
	
	public void commit() {
		
		double totalchange = 0;
		for (int i = 0; i < u; i++) {
			int key = updateKeys[i];
			double val = updateVals[i]/scale; // normalize by the current scale

			sumSquares += 2*w[key]*val + val*val;
			w[key] += val;
			//totalchange += val*val;
		}
		
		// commit to rescaling
		if (sumSquares > RESCALE_TRIGGER) {
			sumSquares = 0;
			for (int i = 0; i < w.length; i++) {
				w[i] *= scale;
				sumSquares += w[i]*w[i];
			}
			scale = 1.0;
			numRescales++;
		}
			
		//System.out.println("Commited "  + u + " changes of norm " + Math.sqrt(totalchange));
		// reset queue
		u = 0;
	}
	@Override
	public void increment(FeatureVector fv, double rate) {
		ensureCapacity(fv);
		for (int i = 0; i < fv.keys.length; i++) {
			updateKeys[u] = fv.keys[i];
			updateVals[u] = rate;
			u++;
		}
	}
	
	@Override
	public void increment(FeatureVector fv, int offset, double rate) {
		ensureCapacity(fv);
		for (int i = 0; i < fv.keys.length; i++) {
			updateKeys[u] = fv.keys[i]+offset;
			updateVals[u] = rate;
			u++;
		}
	}

	@Override
	public void incrementMixed(FeatureVector fv, int offset, double rate) {
		ensureCapacity(fv);
		for (int i = 0; i < fv.keys.length; i++) {
			updateKeys[u] = hash(fv.keys[i]+offset, w.length);
			updateVals[u] = rate;
			u++;
		}
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
