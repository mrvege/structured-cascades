package cascade.io;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ObjectReader {

	ObjectInputStream in;
	
	public ObjectReader(String filename) throws IOException {
		this(filename, true);
	}
	
	public ObjectReader(String filename, boolean strict) throws IOException {
		
		try {
			in = new ObjectInputStream(
					new BufferedInputStream(
							new FileInputStream(filename)));
			
		
		} catch (FileNotFoundException e) {
			System.err.printf("File '%s' not found! Unable to load object.\n", filename);
			if (strict) 
				throw e;
			
		} catch (IOException e) {
			throw e;
		}
	}
	
	public Object readObject() throws IOException, ClassNotFoundException {
		try {
			return in.readObject();
		} catch (EOFException e) {
			return null;	
		}
	}
	
	public void close() throws IOException {
		in.close();
	}

	/**
	 * @param filename
	 * @param strict
	 * if true, will HALT the program if file not found.
	 * @return
	 * @throws IOException
	 */
	public static Object readOneObject(String filename, boolean strict) throws IOException {

		ObjectReader or = new ObjectReader(filename, strict);

		Object ret = null;
		try {
			ret = or.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		or.close();
		
		return ret;
	}
	
	public static Object newInstance(String name) {	    	    
	        Object o = null;
	        try {
	            o = Class.forName(name).newInstance();
	        }
	        catch (Exception e) {
	        	throw new RuntimeException("can't make instance of "+name,e);
	        }
	        return o;
	    }
}
