package cascade.programs;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


import cascade.features.AveragingWeights;
import cascade.features.Weights;
import cascade.io.Corpus;
import cascade.io.SentenceInstance;
import cascade.io.ObjectReader;
import cascade.io.Sequence;
import cascade.lattice.Lattice;
import cascade.lattice.ZeroOrderLattice;
import cascade.learn.GeneralizationStatistics;
import cascade.model.CascadeModel;
import cascade.model.NOrderModel;
import cascade.model.NOrderPOS;
import cascade.model.ZeroOrderModel;
import cascade.model.ZeroOrderPOS;
import cascade.util.Alphabet;
import cascade.util.ArrayUtil;
import cascade.util.OptionsParser;

/**
 * Command line program to run trained cascade on a new test set and evaluate accuracy.
 * 
 * See on-line documentation for usage instructions.
 * 
 */
public class UseTrainedTagger {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		String[] tmp = new String[2];
		tmp[0]  = args[0];
		tmp[1] = args[1];
		
		
		// Run the fig parser to get options
		OptionsParser optparse = new OptionsParser(tmp, false);
		
		
		Options options = optparse.getOptions();
		CascadeModel models [] = optparse.getModels();
		Corpus corpus = options.corpus;
//		for (int i = 0; i < models.length; i++) {
//			models[i] = (CascadeModel) ObjectReader.readOneObject(corpus.getModelFilename(i), true);
//		}
//		
		int partition = corpus.getPartitions().length-1; 
		
		// read in the weights. 
		Weights[] weights = new Weights[models.length];
		double[] alpha = new double[models.length];
		for (int level = 0; level < weights.length; level++) {
			String wfname = corpus.getPartitionFilePrefix(partition, level) + "-weights";
			System.out.printf("Reading weights from file: '%s'\n", wfname);
			ObjectInputStream win = new ObjectInputStream(new FileInputStream(wfname));
			weights[level] = (Weights) win.readObject();
			alpha[level] = win.readDouble();
			win.close();
		}

//		Weights w = weights[0];
//		System.out.println(w.getClass().getCanonicalName());
//		if (w instanceof AveragingWeights)
//			System.out.println(((AveragingWeights)w).toString());
//		else
//			System.out.println(w.toString());

		// Read in the sequences for testing
		Sequence[] testSequences = Corpus.readFile(args[2], Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		
		PrintStream out = new PrintStream(new FileOutputStream(args[3]));
		
		// get the mapping from tag #'s to tags from the last model
		Alphabet alphabet;
		if (models[models.length-1] instanceof NOrderPOS){
			 alphabet = ((NOrderPOS)models[models.length-1]).POSAlphabet;
		} else if (models[models.length-1] instanceof ZeroOrderPOS){
			alphabet = ((ZeroOrderPOS)models[models.length-1]).POSAlphabet;
		} else {
			throw new UnsupportedOperationException("Model not supported: " + models[models.length-1].getClass().getCanonicalName());
		}
		
		GeneralizationStatistics genstats[] = new GeneralizationStatistics[models.length];
		for (int i = 0; i < genstats.length; i++) {
			genstats[i]= new GeneralizationStatistics(alpha[i]);
			models[i].featureAlphabet.stopGrowth();
		}
		
		// evaluate on each test sequence
		int correct = 0;
		int total = 0;
		for (int instId = 0; instId < testSequences.length; instId++) {
			Sequence seq = testSequences[instId];
			int[] result = decode(seq, models, weights, alpha, genstats);
			writeInstance(out, seq.getInstance(), result, alphabet);
			String[] truth = seq.getInstance().postags;
			for (int i = 0; i < result.length; i++) {
				if(result[i] == alphabet.lookupIndex(truth[i+1])){
					correct++;
				}
				total++;
			}
		}
		
		for (int i = 0; i < genstats.length; i++){
			genstats[i].average();
			System.out.println("Level " + i);
			System.out.println(genstats[i].summarize());
		}
		System.out.println("Test set Accuracy: " + (correct*100.0/total));
		out.close();
	}
	
