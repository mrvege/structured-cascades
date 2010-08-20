package cascade.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class SentenceReader {
	
	 protected BufferedReader inputReader;
	 
	 public void startReading (String file) throws IOException {
		 inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));
	 }
	 
	 public SentenceInstance getNext() throws IOException {

		 ArrayList<String[]> lineList = new ArrayList<String[]>();
		 
		 String line = inputReader.readLine();
		 while (line != null && !line.trim().equals("") && !line.startsWith("*")) {
			 lineList.add(line.split("\t"));
			 line = inputReader.readLine();
			 //System.out.println("## "+line);
		 }

		 int length = lineList.size();

		 if(length == 0) {
			 inputReader.close();
			 return null;
		 }

		 String[] forms = new String[length+1];
		 String[] pos = new String[length+1];
		 String[][] feats = new String[length+1][];

		 forms[0] = "<root>";
		 pos[0] = "<root-POS>";

		 for(int i = 0; i < length; i++) {
			 String[] info = lineList.get(i);
			 forms[i+1] = normalize(info[1]);
			 pos[i+1] = info[2];
			 feats[i+1] = info[3].split("\\|");
		 }

		 return new SentenceInstance(forms, pos, feats);

	 }

	 public void close () throws IOException {
		 inputReader.close();
	 }
	    
	 protected String normalize (String s) {
		 if(s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
			 return "<num>";
		 
		 return s;
	 }	
}
