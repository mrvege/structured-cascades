package cascade.programs;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

import cascade.features.AveragingWeights;
import cascade.features.Weights;
import cascade.io.Corpus;
import cascade.io.ObjectWriter;
import cascade.io.SimpleLogger;
import cascade.lattice.Lattice;
import cascade.learn.FilterTradeoffStatistics;
import cascade.learn.GeneralizationStatistics;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.util.ArrayUtil;
import cascade.util.OptionsParser;
import cascade.util.RunTimeEstimator;

/**
 * A command-line program to train and evaluate a cascade on a given dataset.
 * 
 * For usage information, see {@link cascade.util.OptionsParser} about how command-line
 * options are parsed.
 *
 * See README information about the output that this program creates.
 */
public class TrainTagger {

	static class ModelWeights{
		public ModelWeights(double bestalpha, Weights bestw, int epoch) {
			this.alpha = bestalpha;
			this.weights = bestw;
			this.epoch = epoch;
		}
		public double alpha;
		public Weights weights;
		public int epoch;
	}


	/**
	 * Runs the structured cascade training and evaluation.
	 */
	public static void main(String[] args) throws Exception {

		// Run the fig parser to get options
		OptionsParser optparse = new OptionsParser(args);

		Options options = optparse.getOptions();
		CascadeModel models [] = optparse.getModels();
		boolean [] trainModel = optparse.getDoTraining();

		Corpus corpus = options.corpus;

		// read in the first level of data from disk	
		String [] partitions = corpus.getPartitions();

		// make sure that the first level of the lattices exist
		if (trainModel[0] || options.alwaysPrecomputeFirst) {
			ensureFirstLevelLattices(options, models);
		}

		///////////////////////////////////////
		// train the pruning part of the cascade

		for (int level = 0; level < (models.length-1); level++) {
			// for each partition
			if (!trainModel[level]) {
				options.println(1,"><><><><><><><><><><><><><><><><><><><><><><><><");
				options.println(0,"level " + level + " is already loaded, skipping...");
				options.println(1,"><><><><><><><><><><><><><><><><><><><><><><><><");
				continue;
			}			
			for (int partition = 3; partition < partitions.length; partition++) {
				ModelWeights w;
				
				if (!models[level].generateLatticesOnly)
					w = trainFilterPartition(options, models, level, partition);
				else {
					options.println(0,"Generating lattices only for " + level + "-" + partition);
					String wfname = corpus.getPartitionFilePrefix(partition, level) + "-weights";
					options.println(0,"Loading weights from: " + wfname);
					ObjectInputStream win = new ObjectInputStream(new FileInputStream(wfname));
					Weights we = (Weights)win.readObject();
					for (double w1 : we.w)
						options.println(0,w1);
					
					double alpha = win.readDouble();
					w = new ModelWeights(alpha, we, -1);
					win.close();
				}
					
				boolean isFullPartition = (partition == partitions.length-1);
				computeLatticesForNextLevel(options, models, level, partition, isFullPartition, w);
			}

			// last step: save the the next model, which was used to generate the features for the next level, 
			// to file, so that it can be loaded in the futures.
			String nextmodelname = corpus.getModelFilename(level+1);
			options.print(1, String.format("Saving next model to file: '%s'\n", nextmodelname));
			ObjectWriter.writeOneObject(nextmodelname, models[level+1]);

		}

		////////////////////////////////////////
		// train the final classifier part of the cascade

		int level = models.length-1;
		int partition = partitions.length-1; 

		options.println(1,"=====================================================================");
		options.println(0,"TRAINING Level: " + level + " Partition: " + corpus.partitionNames[partition]);
		options.println(1,"=====================================================================");

		CascadeModel model = models[level];

		// initialize weights
		AveragingWeights w = new AveragingWeights(model.getNumberOfFeatures());
		Weights bestw = null;
		
		double bestalpha = -1;
		int bestEpoch = -1;

		int avgDenominator = 0;

		// run training pass
		corpus.switchToTrain(partition, level);

		SimpleLogger trainingProgress = new SimpleLogger(corpus.getPartitionFilePrefix(partition, level) +  "-train");
		trainingProgress.println("epoch, trainMistakes, wNorm," + ArrayUtil.join(GeneralizationStatistics.writeFields) +
				"," + ArrayUtil.join(FilterTradeoffStatistics.writeFields));

		double minError = Double.MAX_VALUE;

		// run training
		for (int t = 0; t < options.trainEpochs; t++) {

			// run training pass
			corpus.switchToTrain(partition, level);

			double mistakes = 0;
			//RunTimeEstimator est = new RunTimeEstimator(corpus.train[partition].length, options.trainUpdatePercentage);

			double elapsed = 0;
			while (corpus.hasMoreLattices()) {
				Lattice lattice = corpus.nextLattice();
				model.prepareForLattice(lattice);

				// run weight update procedure
				long startTime = System.nanoTime();
				mistakes += model.update.updateWeights(lattice, w, 0.0);

				w.nextIteration();

				elapsed += System.nanoTime()-startTime;
				//est.report();
			}
			elapsed /= 1e9; // convert nanoseconds to seconds

			options.println(1,"Finished training: " + mistakes + " updates");
			options.print(2, String.format("Total train CPU time discounting I/O: %g s = %g iter/s\n",
					elapsed, (double)corpus.train[partition].length/elapsed));

			// run evaluation pass on development set
			corpus.switchToDevel(partition, level);					

			GeneralizationStatistics genstats = new GeneralizationStatistics(0.0); 

			// Average weights and compute norm
			w.average();
			double wNorm = w.getNorm(); 

			while (corpus.hasMoreLattices()) {
				Lattice lattice = corpus.nextLattice();
				model.prepareForLattice(lattice);
				model.addGeneralizationStats(lattice, w, genstats, 0.0);							
			}

			// Compute test statistics / efficiency vs. accuracy trade-off
			genstats.average();

			String logstr = t + "," + mistakes + "," + wNorm + "," 
			+ ArrayUtil.joinDoubleFields(genstats, GeneralizationStatistics.writeFields);

			trainingProgress.println(logstr);

			options.println(2,"Peformance:");
			options.println(2,genstats.summarize());

			if ( genstats.avgError < minError) {
				options.println(2,"** Best weights so far **");
				bestw = w.getCopy();
				bestEpoch = t;
				minError = genstats.avgError;
			}
			
			w.unaverage();
		}

		// save model weights
		String wfname = corpus.getPartitionFilePrefix(partition, level) + "-weights";
		options.print(1, String.format("Saving weights to file: '%s'\n", wfname));
		
		ObjectOutputStream wout = new ObjectOutputStream(new FileOutputStream(wfname));
		wout.writeObject(bestw);
		wout.writeDouble(0.0);
		wout.close();

		/////////////////////////////////////////////////////////
		// compute lattices for next model
		corpus.switchToTest(partition, level);

		// also compute test-set performance stats 
		// (but only for full partition...test set performance is meaningless for others)
		GeneralizationStatistics genstats = new GeneralizationStatistics(bestalpha); 

		RunTimeEstimator est = new RunTimeEstimator(corpus.test[partition].length, options.testUpdatePercentage);
		double elapsed = 0;
		while (corpus.hasMoreLattices()) {
			Lattice lattice = corpus.nextLattice();
			model.prepareForLattice(lattice);

			long startTime = System.nanoTime();

			model.addGeneralizationStats(lattice, bestw, genstats, bestalpha);
			elapsed += System.nanoTime() - startTime;

			if (options.verbosity > 1)
				est.report();
		}	
		elapsed /= 1e9;
		
		genstats.average();
		
		options.print(2, String.format("Total CPU time discounting I/O: %g s = %g iter/s\n",
				elapsed, (double)corpus.test[partition].length/elapsed));
		
		options.println(0,"** TEST Peformance: **");

		// Compute test statistics / efficiency vs. accuracy trade-off and log to file
		options.println(0,genstats.summarize(false));

		String logstr = bestEpoch + ",-1,-1," + ArrayUtil.joinDoubleFields(genstats, GeneralizationStatistics.writeFields);
		trainingProgress.println(logstr);

		PrintWriter testout = new PrintWriter(corpus.getPartitionFilePrefix(partition, level) + "-test.txt");
		testout.println(genstats.summarize());
		testout.close();		
		
	}
	//.for (int t = 0; t < 30; t++) {


