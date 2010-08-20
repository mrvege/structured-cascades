package cascade.features;

import gnu.trove.THashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectShortHashMap;
import gnu.trove.TPrimitiveHash;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Pattern;

import cascade.io.Corpus;
import cascade.io.SentenceInstance;
import cascade.io.Sequence;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.programs.Options;
import cascade.util.ArrayUtil;

/**
 * Generates features based on word identity (before and after given position) and a few basic word
 * shape features. Suitable for POS tagging.
 *
 */
public class NGramPOSFeatures extends FeatureGenerator implements Serializable {
	
	public int numFrequentWords = 500;
	TObjectIntHashMap<String> conditionWords;
	
	private Pattern patNumbers, patSymbols, patCapital;
	private StringBuilder sb;
	
	public NGramPOSFeatures() {		
		patNumbers = Pattern.compile("\\d");
		patSymbols = Pattern.compile("[^\\w]");
		patCapital = Pattern.compile("[A-Z]");
		sb = new StringBuilder();
	}

	@Override
	public void init(Options opts) {
		System.out.println(this.getClass().getCanonicalName() + ":" + "counting word occurences"); 
		
		TObjectDoubleHashMap<String> wordCounts = new TObjectDoubleHashMap<String>();
		
		Corpus c = opts.corpus;
		for(Sequence s: c.train[c.train.length-1]){
			  
			String [] words = s.getInstance().forms;
			
			for (int pos = 1; pos < (words.length-1); pos++) {
				String w = words[pos].toLowerCase();
				if (wordCounts.contains(w))
					wordCounts.increment(w);
				else
					wordCounts.put(w,1);
			}
		}
		
		String words[] = wordCounts.keys(new String[0]);
		double counts[] = wordCounts.getValues();
		
		ArrayUtil.sortByDoubleValues(counts, words);
//		for (int i = 0; i < numFrequentWords; i++)
//			System.out.println(words[i] + ": " + counts[i]);

		String[] freqWords = Arrays.copyOf(words, numFrequentWords);
		System.out.println(Arrays.toString(freqWords));
		
		conditionWords = new TObjectIntHashMap<String>();
		for (String word : freqWords)
			conditionWords.put(word, 1);

	}
	
	@Override
	public void computeFeatures(CascadeModel m, SentenceInstance inst,
			int pos, int edge) {
		
		NOrderPOS model = (NOrderPOS)m;
		setWorkingAlphabet(model.featureAlphabet);
		
		// condition on common words
		String [] words = inst.forms;

		sb.delete(0, sb.length());
		sb.append((model.order+1) + "GRAM=" + model.ngramToString(edge, model.order+1));
		
		add(sb);
		
		sb.append("&");
		int start = sb.length();
		
		for (int t = -model.order; t <= 0; t++) {
			
			int offset = pos+t+1;
			
			boolean nullword = (offset < 1 || offset >= words.length);
			
			String word = nullword ? "<NULLWORD>" : words[offset]; 
					
			String wordstr = (conditionWords.contains(word.toLowerCase()) || nullword) ? word.toLowerCase() : "<uncommon>"; 
			
			sb.append("WORD").append(t).append("=").append(wordstr);
			add(sb.toString());
			sb.delete(start, sb.length());
			
			if (!nullword) {
				if (patNumbers.matcher(word).find()) {
					add(sb.append("NUM").append(t));
					sb.delete(start, sb.length());
				}
				if (patCapital.matcher(word).find()) {
					add(sb.append("CAP").append(t));
					sb.delete(start, sb.length());
				}
				if (patSymbols.matcher(word).find()) {
					add(sb.append("SYM").append(t));
					sb.delete(start, sb.length());
				}
			}
		}
		
		// wildcards: note, these would be read right to left
		
		sb.delete(0,sb.length());
		
		if (model.order > 1) {
			sb.append("WC=").append(model.POSAlphabet.reverseLookup(model.computeTagFromNGramID(null, model.order+1, edge, 0)));
			
			String wordstr1 = (conditionWords.contains(words[pos+1].toLowerCase())) ? words[pos+1].toLowerCase() : "<uncommon>";

			start = sb.length();
			for (int j = 2; j < (model.order+1); j++) {
				
				for (int i = 1; i < j; i++)
					sb.append('*');
				
				sb.append(model.POSAlphabet.reverseLookup(model.computeTagFromNGramID(null, model.order+1, edge, j)));
				int offset = pos-j+1;
				boolean nullword = (offset < 1 || offset >= words.length);
				String word = nullword ? "<NULLWORD>" : words[offset]; 

				String wordstr2 = (conditionWords.contains(word.toLowerCase()) || nullword) ? word.toLowerCase() : "<uncommon>"; 

				add(sb.append("&WORD1=").append(wordstr1));
				add(sb.append("&WORD2=").append(wordstr2));
				sb.delete(start, sb.length());
			}
		}
		sb.delete(0,sb.length());

	}	

	@Override
	public void computePositionFeatures(CascadeModel model,	SentenceInstance inst, int pos) {	

		setWorkingAlphabet(model.featureAlphabet);

		NOrderPOS m = (NOrderPOS)model;
		String [] words = inst.forms;
		
		sb.delete(0, sb.length());
		
		int start = sb.length();
		
		for (int t = -m.order; t <= 0; t++) {
			
			int offset = pos+t+1;
			String word = (offset < 1 || offset >= words.length) ? "<NULLWORD>" : words[offset]; 
					
			if (conditionWords.contains(word.toLowerCase())) {
				sb.append("WORD").append(t).append("=").append(word.toLowerCase());
			
				add(sb.toString());
				sb.delete(start, sb.length());
			}
			if (patNumbers.matcher(word).find()) {
				add(sb.append("NUM").append(t));
				sb.delete(start, sb.length());
			}
			if (patCapital.matcher(word).find()) {
				add(sb.append("CAP").append(t));
				sb.delete(start, sb.length());
			}
			if (patSymbols.matcher(word).find()) {
				add(sb.append("SYM").append(t));
				sb.delete(start, sb.length());
			}
			
		}
		
//		String word = words[pos+1];
//		int L = word.length();
//		for (int p = 0; p < maxPrefixLength; p++) {
//			
//			
//			if (p >= L) break;
//			
//			sb.append("PRE").append(p).append("=").append(word.substring(0, p+1));
//
//			add(sb.toString());
//			sb.delete(start, sb.length());
//			
//			sb.append("SUF").append(p).append("=").append(word.substring(L-p-1, L));
//			
//			add(sb.toString());
//			sb.delete(start, sb.length());
//		}	
//		sb.delete(start, sb.length());
		
		// word shape features
	
		// add constant (conditioned = tag)
		add(sb.append("GRAM"));

	}

//	@Override
//	protected void add(String f) {
//		if (computeOnly)
//			System.out.println(f);
//		super.add(f);
//	}



}
