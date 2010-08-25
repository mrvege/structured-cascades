package cascade.io;


import cascade.util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cascade.lattice.Lattice;
import cascade.model.CascadeModel;
import cascade.programs.Options;

/**
 * Manages all aspects of reading and writing the dataset and Lattice files.
 * 
 * It will automatically partition the training set into folds so that training
 * can be jacknifed to avoid overfitting one level of the cascade on the output
 * of the previous level of the cascade.
 * 
 */
/**
 * @author djweiss
 *
 */
public class Corpus {
	
	public static Options options;
	
	/** number of folds to do in jackknifing. WARNING: is not tested with any value other than 3. */
	public int numJackKnives = 3; 
	
	/**
	 * Discard sentences from the datasets above this sentence length
	 */
	private int maxSentenceLength = Integer.MAX_VALUE;
	
	/**
	 * Discard sentences from the datasets below this sentence length  
	 */
	private int minSentenceLength = 2;
	
	/**
	 * Maximum size of the training set (will stop reading examples after this #)
	 */
	public int maxTrainingSize = Integer.MAX_VALUE;
	
	/**
	 * Maximum size of the testing set (will stop reading example safter this #) 
	 */
	public int maxTestingSize = Integer.MAX_VALUE;
		
	/** 
	 * src is the <i>prefix</i> of the source files that we read in. The actual files should
	 * be named <i>prefix</i>_train.txt and <i>prefix</i>prefix_test.txt 
	 * and these are automatically appended to make the files we actually read. 
	 */
	public String src;
	
	/**
	 * the output prefix; all output from the program will be subdirectories of this folder.
	 * 
	 */
	public String prefix;
	
	/**
	 * fraction of the training set that is set aside for development data
	 */
	public double develFraction = 0.1;
	
	/**
	 * Writes lattice files to RAM instead of to disk. This may speed up some application dramatically,
	 * at the cost of much higher memory usage. 
	 */
	public boolean writeToRAM = false;
	
	/**
	 * Compress data before writing to disk (or RAM) to conserve space usage. 
	 */
	public boolean useCompression = false;
	
	/**
	 * Keeps track of which files are using which output streams. 
	 */
	public HashMap<String,FastByteArrayOutputStream> streamsBuffer;
	
	public Sequence[][] train;
	public Sequence[][] test;
	public Sequence[] devel; // development is the same for all levels. 
	
	/**
	 * Stores the names of the jacknife partitions. 
	 */
	public String[] partitionNames;
	
	/**
	 * Whether or not to store the original level of lattices (computed before any training is run) in RAM,
	 * or on disk. May save time at increased memory usage to store in RAM.
	 */
	public boolean storeBaseLatticesInRAM = false;
	
	public Lattice[][] testLattice;
	public Lattice[] develLattice;

	public Corpus() {
		
	}
	
	/**
	 * create train[] devel[] and test[] for the jack knife as well as a final one by reading the 
	 * files at src+"_train.txt" and src+"_test.txt". 
	 * @param src the prefix of the source data location
	 * @param prefix the prefix of the output data location
	 * @throws IOException
	 */
	public Corpus(String src, String prefix) throws IOException {
		this.src = src;
		this.prefix = prefix;
	}
	