	public static void writeInstance(PrintStream out, SentenceInstance inst, int[] result, Alphabet a){
		
		for (int i = 0; i < result.length; i++){
			out.print(i+1); out.print("\t");
			out.print(inst.forms[i+1]); out.print("\t");
			
			out.print(a.reverseLookup(result[i])); out.print("\t");
			out.print("_\t");
			out.println();
		}
		
//		for (int i = 1; i < result.length+1; i++) {
//			out.print(i); out.print("\t");
//			out.print(inst.forms[i]); out.print("\t"); // form
//			
//			//out.print(inst.forms[i]); out.print("\t"); // lemma
//			
//			//out.print("-"); out.print("\t"); // cpostag
//			
//			out.print(a.reverseLookup(result[i-1])); out.print("\t");// postag
//			if (inst.feats==null) {
//				out.print("_\t");
//			}else {
//				out.print(ArrayUtil.join(inst.feats[i],"|")); out.print("\t");// feats
//			}
//			//out.print(depInst.heads[i]); out.print("\t");// head
//			//out.print(depInst.deprels[i]); out.print("\t");// deprel
//			
//			//out.print("_"); out.print("\t");// phead
//			//out.print("_"); out.print("\t");// pdeprel
//			out.println();
//		}
		out.println();
	}

	public static int[] decode(Sequence seq, CascadeModel[] models, Weights[] weights, double[] alpha, GeneralizationStatistics genstats[]){
		Lattice[] lattices = new Lattice[models.length];

		lattices[0] = models[0].createLattice(seq);
	
		// we did first model, and last model is actual predictor. 
		for (int level = 0; level < models.length-1; level++) {

			CascadeModel model = models[level];
			Lattice lattice = lattices[level];
			double a = alpha[level];
			Weights w=  weights[level];

//			System.out.println(model.featureAlphabet);
//			System.out.println(model.featureGen);
//	
		if (level == 1) {

				lattice.stateScores = null;
				
				System.out.println(weights[level].hashCode());

				System.out.println(lattice.maxEdgeScore);
				System.out.println(lattice.meanEdgeScore);
				
				genstats[level].average();
				System.out.println(genstats[level].summarize());
				//lattices[level-1].print();
				System.out.println(weights[level-1].hashCode());
				
				//lattice.print();
				
				for (int i = 0; i < 5; i++)
					System.out.println(w.score(lattice.fv[i]));
			
				System.out.println(((NOrderPOS)model).viterbi);
				
				//lattice.edgeScores = ArrayUtil.ensureCapacity(lattice.edgeScores, lattice.fv.length);
				//model.scoreLatticeEdges(w, lattice);
				((NOrderPOS)model).computeEdgeMarginals(lattice, w);
				
				for (int i = 0; i < 5; i++)
					System.out.println(lattice.edgeScores[i]);
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).marginalVals[i]);
				
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).alphaVals[i]);
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).betaVals[i]);
	
				
				System.exit(1);
			}

			boolean [] mask = model.computeFilterMask(lattice, w, a, false);
			
			Lattice newLattice = models[level+1].expandLattice(lattice, mask);
			lattices[level+1] = newLattice;
			
			model.addGeneralizationStats(lattice, w, genstats[level], a);

			System.out.println(model.featureAlphabet.toString());//			
				
			//System.out.println(models[level].featureAlphabet.toString());
			
//			

			
//			lattices[level].print();
			//System.exit(1);
		}

		// do the actual prediction...
		int level = models.length-1;
		CascadeModel model = models[level];

		model.addGeneralizationStats(lattices[level], weights[level], genstats[level], alpha[level]);
		
		if(model instanceof NOrderPOS){
			((NOrderModel) model).computeEdgeMarginals(lattices[level], weights[level]);
			int [] guess = ((NOrderModel) model).computeGuesses(lattices[level]);

//			System.out.print(Arrays.toString(guess) + " --> ");
//			for (int s : guess )
//				System.out.print( ((NOrderPOS)model).POSAlphabet.reverseLookup(s) + ",");
//			System.out.println();
//	

			return guess;
		}else if(model instanceof ZeroOrderPOS){
			lattices[level].stateScores = new double[lattices[level].getNumStates()];
			((ZeroOrderModel) model).scoreLatticeStates(weights[level], lattices[level]);
			int [] guess = ((ZeroOrderModel) model).computeGuesses((ZeroOrderLattice)lattices[level]);
			return guess;
		}else{
			throw new UnsupportedOperationException("Model not supported: " + models[models.length-1].getClass().getCanonicalName());
		}
	}

}
