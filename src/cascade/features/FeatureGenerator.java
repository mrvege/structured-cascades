package cascade.features;

import java.io.Serializable;
import java.util.Arrays;


import gnu.trove.TDoubleArrayList;
import cascade.util.Alphabet;
import cascade.util.IntBuffer;
import cascade.io.SentenceInstance;
import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.programs.Options;
import cascade.util.ArrayUtil;

/**
 * 
 * Abstract class that provides some basic feature-building capabilities.
 * 
 * @author djweiss
 *
 */
public abstract class FeatureGenerator implements Serializable {

	private static final int DEFAULT_CAPACITY = 10000;	
	private static final int NOT_FOUND = -1;

	private double [] sortingBuffer;
	
//	public long lookupTime = 0;
//	public long totalTime = 0;
//	public long addTime = 0;
	
	// FIXME: this should not be static, but we need some way of saving it
	public static final int numQuintiles = 8;
	
	private IntBuffer keysBuffer;		
	private TDoubleArrayList valsBuffer;

	protected boolean computeOnly = false;
	private boolean binaryOnly = false;
	
	private Alphabet workingAlphabet;
	
	public Alphabet getWorkingAlphabet() {return workingAlphabet;}
	public void setWorkingAlphabet(Alphabet workingAlphabet) {this.workingAlphabet = workingAlphabet;}

	public FeatureGenerator() {
		keysBuffer = new IntBuffer(DEFAULT_CAPACITY);
		valsBuffer = new TDoubleArrayList(DEFAULT_CAPACITY);
		sortingBuffer = new double[DEFAULT_CAPACITY];
	}

	public abstract void init(Options opts);
	
	/**
	 * Function to be overridden that generates features that do NOT depend on any given state.
	 * 
	 * This is just to cleanly from calls from computeFeatures; one or the other should be used, but not both.
	 * 
	 * @param model
	 * @param inst
	 * @param pos
	 */
	public void computePositionFeatures(CascadeModel model, SentenceInstance inst, int pos) {
		throw new UnsupportedOperationException("Method not implemented by class: " + this.getClass().getCanonicalName());
	}
	
	/**
	 * Function to be overridden that generates specific features from the raw input data.
	 * 
	 * Here "state" must be interpreted by the sub-class and the model.
	 * 
	 * @param inst
	 * @param pos
	 * @param state
	 */
	public void computeFeatures(CascadeModel model, SentenceInstance inst, int pos, int state) {
		throw new UnsupportedOperationException("Method not implemented by class: " + this.getClass().getCanonicalName());
	}
	
	public void computeEdgeFeatures(CascadeModel model, SentenceInstance inst, int pos, int s1, int s2){
		throw new UnsupportedOperationException("Method not implemented by class: " + this.getClass().getCanonicalName());
	}
		
	/**
	 * Used for building a proper alphabet.
	 * 
	 * @param margLeftString
	 * @param margRightString
	 */
	public void addAllQuintileFeatures(String margLeftString, String margRightString) {
		for (int i = 0; i < numQuintiles; i++) {
			add(margLeftString + "_" + i);
			add(margRightString + "_" + i);
		}
	}	
	
	/**
	 * Adds quintile features of the form "STATE_QX/X", for all of the states in a given lattice at a given position.
	 * 
	 * @param m
	 * @param lattice
	 * @param pos
	 * @param quintiles
	 */
	public void addPositionalQuintileFeatures(CascadeModel m, Lattice lattice, int pos, double [] quintiles) {
		
		int start = lattice.getStateOffset(pos);
		int end = lattice.getStateOffset(pos+1);
		
		for (int idx = start; idx < end; idx++) {
			double val = lattice.stateScores[idx];
			String prefix = m.stateToString(lattice, lattice.getStateID(idx)) + "_";		
			for (int i = 1; i <= 3; i++) {
				if (val >= quintiles[quintiles.length-i])
					add(prefix + (quintiles.length-i));
				//addQuintileFeatures(m.stateToString(lattice.getStateID(idx)), quintiles, lattice.stateScores[idx]);
			}
		}
	}
	
	/**
	 * Binarizes a value into a single quintile bin and adds the resulting feature.
	 * 
	 * @param prefix
	 * @param quintiles
	 * @param val
	 */
	public void addQuintileFeatures(String prefix, double quintiles[], double val) {

		prefix += "_";
		for (int i = 0; i < quintiles.length; i++) {
			if (val > quintiles[i])
				add(prefix + i); // String.format("%s_Q%d/%d", prefix, i, quintiles.length));
			
		}
	}

	/**
	 * Computes quintiles from within a section of a double array.
	 * 
	 * @param marginals
	 * @param start
	 * @param end
	 * @param quintiles
	 */
	public void computeQuintiles(double [] marginals, int start, int end, double [] quintiles) {

		sortingBuffer = ArrayUtil.ensureCapacity(sortingBuffer, end-start);
		
		// sort.
		int len = 0;
		for (int idx = start; idx < end; idx++) {
			sortingBuffer[len++] = marginals[idx];
		}
		
		Arrays.sort(sortingBuffer, 0, len);
		
		double step = (double)len/(double)quintiles.length;
		
		for (int i = 0; i < quintiles.length; i++) { 
			int j = (int) Math.floor((double)i * step);
			quintiles[i] = sortingBuffer[j];
		}

		//System.out.println(Arrays.toString(quintiles));
	}

	/**
	 * Returns the contents of the keys, values buffers and resets them.
	 * 
	 * @return
	 */
	public FeatureVector finalizeFeatureVector() {
		
		FeatureVector fv = new FeatureVector(keysBuffer.toNativeArray(), valsBuffer.toNativeArray());

		keysBuffer.reset();
		valsBuffer.resetQuick();
		binaryOnly = false;
		
		return fv;
	}		
	
	/**
	 * Adds a real-valued feature. All real-valued features must be added BEFORE any compressed 
	 * features are added.
	 * 
	 * @param f
	 * @param val
	 */
	protected void add(String f, double val) {
		
		if (binaryOnly)
			throw new RuntimeException("A non-binary feature is being added after binary only!!");
		
		int id = workingAlphabet.lookupIndex(f);

		if (!computeOnly && id != NOT_FOUND) {
			keysBuffer.add(id);
			valsBuffer.add(val);
		}
	}
	
	/**
	 * Adds a binary feature in compressed form (i.e. does NOT require storing a "1.0" double).
	 * 
	 * Once this is called, real-valued features CANNOT be added until the FV is finalized.
	 * 
	 * @param f
	 */
	protected void add(String f) {
		// this is the first binary
		if (!binaryOnly && (keysBuffer.size() == valsBuffer.size()))
			binaryOnly = true;

		int id = workingAlphabet.lookupIndex(f);
			
		if (!computeOnly && id != NOT_FOUND)
			keysBuffer.add(id);
	}

	protected void add(StringBuilder sb) {add(sb.toString());}
	public boolean isComputeOnly() {return computeOnly;}
	public void setComputeOnly(boolean computeOnly) {this.computeOnly = computeOnly;}
	public int getNumMarginalQuintiles() {return numQuintiles;}
	

	
}
	

