package cascade.features;

import cascade.io.SentenceInstance;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.model.ZeroOrderPOS;
import cascade.programs.Options;

/**
 * Generates pixel features only, and no NGRAM features.
 *
 */
public class OCRFeatures extends FeatureGenerator {

	private static final long serialVersionUID = 1L;

	@Override
	public void computeFeatures(CascadeModel model, SentenceInstance inst,
			int pos, int ngramID, int order) {
		NOrderPOS m = (NOrderPOS)model;
		setWorkingAlphabet(m.featureAlphabet);

		if (order == 1)
			add( order + "GRAM=" + m.POSAlphabet.reverseLookup(m.computeTagFromNGramID(m.order, ngramID, 0)) );
		else if (order < m.order)
			add( order + "GRAM=" + m.ngramToString(m.computeLowerOrderNGramID(order, ngramID), order) );
		else
			add( order + "GRAM=" + m.ngramToString(ngramID, order));		
	}

	@Override
	public void computeEdgeFeatures(CascadeModel model, SentenceInstance inst,
			int pos, int s1, int s2) {
		NOrderPOS m = (NOrderPOS)model;
		setWorkingAlphabet(m.featureAlphabet);

		int ngram = (int) m.computeNGramIDFromEdge(s1, s2); 
		add( (m.order+1) + "GRAM=" + m.ngramToString(ngram, (m.order+1)) );
	}

//	@Override
//	public void computeFeatures(CascadeModel m, SentenceInstance inst, int pos, int state) {
//	
//		ZeroOrderPOS model = (ZeroOrderPOS)m;
//		setWorkingAlphabet(model.featureAlphabet);
//
//		char [] word = inst.forms[pos+1].toCharArray();
//		
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("L=").append(model.POSAlphabet.reverseLookup(state)).append("&");
//		add(sb.toString());
//		
//		int start = sb.length();
//
//		for (int i = 2; i < word.length; i++) {
//			add(sb.append("p" + i + "=" + word[i]).toString());
//			sb.delete(start, sb.length());
//		}
//	}

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
