package cascade.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

import cascade.features.FeatureVector;

public class ArrayUtil {

	static int GROWTH_FACTOR = 2;
	
	public static String [] ensureCapacity(String [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) {
			String [] buf = new String[GROWTH_FACTOR*newCapacity];
	
			if (x != null)
				for (int i = 0; i < x.length; i++)
					buf[i] = x[i];
			
			return buf;
		}
		return x;
	}

	public static int [] ensureCapacity(int [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) {
			int [] buf = new int[GROWTH_FACTOR*newCapacity];
			
			if (x != null)
				for (int i = 0; i < x.length; i++)
					buf[i] = x[i];
			
			return buf;
		}
		return x;
	}
	public static int [] ensureCapacityReset(int [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) {
			int [] buf = new int[GROWTH_FACTOR*newCapacity];
			return buf;
		}
		for (int i = 0; i < newCapacity; i++) x[i] = 0;
		
		return x;
	}
	
	public static boolean [] ensureCapacity(boolean [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) {
			boolean [] buf = new boolean[GROWTH_FACTOR*newCapacity];
			
			if (x != null)
				for (int i = 0; i < x.length; i++)
					buf[i] = x[i];
			
			return buf;
		}
		return x;
	}
	
	public static double [] ensureCapacity(double [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) {
			double [] buf = new double[GROWTH_FACTOR*newCapacity];
		
			if (x != null)
				for (int i = 0; i < x.length; i++)
					buf[i] = x[i];
		
			return buf;
		}
		return x;
		
	}

	public static double [] ensureCapacityReset(double [] x, int newCapacity) {
		if (x == null || x.length <= newCapacity) 
			return new double[GROWTH_FACTOR*newCapacity];
		for (int i = 0; i < newCapacity; i++) x[i] = 0;
		return x;
	}
	
	public static String join(String [] x) {
		return join(x, ",");
	}
	
	public static String join(String [] x, String sep) {
		StringBuilder s = new StringBuilder();

		for (int i = 0; i < (x.length-1); i++)
			s.append(x[i]).append(sep);
		s.append(x[x.length-1]);
		
		return s.toString();
	}
	
	public static String joinDoubleFields(Object o, String [] fields) { 
		return joinDoubleFields(o, fields, ",", false);
	}
	
	/**
	 * Given an array of strings, lookups up corresponding numeric (double) fields in an object and 
	 * concatenates them into a new String.
	 * 
	 * @param o
	 * @param fields
	 * @param sep
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String joinDoubleFields(Object o, String [] fields, String sep, boolean displayName) {

		StringBuilder s = new StringBuilder();
        
		Class c = o.getClass();
		double [] x = new double[fields.length];

		for (int i = 0; i < x.length; i++){
			try {
				x[i] = c.getField(fields[i]).getDouble(o);
				
			} catch (NoSuchFieldException nsfe) {
				throw new RuntimeException("Class "+c.getName()+" has no such property/field: "+ fields[i] +
						":" +nsfe);
			} catch (IllegalAccessException iae) {
				throw new RuntimeException("Can't access property "+fields[i]+" using direct field access from "+
						c.getName()+" instance: "+ iae);
			}
		}
		
		for (int i = 0; i < x.length; i++) {
			if (displayName)
				s.append(fields[i]).append(" = ");
			
			s.append(x[i]);
			
			if (i < (x.length-1))
				s.append(sep);
		}
		
		return s.toString();
		
	}
	
	public static double[] readDoubleArray(DataInput in) throws IOException {
		boolean isNull = in.readBoolean();
		if(isNull) return null;
		double[] res = new double[in.readInt()];
		for (int i = 0; i < res.length; i++) {
			res[i] = in.readDouble();
		}
		return res;
	}
	
	public static void writeDoubleArray(DataOutput out, double[] a) throws IOException{
		out.writeBoolean(a==null);
		if(a==null) return;
		out.writeInt(a.length);
		for (int i = 0; i < a.length; i++) out.writeDouble(a[i]);
	}
	
	public static void writeIntArrayAA(DataOutput out, int[][][] a) throws IOException {
		out.writeBoolean(a == null);
		if (a==null) return;
		out.writeInt(a.length);
		for (int i = 0; i < a.length; i++) writeIntArrayA(out,a[i]);
	}
	
	public static void writeIntArrayA(DataOutput out, int[][] a) throws IOException {
		out.writeBoolean(a == null);
		if (a==null) return;
		out.writeInt(a.length);
		for (int i = 0; i < a.length; i++) writeIntArray(out,a[i]);
	}

	public static void writeIntArray(DataOutput out, int[] a) throws IOException{
		out.writeBoolean(a==null);
		if(a==null) return;
		out.writeInt(a.length);
		for (int i = 0; i < a.length; i++) out.writeInt(a[i]);
	}

	public static void writeFeatureVectorArray(DataOutput out, FeatureVector[] a) throws IOException{
		out.writeBoolean(a==null);
		if(a==null) return;
		out.writeInt(a.length);
		for (int i = 0; i < a.length; i++) a[i].write(out);
	}

	public static int[][][] readIntArrayAA(DataInput in) throws IOException {
		boolean isNull = in.readBoolean();
		if(isNull) return null;
		int[][][] res = new int[in.readInt()][][];
		for (int i = 0; i < res.length; i++) {
			res[i] = readIntArrayA(in);
		}
		return res;
	}

	
	public static int[][] readIntArrayA(DataInput in) throws IOException {
		boolean isNull = in.readBoolean();
		if(isNull) return null;
		int[][] res = new int[in.readInt()][];
		for (int i = 0; i < res.length; i++) {
			res[i] = readIntArray(in);
		}
		return res;
	}
	
	public static int[] readIntArray(DataInput in) throws IOException {
		boolean isNull = in.readBoolean();
		if(isNull) return null;
		int[] res = new int[in.readInt()];
		for (int i = 0; i < res.length; i++) {
			res[i] = in.readInt();
		}
		return res;
	}
	
	public static FeatureVector[] readFeatureVectorArray(DataInput in) throws IOException {
		boolean isNull = in.readBoolean();
		if(isNull) return null;
		FeatureVector[] res = new FeatureVector[in.readInt()];
		for (int i = 0; i < res.length; i++) {
			res[i] = new FeatureVector(in);
		}
		return res;
	}
	
	public static void sortByDoubleValues(double values[], String keys[]) {
		
		WeightsEntry entries[] = new WeightsEntry[keys.length];
		
		for (int i = 0; i < keys.length; i++)
			entries[i] = new WeightsEntry(keys[i], values[i]);

		Arrays.sort(entries);
		
		for (int i = 0; i < keys.length; i++) {
			keys[i] = entries[i].aKey;
			values[i] = entries[i].weight;
		}
	}

	public static int dotProduct(int[] a, int[] b) {
		int res = 0;
		for (int i = 0; i < a.length; i++) {
			res+=a[i]*b[i];
		}
		return res;
	}

	public static int min(int[] vals) {
		int min = Integer.MAX_VALUE; 
		for(int v:vals) min = min>v ? v : min;
		return min;
	}

	public static void minusEquals(int[] vals, int min) {
		for (int i = 0; i < vals.length; i++) {
			vals[i] -= min;
		}
	}

}

class WeightsEntry implements Comparable<WeightsEntry>{
	String aKey;
	double weight;
	
	public WeightsEntry(String aKey, double weight) {
		super();
		this.aKey = aKey;
		this.weight = weight;
	}

	public int compareTo(WeightsEntry o) {
		if (o.weight < this.weight)
			return -1;
		if (o.weight > this.weight)
			return 1;
		
		return 0;
	}
}