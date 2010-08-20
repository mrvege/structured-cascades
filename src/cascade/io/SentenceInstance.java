package cascade.io;

import gnu.trove.*;
import java.io.*;
import java.util.*;
import cascade.features.FeatureVector;


/**
 * Class to represent the raw data associated with a given sentence: word forms, POS tags,
 *  and (optional, unused at this point) features.
 *
 */
public class SentenceInstance implements Serializable {

	
	// Data type:
	// ID FORM POSTAG FEATURES
	// Example:
	// 3  eles pron   M|3P|NOM
    // 
    //
    // 3  eles ele   pron       pron-pers M|3P|NOM 4    SUBJ   _     _
    // ID FORM LEMMA COURSE-POS FINE-POS  FEATURES HEAD DEPREL PHEAD PDEPREL
    //
    // We ignore PHEAD and PDEPREL for now. 


    // FORM: the forms - usually words, like "thought"
    public String[] forms;

    public String[] cpostags;
    
    // FINE-POS: the fine-grained part-of-speech tags, e.g."VBD"
    public String[] postags;

    // FEATURES: some features associated with the elements separated by "|", e.g. "PAST|3P"
    public String[][] feats;

    public SentenceInstance() {}

    public SentenceInstance(String[] forms, String[] postags, String[][] feats) {
		super();
		this.forms = forms;
		this.postags = postags;
		this.cpostags = postags;
		this.feats = feats;
//		
//		System.out.println("Forms: " + Arrays.toString(this.forms));
//		System.out.println("Tags: " + Arrays.toString(this.postags));
//		System.out.println("Feats: " + Arrays.toString(this.feats[0]));

	}

	public int length () {
    	return forms.length;
    }

    public String toString () {
    	StringBuffer sb = new StringBuffer();
    	sb.append(Arrays.toString(forms)).append("\n");
    	return sb.toString();
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
    	out.writeObject(forms);
    	out.writeObject(postags);
    	out.writeObject(feats);
    }


    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
    	forms = (String[])in.readObject();
    	postags = (String[])in.readObject();
    	feats = (String[][])in.readObject();
    }

}
