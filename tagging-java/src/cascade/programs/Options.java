package cascade.programs;

import cascade.io.Corpus;


/**
 * @author djweiss
 *
 */
public class Options  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int trainEpochs = 30;
	
	/**
	 * @deprecated this is not used yet
	 */
	public int miniBatch = 50;
	
	public double trainUpdatePercentage = 0.25;
	public double testUpdatePercentage = 0.25;
	
	public int seed = -1;
	
	// verbosity of program output:
	//    0 -- only indicates final performance of each round
	//    1 -- outputs per-epoch training
	//    2 -- outputs everything
	public int verbosity = 0;
	
	// should we save weights during training as we go 
	public Corpus corpus;

	public boolean alwaysPrecomputeFirst = false;
	public boolean loadFirstIntoRAM = false;
	public boolean precomputeFirstOnlyIfNonExistent = true;

	
	/**
	 * @deprecated 
	 */
	public boolean doProjection = true;
	
	public Options() {
		seed = (int) Math.random()*Integer.MAX_VALUE;
	}
	
	public void print(int level, String s) {
		if (level <= verbosity)
			System.out.print(s);
	}
	public void println(int level, Object o) {
		println(level, o.toString());
	}
	public void println(int level, String s) {
		
		if (level <= verbosity)
			System.out.println(s);
	}
	
}