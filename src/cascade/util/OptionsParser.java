package cascade.util;


import java.util.Arrays;
import java.util.List;

import cascade.io.Corpus;
import cascade.io.ObjectReader;
import cascade.model.CascadeModel;
import cascade.programs.Options;
import fig.FigLoader;

/**
 * Parses the command line and reads configuration files.
 * 
 * Command line arguments are as follows:
 * 
 * <ol>
 * <li><i>configfile</i> - The first argument is always the name of a configuration file. For information
 * on how to use these files, see the online documentation.</li>
 * <li><i>[suffix]</i> - If the second argument is a string, it is appended to the <b>src</b> and <b>prefix</b>
 * options of the Corpus object in the Options file. This allows a single config file to be used for multiple datasets.</li>
 * <li><i>[trainlevels]</i> - The final optional arguments are a sequence of numbers indicating which levels of the
 * cascade defined in the configuration file should be trained. All other levels are loaded from disk.</li>
 * </ol>
 * 
 * Thus, the command line arguments of <tt>ocr4gram.fig fold3 2 3</tt> would load the file <tt>ocr4gram.fig</tt> to find
 * parameters for the cascade, append <tt>fold3</tt> to the <b>src</b> and <b>prefix</b> parameters of the Corpus object, 
 * and load levels 0 and 1 of the cascade from disk instead of training them.
 *
 */
public class OptionsParser {
	
	private Options options;
	private CascadeModel[] models;
	private boolean[] trainModel;
	
	public OptionsParser(String args[]) throws Exception {
		this(args, true);
	}
	
	public OptionsParser(String args[], boolean doTrain) throws Exception {
		
		// Run the fig parser to get options
		List<Object> optslist = FigLoader.initObjects(args[0]);
		System.out.println("parsed " + optslist.size() + " objects.");
		
		options = (Options) optslist.get(0);
		
		Corpus corpus = options.corpus;
		
		models = new CascadeModel[optslist.size()-1];

		// parse arguments: if no integers are given, train ALL models; if any integers are given, train 
		// ONLY those models. also require that we train ALL levels after a given level if that level 
		// is supposed to be trained.
		
		int argStart = 1;
		if (args.length > argStart) {
			try {
				Integer.parseInt(args[argStart]);
			} catch (NumberFormatException e) {
				String suffix = args[argStart];
				System.out.println("Appending suffix " + suffix + " to corpus's src and prefix");
				corpus.src += suffix;
				corpus.prefix += suffix;
				argStart++;
			}
		}
		
		trainModel = new boolean[models.length];
		if (args.length == argStart && doTrain)
			for (int i = 0; i < trainModel.length; i++) trainModel[i] = true;
		else {
			for (int i = argStart; i < args.length; i++) {
				trainModel[Integer.parseInt(args[i])] = true;
			}
			boolean trained = false;
			for (boolean t : trainModel)
				if (t & !trained) trained = true;
				else if (!t & trained) throw new RuntimeException("Cannot train only the first part of a cascade");
		}
		
		corpus.init();
		Corpus.options = options;

		System.out.println("Training models: " + Arrays.toString(trainModel));
		
		for (int i = 0; i < models.length; i++) {
			// load model if it is not being trained OR the previous model was NOT trained (in order to get alphabet, etc.)
		  if ( (trainModel[i] || (i == 0 && options.alwaysPrecomputeFirst))  
			  && (i == 0 || trainModel[i-1])) {
				models[i] = (CascadeModel) optslist.get(i+1);
				models[i].init(options);
				
			} else {
				String filename = corpus.getModelFilename(i);
				System.out.println("Loading model from file: " + filename);
				models[i] = (CascadeModel) ObjectReader.readOneObject(filename, true);

				System.out.println(models[i]);
				// set to use whatever the Fig says as the update rule and featureGen 
				CascadeModel mFromFile = ((CascadeModel)optslist.get(i+1));
				models[i].update = mFromFile.update;
				if (mFromFile.featureGen != null) {
					models[i].featureGen = mFromFile.featureGen;
					models[i].featureGen.init(options);
					models[i].featureGen.setWorkingAlphabet(models[i].featureAlphabet);
				}
				models[i].generateLatticesOnly = mFromFile.generateLatticesOnly;
				models[i].trainingAlphas = mFromFile.trainingAlphas;
				models[i].maxerr = mFromFile.maxerr;
			}
		}
		
	}

	/**
	 * @return the Options argument from the configuration file
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * @return the model configurations from the configuration file
	 */
	public CascadeModel[] getModels() {
		return models;
	}

	/**
	 * @return an boolean array indicating whether or not each level should be trained or loaded from disk
	 */
	public boolean[] getDoTraining() {
		return trainModel;
	}

}
