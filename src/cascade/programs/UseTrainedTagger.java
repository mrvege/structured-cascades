package cascade.programs;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;


import cascade.features.Weights;
import cascade.io.Corpus;
import cascade.io.SentenceInstance;
import cascade.io.ObjectReader;
import cascade.io.Sequence;
import cascade.lattice.Lattice;
import cascade.lattice.ZeroOrderLattice;
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
		OptionsParser optparse = new OptionsParser(tmp);
		
		
		Options options = optparse.getOptions();
		CascadeModel models [] = optparse.getModels(); // really the only thing that we need to know is their number
		Corpus corpus = options.corpus;
		for (int i = 0; i < models.length; i++) {
			models[i] = (CascadeModel) ObjectReader.readOneObject(corpus.getModelFilename(i), true);
		}
		
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
		
		Sequence[] testSequences = Corpus.readFile(args[2], Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		
		PrintStream out = new PrintStream(new FileOutputStream(args[3]));
		Alphabet alphabet;
		if(models[models.length-1] instanceof NOrderPOS){
			 alphabet = ((NOrderPOS)models[models.length-1]).POSAlphabet;
		}else if(models[models.length-1] instanceof ZeroOrderPOS){
			alphabet = ((ZeroOrderPOS)models[models.length-1]).POSAlphabet;
	}else{
			throw new UnsupportedOperationException("Model not supported: " + models[models.length-1].getClass().getCanonicalName());
		}
		int correct = 0;
		int total = 0;
		for (int instId = 0; instId < testSequences.length; instId++) {
			Sequence seq = testSequences[instId];
			int[] result = decode(seq, models, weights, alpha);
			writeInstance(out, seq.getInstance(), result, alphabet);
			String[] truth = seq.getInstance().postags;
			for (int i = 0; i < result.length; i++) {
				if(result[i] == alphabet.lookupIndex(truth[i+1])){
					correct++;
				}
				total++;
			}
		}
		System.out.println("Test set Accuracy: " + (correct*100.0/total));
		out.close();
	}
	
	public static void writeInstance(PrintStream out, SentenceInstance inst, int[] result, Alphabet a){
		for (int i = 1; i < result.length+1; i++) {
			out.print(i); out.print("\t");
			out.print(inst.forms[i]); out.print("\t"); // form
			
			//out.print(inst.forms[i]); out.print("\t"); // lemma
			
			//out.print("-"); out.print("\t"); // cpostag
			
			out.print(a.reverseLookup(result[i-1])); out.print("\t");// postag
			if (inst.feats==null) {
				out.print("_\t");
			}else {
				out.print(ArrayUtil.join(inst.feats[i],"|")); out.print("\t");// feats
			}
			//out.print(depInst.heads[i]); out.print("\t");// head
			//out.print(depInst.deprels[i]); out.print("\t");// deprel
			
			//out.print("_"); out.print("\t");// phead
			//out.print("_"); out.print("\t");// pdeprel
			out.println();
		}
		out.println();
	}

	public static int[] decode(Sequence seq, CascadeModel[] models, Weights[] weights, double[] alpha){
		Lattice[] lattices = new Lattice[models.length];
		lattices[0] = models[0].createLattice(seq);

		// we did first model, and last model is actual predictor. 
		for (int level = 1; level < models.length; level++) {
			boolean[] mask = models[level-1].computeFilterMask(lattices[level-1], weights[level-1], alpha[level-1], false);
			lattices[level] = models[level].expandLattice(lattices[level-1], mask);
		}

		// do the actual prediction...
		int level = models.length-1;
		CascadeModel model = models[level];

		
		if(model instanceof NOrderPOS){
			((NOrderModel) model).computeEdgeMarginals(lattices[level], weights[level]);
			int [] guess = ((NOrderModel) model).computeGuesses(lattices[level]);
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