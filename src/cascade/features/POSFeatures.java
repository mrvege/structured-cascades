package cascade.features;

import java.util.regex.Pattern;

import cascade.io.SentenceInstance;
import cascade.io.Sequence;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.programs.Options;
import cascade.util.Alphabet;
import cascade.util.CountingAlphabet;

public class POSFeatures extends FeatureGenerator {

	int wordCutoff = 5;
	int wcWordCutoff = 2000;
	int ixCutoff = 100;
	CountingAlphabet wordCounts;
	CountingAlphabet ixCounts;
	
	String rareKey = "<RARE>"; 
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	public int maxPrefixLength = 5;
	
	private Pattern patNumbers, patSymbols, patCapital;
	private StringBuilder sb;
	
	public POSFeatures() {		
		wordCounts = new CountingAlphabet();
		ixCounts = new CountingAlphabet();
		patNumbers = Pattern.compile("\\d");
		patSymbols = Pattern.compile("[^\\w]");
		patCapital = Pattern.compile("[A-Z]");
		sb = new StringBuilder();
	}


	@Override
	public void computePositionFeatures(CascadeModel model,	SentenceInstance inst, int pos) {	

		setWorkingAlphabet(model.featureAlphabet);

		String [] words = inst.forms;
		
		sb.delete(0, sb.length());
		
		int start = sb.length();
		
		for (int t = -2; t < 2; t++) {
			
			int offset = pos+t+1;
			
			String word = (offset < 1 || offset >= words.length) ? "<NULLWORD>" : words[offset];
			
			if (word != "<NULLWORD>" && wordCounts.getCount(word) <= wordCutoff)
				sb.append("WORD").append(t).append("=").append(rareKey);
			else 
				sb.append("WORD").append(t).append("=").append(word.toLowerCase());
			
			add(sb.toString());
			sb.delete(start, sb.length());
		}
		
		String word = words[pos+1];
		String prefix, suffix;
		int L = word.length();
		for (int p = 0; p < maxPrefixLength; p++) {
			if (p >= L) break;
			
			prefix = word.substring(0, p+1);
			int id = ixCounts.lookupIndex(prefix);
			if (ixCounts.getCount(prefix) > ixCutoff);
				add(sb.append("PRE").append(p).append("=").append(prefix));
			sb.delete(start, sb.length());
			
			suffix = word.substring(L-p-1, L);
			if (ixCounts.getCount(suffix) > ixCutoff);
				add(sb.append("SUF").append(p).append("=").append(suffix));
			sb.delete(start, sb.length());
		}	
		sb.delete(start, sb.length());
		
		
		// word shape features
		if (patNumbers.matcher(word).find()) {
			add(sb.append("NUM"));
			sb.delete(start, sb.length());
		}
		if (patCapital.matcher(word).find()) {
			add(sb.append("CAP"));
			sb.delete(start, sb.length());
		}
		if (patSymbols.matcher(word).find()) {
			add(sb.append("SYM"));
			sb.delete(start, sb.length());
		}
		
		// add constant (conditioned = tag)
		add(sb.append("TAG"));

	}
	
	public void addWordConditionalFeatures(SentenceInstance inst, int pos, int len) {
		sb.append("&W=");
		String [] words = inst.forms;
		for (int t = 1-len; t <= 0; t++) {
			int offset = pos+t+1;
			String word = (offset < 1 || offset >= words.length) ? "<NULLWORD>" : words[offset].toLowerCase();
						
			if (wordCounts.getCount(word.toLowerCase()) > wcWordCutoff) {
				sb.append(word).append(" ");
			} else {
				sb.append("* ");
			}
		}
		
	}
	public void addNGramFeatures(NOrderPOS m, SentenceInstance inst, int pos, int order, int ngramID) {
		sb.delete(0, sb.length());

		add(sb.append(order + "GRAM=").append(m.ngramToString(ngramID, order)));		
		addWordConditionalFeatures(inst, pos, order);
		add(sb);
		sb.delete(0, sb.length());
		
		// Add wildcard features
		if (order > 2 && pos >= (order-1)) {
			String tag0 = m.POSAlphabet.reverseLookup(m.computeTagFromNGramID(order, ngramID, 0));
			for (int i = 2; i < order; i++) {

				sb.delete(0, sb.length());
				String stars = "";
				for (int j = 1; j < i; j++) {
					stars += "*";
				}
				
				add(sb.append("WC=").append(m.POSAlphabet.reverseLookup(m.computeTagFromNGramID(order, ngramID, i))).append(stars).append(tag0));
				addWordConditionalFeatures(inst, pos, i+1);
				add(sb);
			}
		}
	}
	
	@Override
	public void computeFeatures(CascadeModel model, SentenceInstance inst,
			int pos, int ngramID, int order) {
		NOrderPOS m = (NOrderPOS)model;
		setWorkingAlphabet(m.featureAlphabet);

		if (order < m.order)
			addNGramFeatures(m, inst, pos, order, m.computeLowerOrderNGramID(order, ngramID));
		else
			addNGramFeatures(m, inst, pos, order, ngramID);
	}

	@Override
	public void computeEdgeFeatures(CascadeModel model, SentenceInstance inst,
			int pos, int s1, int s2) {
		NOrderPOS m = (NOrderPOS)model;
		setWorkingAlphabet(m.featureAlphabet);

		int ngram = (int) m.computeNGramIDFromEdge(s1, s2);
		addNGramFeatures(m, inst, pos, m.order+1, ngram);	// 
	}
	

	@Override
	public void init(Options opts) {
		System.out.println(this.getClass().getCanonicalName() + ":" + "counting word occurences"); 
		for(Sequence s: opts.corpus.getTrainSequences()) {
			  
			String [] words = s.getInstance().forms;
			
			for (int pos = 1; pos < (words.length-1); pos++) {
				String w = words[pos];
				wordCounts.lookupIndex(w.toLowerCase());
				
				int L = w.length();
				for (int p = 0; p < maxPrefixLength; p++) {
					if (p >= L) break;
					ixCounts.lookupIndex(w.substring(0, p+1));
					ixCounts.lookupIndex(w.substring(L-p-1, L));
				}	
			}
		}
	}
	
}
