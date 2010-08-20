package fig;

import java.util.List;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;

public class FigLoader {

	@SuppressWarnings("unchecked")
	public static List<Object> initObjects(String fileName) throws Exception {
		
	    FigLexer lexer = new FigLexer(new ANTLRFileStream(fileName));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    
	    FigParser g = new FigParser(tokens);
	    // begin parsing and get list of config'd objects	    
	    List<Object> config_objects = g.file();
	    return config_objects;

	}
	
}
