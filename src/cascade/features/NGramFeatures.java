package cascade.features;

import cascade.io.SentenceInstance;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.programs.Options;

/**
 * Generates 1 feature for each ngram in a model.
 *
 */
public class NGramFeatures extends FeatureGenerator {

	public NGramFeatures() {}

	@Override
	public void computeFeatures(CascadeModel m, SentenceInstance inst,
			int pos, int edge) {
		
		NOrderPOS model = (NOrderPOS)m;
		setWorkingAlphabet(model.featureAlphabet);
		
		add( (model.order+1) + "GRAM=" + model.ngramToString(edge, model.order+1) );
		
	}
	
	

	@Override
	public void init(Options opts) {}

//	@Override
//	public void computePositionFeatures(CascadeModel model,
//			DependencyInstance inst, int pos) {
//		// TODO Auto-generated method stub
//		super.computePositionFeatures(model, inst, pos);
//	}	

}
