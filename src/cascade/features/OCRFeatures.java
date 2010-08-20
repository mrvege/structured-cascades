package cascade.features;

import cascade.io.SentenceInstance;
import cascade.model.CascadeModel;
import cascade.model.ZeroOrderPOS;
import cascade.programs.Options;

/**
 * Generates pixel features only, and no NGRAM features.
 *
 */
public class OCRFeatures extends FeatureGenerator {

	@Override
	public void computeFeatures(CascadeModel m, SentenceInstance inst,
			int pos, int state) {
	
		ZeroOrderPOS model = (ZeroOrderPOS)m;
		setWorkingAlphabet(model.featureAlphabet);

		char [] word = inst.forms[pos+1].toCharArray();
		
		StringBuilder sb = new StringBuilder();

		sb.append("L=").append(model.POSAlphabet.reverseLookup(state)).append("&");
		add(sb.toString());
		
		int start = sb.length();

		for (int i = 2; i < word.length; i++) {
			add(sb.append("p" + i + "=" + word[i]).toString());
			sb.delete(start, sb.length());
		}
	}

	@Override
	public void init(Options opts) {}

	@Override
	public void computePositionFeatures(CascadeModel model,
			SentenceInstance inst, int pos) {
		setWorkingAlphabet(model.featureAlphabet);

		StringBuilder sb = new StringBuilder();
		char [] word = inst.forms[pos+1].toCharArray();

		int start = sb.length();

		for (int i = 2; i < word.length; i++) {
			add(sb.append("p" + i + "=" + word[i]).toString());
			sb.delete(start, sb.length());
		}		
	}


	

}