	/**
	 * Trains a filter for a given partition of the dataset at a given level.
	 */
	private static ModelWeights trainFilterPartition(Options options, CascadeModel[] models, int level, int partition) throws IOException, ClassNotFoundException {
		Corpus corpus = options.corpus;
		CascadeModel model = models[level];

		// TODO Auto-generated method stub
		options.println(1,"=====================================================================");
		options.println(0,"TRAINING Level: " + level + " Partition: " + corpus.partitionNames[partition]);
		options.println(1,models[level]);
		options.println(1,"=====================================================================");

		// initialize weights
		AveragingWeights w = new AveragingWeights(model.getNumberOfFeatures());
		Weights bestw = null;

		double bestalpha = -1;
		int bestEpoch = -1;

		int avgDenominator = 0;

		// run training pass
		corpus.switchToTrain(partition, level);

		SimpleLogger trainingProgress = new SimpleLogger(corpus.getPartitionFilePrefix(partition, level) +  "-train");
		trainingProgress.println("epoch, trainMistakes, wNorm," + ArrayUtil.join(GeneralizationStatistics.writeFields) +
				"," + ArrayUtil.join(FilterTradeoffStatistics.writeFields));

		double minSatisfyingEff = Double.MAX_VALUE;
		double minViolatingError = Double.MAX_VALUE;

		FilterTradeoffStatistics bestTradeoff = null;
		GeneralizationStatistics bestGenstats = null;

		for (Double alpha : model.trainingAlphas) {

			// run training
			for (int t = 0; t < options.trainEpochs; t++) {
				options.println(2,"Training epoch "+t+" for alpha="+alpha+" level="+level+" partition="+corpus.partitionNames[partition]);
				// run training pass
				corpus.switchToTrain(partition, level);

				double mistakes = 0;
				RunTimeEstimator est = new RunTimeEstimator(corpus.train[partition].length, options.trainUpdatePercentage);

				double elapsed = 0;
				while (corpus.hasMoreLattices()) {
					Lattice lattice = corpus.nextLattice();
					model.prepareForLattice(lattice);

					// run weight update procedure
					long startTime = System.nanoTime();

					mistakes += model.update.updateWeights(lattice, w, alpha.doubleValue());

					w.nextIteration();
					
					elapsed += System.nanoTime()-startTime;

					if (options.verbosity >= 2) 
						est.report();
				}
				elapsed /= 1e9; // convert nanoseconds to seconds

				options.println(1,"Finished training "+level+"-"+partition+"-"+alpha+"-"+t+": " + mistakes + " updates");
				options.print(2, String.format("Total train CPU time discounting I/O: %g s = %g iter/s\n",
						elapsed, (double)corpus.train[partition].length/elapsed));

				// run evaluation pass on development set
				corpus.switchToDevel(partition, level);					

				FilterTradeoffStatistics tradeoff = new FilterTradeoffStatistics();
				GeneralizationStatistics genstats = new GeneralizationStatistics(alpha); 

				// Average weights and compute norm
				w.average();
				double wNorm = w.getNorm();

				while (corpus.hasMoreLattices()) {
					Lattice lattice = corpus.nextLattice();
					model.prepareForLattice(lattice);

					model.addGeneralizationStats(lattice, w, genstats, alpha.doubleValue());							
					model.addTradeoffStats(lattice, w, tradeoff);
				}

				// Compute test statistics / efficiency vs. accuracy trade-off
				tradeoff.average();
				genstats.average();

				tradeoff.findBestAlpha(model.maxerr, 1.0); //alpha.doubleValue());

				String logstr = t + "," + mistakes + "," + wNorm + "," 
				+ ArrayUtil.joinDoubleFields(genstats, GeneralizationStatistics.writeFields) + ","
				+ ArrayUtil.joinDoubleFields(tradeoff, FilterTradeoffStatistics.writeFields);

				trainingProgress.println(logstr);

				options.println(2,"Peformance:");
				options.println(2,tradeoff.summarize() + genstats.summarize());

				if ( (tradeoff.bestErrUnderCap < model.maxerr && tradeoff.bestEffUnderCap < minSatisfyingEff) ||
						(tradeoff.bestErrUnderCap >= model.maxerr && tradeoff.bestErrUnderCap < minViolatingError &&
								minSatisfyingEff == Double.MAX_VALUE) ) {

					options.println(2,"** Best weights so far **");
					bestw = w.getCopy();
					bestalpha = tradeoff.bestAlpha;
					bestEpoch = t;

					if (tradeoff.bestErrUnderCap < model.maxerr)
						minSatisfyingEff = tradeoff.bestEffUnderCap;
					else
						minViolatingError = tradeoff.bestErrUnderCap;

					bestGenstats = genstats;
					bestTradeoff = tradeoff;
				}

				w.unaverage();

				if (mistakes == 0)
					break;
			}
		}

		if (bestalpha == -1)
			throw new RuntimeException("Alpha was never chosen!!");

		
		if (options.verbosity > 0) {
			options.println(1,"** DEVEL SET Performance: (best epoch = " + bestEpoch + ")");
			options.println(1,bestTradeoff.summarize() + bestGenstats.summarize());
		}
		
		// save model weights
		String wfname = corpus.getPartitionFilePrefix(partition, level) + "-weights";
		options.print(1, String.format("Saving weights to file: '%s'\n", wfname));
		ObjectOutputStream wout = new ObjectOutputStream(new FileOutputStream(wfname));
		wout.writeObject(bestw);
		wout.writeDouble(bestalpha);
		wout.close();
		return new ModelWeights(bestalpha, bestw, bestEpoch);
	}

