package cascade.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * 
 * A simple utility printing to file class that makes it easy to enable/disable overwriting previous logs.
 * 
 * @author djweiss
 *
 */
public class SimpleLogger {
		
	String myFileName;
	PrintStream out;

	public SimpleLogger(String targetFileName) {
		this(targetFileName, ".csv", true);
	}
		
	public static String findUniqueName(String targetName, String ext) {
	
		File f = new File(targetName + ext);
		
		String unique = targetName;
		
		int counter= 0;
		while (f.exists()) {
			unique = targetName + "-" + String.format("%02d", counter++);
			f = new File(unique + ext);
		}
			
		return unique;
	}
	
	public SimpleLogger(String targetFileName, String ext, boolean overwrite) {
	
		String myFileName = targetFileName;
		if (!overwrite) myFileName = findUniqueName(targetFileName, ext);

		try {
			out = new PrintStream(new File(myFileName + ext));
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException("Fatal error: cannot log to file " + myFileName);
		}

	}
	
	public String getLogFileName() {
		return myFileName;
	}
		
	public void println(String s){	
		out.println(s);
		out.flush();
	}
	
}
