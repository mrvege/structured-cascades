package cascade.features;

import cascade.io.SentenceInstance;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.programs.Options;

/**
 * Generates features for each NGram in a model.
 * 
 * Breaks down an input word into pixels and also generates features for activation
 * or deactivation of each pixel in the input image. 
 *
 */
public class NGramOCRFeatures extends FeatureGenerator {

	public NGramOCRFeatures() {}

	@Override
	public void computeFeatures(CascadeModel m, SentenceInstance inst,
			int pos, int ngramid) {

		NOrderPOS model = (NOrderPOS)m;
		setWorkingAlphabet(model.featureAlphabet);
		
		add( (model.order+1) + "GRAM=" + model.ngramToString(ngramid, model.order+1) );

		if (pos < (inst.forms.length-1)) {
		  char [] word = inst.forms[pos+1].toCharArray();
		    
		  StringBuilder sb = new StringBuilder();
		  
		  // FIXME -- is it OK to pass null for OCR?
		  int state = model.computeTagFromNGramID(null, model.order+1, ngramid, 0);
		  
		  sb.append("L=").append(model.POSAlphabet.reverseLookup(state)).append("&");
		  add(sb.toString());
		  
		  int start = sb.length();
		  
		  for (int i = 2; i < word.length; i++) {
		    add(sb.append("p" + i + "=" + word[i]).toString());
		    sb.delete(start, sb.length());
		  }
		}
	}

	@Override
	public void init(Options opts) {}

}