	/**
	 * Initializes the corpus based on current parameter settings.
	 * 
	 * Note that if you don't override options in a .fig file, you may lose
	 * knowledge of what the default parameters were at the time you ran the experiment. 
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException{
		// FIXME: we're currently assuming that the input data file is randomly sorted -- right?
		// maybe we should randomize its order to make the training work
		Sequence[] fullTrain = readFile(src+"_train.txt", maxTrainingSize);
		// development is first numDevel sequences of fullTrain, for all partitions. 
		int numDevel = (int)((fullTrain.length)*develFraction);
		devel = new Sequence[numDevel];
		System.arraycopy(fullTrain, 0, devel, 0, numDevel);
		Sequence[] newFullTrain = new Sequence[fullTrain.length - numDevel];
		System.arraycopy(fullTrain, numDevel, newFullTrain, 0, newFullTrain.length);
		fullTrain = newFullTrain;
		// done with development.  
		train = new Sequence[numJackKnives+1][];
		test = new Sequence[numJackKnives+1][];
		Sequence[][] tmp = splitIntoEvenParts(fullTrain, numJackKnives);
		// create the jack knife data sets; this means we copy the pointers to the Sequence objects. 
		for (int i = 0; i < numJackKnives; i++) {
			// add all the training examples excluding the ones from tmp[i];
			test[i] = tmp[i];
			train[i] = new Sequence[fullTrain.length-tmp[i].length];
			int trainInd = 0;
			for (int j = 0; j < tmp.length; j++) {
				if (i==j) continue;
				for (int k = 0; k < tmp[j].length; k++) {
					train[i][trainInd++] = tmp[j][k];
				}
			}
			
		}
		
		// create the final jack knife which has the full data
		test[numJackKnives] = readFile(src+"_test.txt", maxTestingSize);
		train[numJackKnives] = new Sequence[fullTrain.length];
		int trainInd = 0;
		for (int j = 0; j < tmp.length; j++) {
			for (int k = 0; k < tmp[j].length; k++) {
				train[numJackKnives][trainInd++] = tmp[j][k];
			}
		}
		partitionNames = makePartitions();
		
		File directory = new File(getDnameFor(0));
		if (directory.exists() && directory.isDirectory())
			System.out.println("Overwriting contents in directory "+directory.getAbsolutePath());
		else{
			System.out.println("Creating directory "+directory.getAbsolutePath());
			boolean success = directory.mkdirs();
			if(!success) throw new IOException("Failed to create directory!");
		}
		
	}
	
	
	/**
	 * Take a sequence and split it into approximately even parts. 
	 * @param fullTrain
	 * @param numParts
	 * @return
	 */
	private Sequence[][] splitIntoEvenParts(Sequence[] fullTrain, int numParts) {
		Sequence[][] parts = new Sequence[numParts][];
		int[] lengths = new int[numParts];
		int j = 0;
		for (int i = 0; i < fullTrain.length; i++) {
			lengths[j]+=1;
			j = (j+1)%numParts;
		}
		for (int i = 0; i < lengths.length; i++) {
			parts[i] = new Sequence[lengths[i]];
		}
		int[] used = new int[numParts];
		j = 0;
		for (int i = 0; i < fullTrain.length; i++) {
			parts[j][used[j]] = fullTrain[i];
			used[j]+=1;
			j = (j+1)%numParts;
		}
		return parts;
	}


	/**
	 * Reads a file into an array of {@link cascade.io.Sequence Sequence} objects.
	 */
	public static Sequence[] readFile(String fname, int maxInstances, int minSentenceLength, int maxSentenceLength) throws IOException {
		SentenceReader reader = new SentenceReader();
		reader.startReading(fname);
		SentenceInstance inst;
		int id = 0, numread = 0;
		ArrayList<Sequence> res = new ArrayList<Sequence>();
		System.out.println("Starting to read sequences from "+fname);
		while ( (inst = reader.getNext()) != null && numread < maxInstances) {

			// check that it matches length constraints
			if (inst.length()-1 < minSentenceLength || inst.length()-1 > maxSentenceLength)
				continue;
			
			numread++;
			
			Sequence seq = new Sequence(id++);
			seq.setInstance(inst);
			res.add(seq);
		}	
		reader.close();
		
		System.out.printf("Processed %d sequences.\n", numread);
		return res.toArray(new Sequence[res.size()]);
	}


	private Sequence[] readFile(String fname, int maxInstances) throws IOException {
		return readFile(fname, maxInstances, minSentenceLength, maxSentenceLength);
	}
	
	public  void setMaxSentenceLength(int maxSentenceLength) {
		this.maxSentenceLength = maxSentenceLength;
	}

	public  void setMinSentenceLength(int minSentenceLength) {
		this.minSentenceLength = minSentenceLength;
	}

	public  void setMaxTrainingSize(int maxTrainingSize) {
		this.maxTrainingSize = maxTrainingSize;
	}

