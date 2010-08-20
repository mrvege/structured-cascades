package cascade.features;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import cascade.model.CascadeModel;
import cascade.util.Alphabet;
import cascade.util.ArrayUtil;

public class FeatureVector{ //implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	
	int [] keys;
	double [] vals;
	
	public FeatureVector(int[] keys, double[] vals) {
		this.keys = keys;
		this.vals = vals;
	}

	public FeatureVector() {
		this.keys = null;
		this.vals = null;
	}

	public int [] getKeys () { return keys; }
	public double [] getValues() { return vals; }
	
	public int size() { return keys.length; }
	public int sizeBinary() { return keys.length - vals.length; }
	public int sizeDouble() { return vals.length; }

	
	private final int hash(int i, int max) { return (int) (((long)i*31L) % max); } 
	/**
	 * Score, but add offset to each key first, with weight mixing.
	 * @param w
	 * @param fac
	 * @return
	 */
	public double scoreMixed(double w[], int offset) {
		
		double s = 0;
		
		if (vals == null || vals.length == 0)  
			for (int i : keys) s += w[hash(i+offset,w.length)];
		else {
			for (int i = 0; i < keys.length; i++) 
				s += (i < vals.length ? vals[i]*w[hash(i+offset,w.length)] : w[i+offset]);
		}
		
		return s;
	}
	
	/**
	 * Increment, but add offset to each key first, with weight mixing
	 * @param w
	 * @param offset
	 * @param rate
	 */
	public void incrementMixed(double[] w, int offset, double rate) {
		
		if (vals == null) {
			for (int i = 0; i < keys.length; i++)
				w[hash(keys[i]+offset, w.length)] += rate;
		} else {
			
			for (int i = 0; i < keys.length; i++) {
				if (i < vals.length)				
					w[hash(keys[i]+offset,w.length)] += vals[i]*rate;
				else
					w[hash(keys[i]+offset, w.length)] += rate;
			}
		}		
	}
	/**
	 * Score, but add offset to each key first.
	 * @param w
	 * @param fac
	 * @return
	 */
	public double score(double w[], int offset) {
		
		double s = 0;
		
		if (vals == null || vals.length == 0)  
			for (int i : keys) s += w[i+offset];
		else {
			for (int i = 0; i < keys.length; i++) 
				s += (i < vals.length ? vals[i]*w[i+offset] : w[i+offset]);
		}
		
		return s;
	}
	
	/**
	 * Increment, but add offset to each key first.
	 * @param w
	 * @param offset
	 * @param rate
	 */
	public void increment(double[] w, int offset, double rate) {
		
		if (vals == null) {
			for (int i = 0; i < keys.length; i++)
				w[keys[i]+offset] += rate;
		} else {
			
			for (int i = 0; i < keys.length; i++) {
				if (i < vals.length)				
					w[keys[i]+offset] += vals[i]*rate;
				else
					w[keys[i]+offset] += rate;
			}
		}
		
	}
	
	public double score(double w[]) {
		
		double s = 0;
		
		if (vals == null || vals.length == 0)  
			for (int i : keys) s += w[i];
		else {
			for (int i = 0; i < keys.length; i++) 
				s += (i < vals.length ? vals[i]*w[i] : w[i]);
		}
		
		return s;
	}
	
	public void increment(double[] w, double rate) {
	
		if (vals == null) {
			for (int i = 0; i < keys.length; i++)
				w[keys[i]] += rate;
		} else {
			for (int i = 0; i < keys.length; i++) {
				double v = (i < vals.length ? vals[i] : 1.0);
				w[keys[i]] += v*rate;
			}
		}
		
	}
	
	public String toString(Alphabet a) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("[");
		for (int i = 0; i < keys.length; i++) {
			String v = (i < vals.length ? " = " + vals[i] : "");
			
			sb.append(a.reverseLookup(keys[i])).append(v);
			if (i < (keys.length - 1))
				sb.append(",");
			
		}
		sb.append("]");
		
		return sb.toString();
		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keys);
		result = prime * result + Arrays.hashCode(vals);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeatureVector other = (FeatureVector) obj;
		if (!Arrays.equals(keys, other.keys))
			return false;
		if (!Arrays.equals(vals, other.vals))
			return false;
		return true;
	}


	public void write(DataOutput out) throws IOException{
		out.writeLong(serialVersionUID);
		ArrayUtil.writeIntArray(out, keys);
		ArrayUtil.writeDoubleArray(out, vals);
	}
	
	public FeatureVector(DataInput in) throws IOException{
		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);
		keys = ArrayUtil.readIntArray(in);
		vals = ArrayUtil.readDoubleArray(in);
	}
	
//	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
//		long id=in.readLong();
//		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);
//		keys = ArrayUtil.readIntArray(in);
//		vals = ArrayUtil.readDoubleArray(in);
//	}
//
//
//	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//	
//		out.writeLong(serialVersionUID);
//		ArrayUtil.writeIntArray(out, keys);
//		ArrayUtil.writeDoubleArray(out, vals);
//		
//	}
}