	/////////////////////////////////////////////////////////
	// compute lattices for next model
	/**
	 * Computes the lattices for the level of the cascade for a given partition.
	 * @param isFullPartition
	 * whether or not this is the test partition. If not, then the true states/edges are always included
	 * in the next level of the partition.
	 */
	private static void computeLatticesForNextLevel(Options options, CascadeModel[] models, int level, int partition, 
			boolean isFullPartition, ModelWeights w) throws IOException, ClassNotFoundException {
		Corpus corpus = options.corpus;
		CascadeModel model = models[level];		
		corpus.switchToTest(partition, level);

		// also compute test-set performance stats 
		// (but only for full partition...test set performance is meaningless for others)

		FilterTradeoffStatistics tradeoff = new FilterTradeoffStatistics();
		GeneralizationStatistics genstats = new GeneralizationStatistics(w.alpha); 

		if (isFullPartition)
			models[level+1].featureAlphabet.stopGrowth(); // ensure no test-set features are improperly added 

		RunTimeEstimator est = new RunTimeEstimator(corpus.test[partition].length, options.testUpdatePercentage);
		corpus.openNewLatticeCache(partition, level+1);
		double elapsed = 0;
		while (corpus.hasMoreLattices()) {

			Lattice lattice = corpus.nextLattice();
			model.prepareForLattice(lattice);

			long startTime = System.nanoTime();

			boolean isFutureTrainingSet = !isFullPartition;

			boolean [] mask = model.computeFilterMask(lattice, w.weights, w.alpha, isFutureTrainingSet);

			Lattice newLattice = models[level+1].expandLattice(lattice, mask);

			if (isFullPartition && level == 1) {
				
				for (int i = 0; i < 5; i++)
					System.out.println(w.weights.score(lattice.fv[i]));
			
				System.out.println(((NOrderPOS)model).viterbi);
				
				//lattice.edgeScores = ArrayUtil.ensureCapacity(lattice.edgeScores, lattice.fv.length);
				//model.scoreLatticeEdges(w, lattice);
				((NOrderPOS)model).computeEdgeMarginals(lattice, w.weights);
				
				for (int i = 0; i < 5; i++)
					System.out.println(lattice.edgeScores[i]);
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).marginalVals[i]);
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).alphaVals[i]);
				for (int i = 0; i < 5; i++)
					System.out.println(((NOrderPOS)model).betaVals[i]);
								
				
				System.out.println(w.weights.hashCode());
				System.out.println(lattice.maxEdgeScore);
				System.out.println(lattice.meanEdgeScore);
				
				//System.out.println(models[level+1].featureAlphabet.toString());
				System.out.println(w.weights.hashCode());
				
				model.addGeneralizationStats(lattice, w.weights, genstats, w.alpha);
				genstats.average();
				System.out.println(w.weights.w.length);				
				System.out.println(genstats.summarize());
				
				for (int i = 0; i < 5; i++)
					System.out.println(w.weights.score(lattice.fv[i]));
				
				//lattice.print();
				System.exit(1);
			}

			elapsed += System.nanoTime()-startTime;

			corpus.saveLatticeToCache(newLattice);

			if (isFullPartition) {
				model.addGeneralizationStats(lattice, w.weights, genstats, w.alpha);
				model.addTradeoffStats(lattice, w.weights, tradeoff);
			}
			if (options.verbosity > 1)
				est.report();
		}	
		elapsed /= 1e9;
		options.print(2, String.format("Total CPU time discounting I/O: %g s = %g iter/s\n",
				elapsed, (double)corpus.test[partition].length/elapsed));

		corpus.closeNewLatticeCache();

		if (isFullPartition) {

			options.println(0,"** TEST Peformance: **");

			// Compute test statistics / efficiency vs. accuracy trade-off and log to file
			tradeoff.average(); genstats.average();
			
			options.print(2, String.format("looking for min with err cap: %g, maxalpha %g\n", model.maxerr, w.alpha));
			tradeoff.findBestAlpha(model.maxerr, w.alpha);

			options.println(0,tradeoff.summarize() + genstats.summarize());

			String logstr = w.epoch + ",-1,-1," + ArrayUtil.joinDoubleFields(genstats, GeneralizationStatistics.writeFields) + ","
			+ ArrayUtil.joinDoubleFields(tradeoff, FilterTradeoffStatistics.writeFields);
			SimpleLogger develLog = new SimpleLogger(corpus.getPartitionFilePrefix(partition, level) +  "-devel");
			develLog.println(logstr);

			
			PrintWriter testout = new PrintWriter(corpus.getPartitionFilePrefix(partition, level) + "-test.txt");
			testout.println("Level " + level + " TEST Performance:");
			testout.println(genstats.summarize());
			testout.close();
			
			// by assumption, the very last partition generates features for the development set  
			corpus.switchToDevel(partition, level);
			corpus.openNewLatticeCache(partition, level+1);
			while (corpus.hasMoreLattices()) {
				Lattice lattice = corpus.nextLattice();
				model.prepareForLattice(lattice);
				boolean [] mask = model.computeFilterMask(lattice, w.weights, w.alpha, false);
				Lattice newLattice = models[level+1].expandLattice(lattice, mask);
				corpus.saveLatticeToCache(newLattice);
			}
			corpus.closeNewLatticeCache();
		}


	}


	/**
	 * Make sure that the first level of the lattices exist on disk (or in ram if they should be in ram). 
	 */
	private static void ensureFirstLevelLattices(Options options, CascadeModel models []) throws IOException, ClassNotFoundException {
		Corpus corpus = options.corpus;			
		if (options.precomputeFirstOnlyIfNonExistent && !options.alwaysPrecomputeFirst) {
			File latpath = new File(corpus.getPartitionFilePrefix(0, 0) + "-test-lattices");				
			if (!latpath.exists()) {
				System.out.printf("file: %s does not exist, so precomputing lattices.\n", latpath.getAbsolutePath());
				corpus.precomputeLattices(models[0]);					
			} else
				System.out.printf("file: %s exists, so precomputing lattices is not necessary.\n", latpath.getAbsolutePath());

		} else 
			corpus.precomputeLattices(models[0]);

		if (options.loadFirstIntoRAM)
			corpus.readInLatticesToRAM(0);

		String modelfname = corpus.getModelFilename(0);

		options.println(0,"Saving model to file: " + modelfname);
		ObjectWriter.writeOneObject(modelfname, models[0]);
	}


}
