package cascade.programs;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

import cascade.io.ObjectReader;
import cascade.lattice.Lattice;
import cascade.model.CascadeModel;

/**
 * Utility command-line program to print out the contents of lattice and weight objects. 
 *
 */
public class Inspector {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
	
		
		String type = args[0];
		
		if (type.toLowerCase().equals("lattice")) {
			inspectLattices(args);
		}

	}

	private static void inspectLattices(String[] args) throws IOException, ClassNotFoundException {

		String dir = args[1];
		String partition = args[2];
		
		CascadeModel m = (CascadeModel) ObjectReader.readOneObject(dir + "model", true);
		
		String fname = dir + partition + "-test-lattices";
		
		DataInputStream in = new DataInputStream(new FileInputStream(fname));//new GZIPInputStream(new FileInputStream(fname)));
		
		Lattice lattice;
		while ( (lattice = Lattice.readLattice(in)) != null) {
			lattice.model = m;
			lattice.print();
		}
		
	}

}