	public  void setMaxTestingSize(int maxTestingSize) {
		this.maxTestingSize = maxTestingSize;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
		
	/**
	 * In order to do jackknife training, we need to split the data into several
	 * partitions.  These are accessed by their automatically generated names.  
	 * This method returns the names of partitions. The names are A,B,C...,BA,BB,...
	 * @return
	 */
	public String[] getPartitions(){
		return partitionNames;
	}
	
	/**
	 * Creates the names of all partitions to be used. 
	 */
	private String[] makePartitions() {
		String[] names = new String[numJackKnives+1];
		for (int i = 0; i < names.length-1; i++) {
			String s = "";
			int j = i;
			while(j>=26){
				s = ((char)((int)'A'+((j%26))))+s;
				j/=26;
			}
			s=(char)((int)'A'+(j%26))+s;
			names[i] = s;
		}
		names[numJackKnives] = "full";
		return names;
	}
	
	/**
	 * Computes and saves the Lattice objects corresponding to the lowest level
	 * in the cascade. 
	 * TODO: test this class
	 * @param cascadeModel
	 * @throws IOException 
	 */
	public void precomputeLattices(CascadeModel model) throws IOException {
		File directory = new File(getDnameFor(0));
		if (directory.exists() && directory.isDirectory())
			System.out.println("Overwriting contents in directory "+directory.getAbsolutePath());
		// FIXME: should we delete the contents just to be explicit that
		// we're overwriting them?
		else{
			System.out.println("Creating directory "+directory.getAbsolutePath());
			boolean success = directory.mkdirs();
			if(!success) throw new IOException("Failed to create directory!");
		}
		
		for (int partition = 0; partition < partitionNames.length; partition++) {
			if (storeBaseLatticesInRAM) {
				testLattice[partition] = new Lattice[test[partition].length];
				computeLattices(model, test[partition], testLattice[partition]);
			} else
				writeLattices(getFnameFor(partition, 0), model, test[partition]);
		}
		
		if (storeBaseLatticesInRAM) {
			develLattice = new Lattice[devel.length];
			computeLattices(model, devel, develLattice);	
		} else
			writeLattices(getDevNameFor(0), model, devel);
		
	}
	
	private void computeLattices(CascadeModel m, Sequence [] ls, Lattice [] l) {
		options.print(1, String.format("Computing %d NEW lattices to RAM\n", ls.length));
		RunTimeEstimator est = new RunTimeEstimator(ls.length, 0.25);
		for (int i = 0; i < ls.length; i++) {
			l[i] = m.createLattice(ls[i]);
			if (options.verbosity > 1)
				est.report();
		}
	}
	

	/**
	 * Creates a set of <i>initial</i> lattices from Sequence objects and writes them to file. 
	 */
	private void writeLattices(String fname, CascadeModel m, Sequence[] ls) throws FileNotFoundException, IOException{
		options.print(1, String.format("Writing out %d NEW lattices to '%s'\n", ls.length, fname));
		DataOutputStream out = getOutputStream(fname);
		RunTimeEstimator est = new RunTimeEstimator(ls.length, 0.25);
		for (int i = 0; i < ls.length; i++) {
			Lattice l = m.createLattice(ls[i]);
			l.write(out);
			if (options.verbosity > 1)
				est.report();
		}
		out.close();
	}

	/**
	 * a way to write to the current cache.  This is manipulated with the methods
	 * openNewLatticeCache, closeNewLatticeCache and saveLatticeToCache
	 */
	DataOutputStream latticeCacheOut = null;
	
	/**
	 * opens an output file corresponding to the test set of the given partition 
	 * and the given model level in the hierarchy.   
	 * @param partition
	 * @param level
	 * @throws IOException
	 */
	public void openNewLatticeCache(int partition, int level) throws IOException {
		
		if (currentlyReadingType == TrainDevTest.Train)
			throw new UnsupportedOperationException("Cannot open lattice cache for training set!!");
			
		File directory = new File(getDnameFor(level));
		if (!directory.exists()){
			options.println(1, "Creating directory "+directory.getAbsolutePath());
			boolean success = directory.mkdir();
			if(!success) throw new IOException("Failed to create directory!");
		}
		
		String fname = (currentlyReadingType == TrainDevTest.Test ? 
				getFnameFor(partition,level) : getDevNameFor(level));
		

		options.print(1, String.format("Opening new lattice cache '%s'\n", fname));
		latticeCacheOut = getOutputStream(fname);
	}
	
	/**
	 * Writes a single lattice to the <i>current</i> lattice being written to.
	 * 
	 */
	public void saveLatticeToCache(Lattice newLattice) throws IOException {
		newLattice.write(latticeCacheOut);
		
//		latticeCacheOut.writeObject(newLattice);
//		latticeCacheOut.reset(); // this means don't keep track of which objects have already been saved to avoid saving them again. 
	}	

	public void closeNewLatticeCache() throws IOException {
		latticeCacheOut.close();
		latticeCacheOut = null;
	}

	/** 
	 * the directory name corresponding to level
	 * @param level
	 * @return
	 */
	private String getDnameFor(int level){
		return prefix+File.separator+"l"+level+File.separator;
	}
	
	/**
	 * the location of the test lattices for the partition and level
	 * @param partition
	 * @param level
	 * @return
	 */
	private String getFnameFor(int partition, int level){
		return getDnameFor(level)+partitionNames[partition]+"-test-lattices";
	}

	/**
	 * the location of the test lattices for the partition and level
	 * @param partition
	 * @param level
	 * @return
	 */
	private String getDevNameFor(int level){
		return getDnameFor(level)+"devel-lattices";
	}
	
	enum TrainDevTest { Train, Dev, Test };
	TrainDevTest currentlyReadingType = null;
	
	/** the partition that we have been switched to using switchToTrain/Devel/Test */
	int currentlyReadingPartition = -1;
	
	/** the level that we have been switched to reading */
	int currentlyReadingLevel = -1;
	
	/** the partition corresponding to the current latticeCacheIn file */
	int latticeCacheInPartition = -1;
	
	/** the index in our seq array that corresponds to the lattice we are about to read */
	int currentlyReadingSeqIndex = -1;
	
	/** the index in our lattice array that corresponds to the lattice we are about to read */
	int currentlyReadingLatticeIndex = -1;
	
	DataInputStream latticeCacheIn = null;
	
	/**
	 * start reading the training portion of the set of lattices corresponding 
	 * to the current partition and level.  This requires opening the first
	 * file in the test portion.  As we move through the training portion for the
	 * current partition we will need to open test files from different 
	 * partitions.  Sort of a pain, but it will make the rest of the code simpler. 
	 * @param partition
	 * @param level
	 * @throws IOException 
	 * @throws  
	 */
	public void switchToTrain(int partition, int level) throws IOException {
//		if (currentlyReadingType!= null)  // TODO: should this be here?
//			throw new RuntimeException("I seem to be in the middle of something!");
		currentlyReadingType = TrainDevTest.Train;
		latticeCacheInPartition = partition==0 ? 1 : 0;
		latticeCacheIn = getInputStream(getFnameFor(latticeCacheInPartition, level));
		currentlyReadingPartition = partition;
		currentlyReadingLevel = level;
		currentlyReadingSeqIndex = 0;
		currentlyReadingLatticeIndex = 0;
	}
	
	/**
	 * Switch to reading the develoment data.  Since this is going to be half way through
	 * some partition's test file, it had better be the case that we have just finished doing:
	 * switchToTrain(); while(hasMoreLattices()) nextLattice();
	 * otherwise throw an exception. 
	 * @param partition ignored
	 * @param level
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void switchToDevel(int partition, int level) throws FileNotFoundException, IOException {
		currentlyReadingType = TrainDevTest.Dev;
		latticeCacheInPartition = partition;
		latticeCacheIn = getInputStream(getDevNameFor(level));
		currentlyReadingPartition = partition;
		currentlyReadingLevel = level;
		currentlyReadingSeqIndex = 0;
		currentlyReadingLatticeIndex = 0;
	}
	
	/**
	 * start reading the test partition for the given level.  Since we save these directly
	 * this is the simplest method. 
	 * @param partition
	 * @param level
	 * @throws IOException 
	 */
	public void switchToTest(int partition, int level) throws IOException {
		currentlyReadingType = TrainDevTest.Test;
		latticeCacheInPartition = partition;
		latticeCacheIn = getInputStream(getFnameFor(latticeCacheInPartition, level));
		currentlyReadingPartition = partition;
		currentlyReadingLevel = level;
		currentlyReadingSeqIndex = 0;
		currentlyReadingLatticeIndex = 0;
	}

		
	public boolean hasMoreLattices() {
		return currentlyReadingSeqIndex < getCurrentlyReadingSequences().length;
	}
	
	private Sequence[] getCurrentlyReadingSequences(){
		switch (currentlyReadingType) {
		case Dev: return devel; 
		case Train: return train[currentlyReadingPartition]; 
		case Test: return test[currentlyReadingPartition]; 
		}
		throw new RuntimeException("Unknown type for reading "+currentlyReadingType);
	}

	public void readInLatticesToRAM(int level) throws IOException, ClassNotFoundException {

		testLattice = new Lattice[partitionNames.length][];
		
		for (int partition = 0; partition < partitionNames.length; partition++) {
			switchToTest(partition, level);
			
			System.out.printf("loading lattices for partition '%s' into memory...\n", partitionNames[partition]);
			testLattice[partition] = new Lattice[test[partition].length];
			int i = 0;
			while (hasMoreLattices())
				testLattice[partition][i++] = nextLattice();

		}
		
		switchToDevel(0, level);
		System.out.printf("loading lattices for 'devel' into memory...\n");
		develLattice = new Lattice[devel.length];
		int i = 0;
		while (hasMoreLattices())
			develLattice[i++] = nextLattice();
		
		storeBaseLatticesInRAM = true;

	}
	
	public Lattice nextLocalLattice() {
		
		if (currentlyReadingType == TrainDevTest.Dev) {
			currentlyReadingSeqIndex++;
			return develLattice[currentlyReadingLatticeIndex++];
		}
		
		if (currentlyReadingLatticeIndex >= testLattice[latticeCacheInPartition].length) {
			if(currentlyReadingType != TrainDevTest.Train)
				throw new RuntimeException("ran out of lattices in file "
						+getFnameFor(latticeCacheInPartition, currentlyReadingLevel)
						+"\nwhile reading "+currentlyReadingType+" "+currentlyReadingPartition
						+"  hasMoreLattices says "+hasMoreLattices());
			
			latticeCacheInPartition +=1;
			if (latticeCacheInPartition == currentlyReadingPartition) latticeCacheInPartition ++;
			
			currentlyReadingLatticeIndex = 0;
		}
		currentlyReadingSeqIndex++;
		return testLattice[latticeCacheInPartition][currentlyReadingLatticeIndex++];
	}
	
	public Lattice nextLattice() throws IOException, ClassNotFoundException{
		
//		if (currentlyReadingLevel == 0 && this.storeBaseLatticesInRAM)
//			return nextLocalLattice();
//			
		// try reading an instance from the currently open cache
		Lattice res = null;
		try{
			res = Lattice.readLattice(latticeCacheIn);
			
			//res= (Lattice) latticeCacheIn.readObject();
		}
		catch (EOFException e){}
		if(res != null){
			if (res.seqHash != getCurrentlyReadingSequences()[currentlyReadingSeqIndex].hashCode())
				throw new IOException("Mis-matched sequence to lattice!");
			res.seq = getCurrentlyReadingSequences()[currentlyReadingSeqIndex];
			currentlyReadingSeqIndex++;
			return res;
		}
		else {
			// we have run out of lattices in the current test partition
			latticeCacheIn.close();
			latticeCacheIn = null;
			if(currentlyReadingType != TrainDevTest.Train)
				throw new RuntimeException("ran out of lattices in file "
						+getFnameFor(latticeCacheInPartition, currentlyReadingLevel)
						+"\nwhile reading "+currentlyReadingType+" "+currentlyReadingPartition
						+"  hasMoreLattices says "+hasMoreLattices());
			latticeCacheInPartition +=1;
			if (latticeCacheInPartition == currentlyReadingPartition) latticeCacheInPartition ++;
			latticeCacheIn = getInputStream(getFnameFor(latticeCacheInPartition, currentlyReadingLevel));
		}
		res = Lattice.readLattice(latticeCacheIn);
		res.seq = getCurrentlyReadingSequences()[currentlyReadingSeqIndex];

		if (res == null) throw new RuntimeException("next lattice failed!");
		currentlyReadingSeqIndex++;
		return res;
	}

	public String getCurrentOutputDir() {
		if (currentlyReadingLevel < 0)
			throw new RuntimeException("No output directory exists yet! level = -1");
		
		return prefix+File.separator+"l"+currentlyReadingLevel+File.separator;
	}
	
	public String getPartitionFilePrefix(int partition, int level) {
		return prefix+File.separator+"l"+level+File.separator + partitionNames[partition];
	}

	public String getModelFilename(int level) {
		return prefix+File.separator+"l"+level+File.separator+"model";
	}
	
	private DataInputStream getInputStream(String fname) throws IOException {
		
		InputStream base = null;
		
		if (writeToRAM)
			base = new BufferedInputStream(streamsBuffer.get(fname).getInputStream());
		else
			base = new BufferedInputStream(new FileInputStream(fname));
			
		if (useCompression)
			base = new GZIPInputStream(base);
		
		return new DataInputStream(base);
	}
	
	private DataOutputStream getOutputStream(String fname) throws FileNotFoundException, IOException{

		OutputStream base = null;
		
		if (writeToRAM) {
			if (streamsBuffer == null) 
				streamsBuffer = new HashMap<String, FastByteArrayOutputStream>();

			if (streamsBuffer.containsKey(fname))
				base = streamsBuffer.get(fname);
			else {
				FastByteArrayOutputStream out = new FastByteArrayOutputStream(10000 * 1024);
				streamsBuffer.put(fname, out);
				base = out;
			}
			
		} else {
			base = new BufferedOutputStream(new FileOutputStream(fname));
		}
		
		if (useCompression)
			base = new GZIPOutputStream(base);

		return new DataOutputStream(base);
	}
	

	

//	@Override
//	public void printSequence(Sequence seq) {
//
//		System.out.print("Forms: " + ((DependencyFeatures)seq.getFeatures()).toString());
//		System.out.print("Labels:");
//		for (int i = 0; i < seq.length(); i++)
//			System.out.print(" " + seq.getLabels()[i]);
//		System.out.println();
//	}
//	
//	public void printSequence(Sequence seq, PrintStream out) {
//
//		out.print("Forms: " + ((DependencyFeatures)seq.getFeatures()).getInstance().toString());
//		
//		if (seq.getOriginalLabels() != null) {
//			out.print("Original Labels:");
//			for (int i = 0; i < seq.length(); i++)
//				out.print(" " + seq.getOriginalLabels()[i]);
//			out.println();
//		}
//			
//		out.print("Labels:");
//		for (int i = 0; i < seq.length(); i++)
//			out.print(" " + seq.getLabels()[i]);
//		out.println();
//	}
//
//	public void printLattice(Sequence seq, DependencySequenceModel model, PrintStream out) {
//		
//		Lattice l = seq.getLattice();
//		DependencyInstance inst = ((DependencyFeatures)seq.getFeatures()).getInstance();
//		
//		for (int pos = 0; pos < l.length(); pos++) {
//			out.printf("Position %d: %s\n", pos, inst.forms[pos+1]);
//			for (int state : l.validStates(pos)) {
//				for (int offset = 0; offset < model.getOrder(); offset++) {
//					int head = model.getEmbeddedState(model.getOrder(), state, offset);
//
//					String form = "null";
//					if (head != model.nullState)
//						form = inst.forms[head];
//					out.printf("\t%d: head(%d) = %d (%s)\n", state, offset, head, form);
//				}
//			}
//		}
//		
//	}
	
//	public void printInstance(DependencyInstance instance) {
//		System.out.println("Current instance " + currentInstanceID);
//		
//		System.out.println("Form: " + Arrays.toString(instance.forms));
//		System.out.println("Heads: " + Arrays.toString(instance.heads));
//		System.out.println("POS Tags: " + Arrays.toString(instance.postags));
//		System.out.println("C POS Tags: " + Arrays.toString(instance.cpostags));
//		System.out.println("Labels: " + Arrays.toString(instance.deprels));
//		//System.out.println("Parse Tree: " + getParseTree(instance));
//		
//	}
//	
//	public void printMistakeHeader(DependencyInstance instance, PrintWriter out) {
//		
//		for (int i = 1; i < instance.forms.length-1; i++)
//			out.printf("%s\t", instance.forms[i]);
//		out.printf("%s\n", instance.forms[instance.forms.length-1]);
//		
//		for (int i = 1; i < instance.postags.length-1; i++)
//			out.printf("%s\t", instance.postags[i]);
//		out.printf("%s\n", instance.postags[instance.postags.length-1]);
//		
//		
//	}
//	public void printLabels(int len, int labels[], PrintWriter out) {
//	
//		for (int i = 0; i < len; i++) {
//			
//			for (int j = 0; j < len; j++)
//				out.printf("%d\t", (labels[i] == j) ? 1 : 0);
//			
//			out.printf("%d\n", (labels[i] == len) ? 1 : 0);
//		}
//		out.println();		
//		
//	}
//		
//	public String getParseTree(DependencyInstance instance){
//
//		int[] heads = instance.heads;
//		String[] labs = instance.deprels;
//
//		StringBuilder spans = new StringBuilder(heads.length*5);
//		for(int i = 1; i < heads.length; i++) {
//			spans.append(heads[i]).append("|").append(i).append(":").append(pipe.typeAlphabet.lookupIndex(labs[i])).append(" ");
//		}
//		return spans.substring(0,spans.length()-1);
//	}
}
