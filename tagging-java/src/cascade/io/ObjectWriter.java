package cascade.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ObjectWriter {

	String filename;
	ObjectOutputStream out;
	
	public ObjectWriter(String filename) throws IOException {
		this.filename = filename;
		out = new ObjectOutputStream(new FileOutputStream(filename));
	}
	
	public void writeObject(Object o) throws IOException {
		out.writeObject(o);
		out.reset();
	}
	
	public void close() throws IOException {
		out.close();
	}

	public static void writeOneObject(String filename, Object o) throws IOException {

		ObjectWriter ow = new ObjectWriter(filename);
		ow.writeObject(o);
		ow.close();
	}

	public String getDest() {
		return filename;
	}
}
