package cascade.features;

import java.util.regex.Pattern;

import cascade.io.SentenceInstance;
import cascade.model.CascadeModel;
import cascade.programs.Options;

public class ZeroOrderPOSFeatures extends FeatureGenerator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int maxPrefixLength = 5;
	
	private Pattern patNumbers, patSymbols, patCapital;
	private StringBuilder sb;
	
	public ZeroOrderPOSFeatures() {		
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
			
			sb.append("WORD").append(t).append("=").append(word.toLowerCase());
			
			add(sb.toString());
			sb.delete(start, sb.length());
		}
		
		String word = words[pos+1];
		int L = word.length();
		for (int p = 0; p < maxPrefixLength; p++) {
			
			
			if (p >= L) break;
			
			sb.append("PRE").append(p).append("=").append(word.substring(0, p+1));

			add(sb.toString());
			sb.delete(start, sb.length());
			
			sb.append("SUF").append(p).append("=").append(word.substring(L-p-1, L));
			
			add(sb.toString());
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


	@Override
	public void init(Options opts) {}
	
}
