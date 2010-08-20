package cascade.io;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Object wrapping a SentenceInstance and associating with it some numeric labels.
 * 
 * Only used as a stepping stone between SentenceInstance and a {@link cascade.lattice.Lattice Lattice} object
 * for historical reasons.
 *
 */
public class Sequence implements Comparable<Sequence>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int id;	
	private int length;
		
	private int [][] labels;
	private SentenceInstance instance;
	
	public Sequence(int id) {
		this.id = id;
	}
	
	public int length() { return length; }
				
	public int[] getLabels(int order) { return labels[order]; }
	public int[] getOriginalLabels() { return labels[0]; }
	
	public void setLabels(int order, int [] newlabels) {
		assert(newlabels.length == labels[0].length);
		
		labels[order] = newlabels;
	}
	
	public int compareTo(Sequence s) {
		if (s.id == id) return 0;
		else if (s.id > id) return -1;
		else return 1;
	}
	
	/**
	 * A hash code that allows us to compare whether this instance
	 * is the same one as the one we're using for a particular lattice.
	 * @return a hash of the concatenation of all the forms of the instance. 
	 */
	public int hashCode(){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < instance.forms.length; i++) {
			sb.append(instance.forms[i]);
			sb.append(" ");
		}
		return sb.toString().hashCode();
	}
	
	public void print() {

		System.out.print("Forms: " + getInstance().toString());
		System.out.print("Tags: " + Arrays.toString(getInstance().postags));
		System.out.println();
		
	}

	public void setInstance(SentenceInstance instance) {
		this.instance = instance;
		this.length = instance.length()-1;
	}

	public SentenceInstance getInstance() {
		return instance;
	}

	public void printAll() {
		System.out.println("Sequence " + id + ":");
		SentenceInstance inst = getInstance();
		System.out.print("Forms: " + inst.toString());
		System.out.println("cpostags: " + Arrays.toString(inst.cpostags));
		
		System.out.print("Labels:");
		for (int i = 0; i < length(); i++)
			System.out.print(" " + getOriginalLabels()[i]);
		System.out.println();
		
		
	}

}
