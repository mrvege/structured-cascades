package cascade.util;

import java.util.ArrayList;

import gnu.trove.TIntArrayList;

public class NgramTrieDictionary {
	ArrayList<TrieNode> numberToNGram;
	TrieNode[] nGramToNumber;
	int depth;
	
	public NgramTrieDictionary(int depth, int numTags){
		this.depth = depth;
		nGramToNumber = new TrieNode[numTags];
		numberToNGram = new ArrayList<TrieNode>();
		for (int i = 0; i < nGramToNumber.length; i++) {
			if(depth > 1){
				nGramToNumber[i] = new TrieNode(null, i);
			}else{
				//Case of first order model
				TrieNode node =  new TrieNode(null, i,i);
				nGramToNumber[i] = node;
				numberToNGram.add(node);
			}
		}
		
	}
	
	/**
	 * Gets an NGramId and if it does not exist create it
	 * @param ngram
	 * @return
	 */
	public int addNGramId(int[] ngram){
		if(ngram.length != depth){
			throw new UnsupportedOperationException("Cannot get an ngramID of length " + ngram.length + " when depth = " + depth);
		}
		TrieNode node = nGramToNumber[ngram[0]];
		if(depth == 1){
			return node.number;
		}
		
		for (int i = 1; i < ngram.length-1; i++) {
			 node = node.addChildById(ngram[i]);
		}
		node = node.addLeafById(ngram[depth-1]);
		return node.number;
	}
	
	/**
	 * Gets an NGramId and do not create new nodes
	 * @param ngram
	 * @return
	 */
	public int getNGramId(int[] ngram){
		if(ngram.length != depth){
			throw new UnsupportedOperationException("Cannot get an ngramID of length " + ngram.length + " when depth = " + depth);
		}
		TrieNode node = nGramToNumber[ngram[0]];
		if(depth == 1){
			return node.number;
		}
		
		for (int i = 1; i < ngram.length-1; i++) {
			 node = node.search(ngram[i]);
			 if(node == null) return -1;
		}
		node = node.search(ngram[depth-1]);
		if(node == null) return -1;
		return node.number;
	}
	
	public int[] getNgram(int number){
		int[] results = new int[depth];
		TrieNode node = numberToNGram.get(number);
		for (int i = 0; i <depth; i++) {
			results[depth-i-1] = node.tagId;
			node = node.parent;
		}
		if(node != null){
			throw new AssertionError("Trie is not of correct depth");
		}
		return results;
	}
	
	public int size(){
		return numberToNGram.size();
	}
	
	public String toString(){
		StringBuilder bf = new StringBuilder();
		for (int i = 0; i < numberToNGram.size(); i++) {
			bf.append(numberToNGram.get(i).number + "{" );
			int[] ngram = getNgram(i);
			for (int j = 0; j < ngram.length-1; j++) {
				bf.append(ngram[j]+" ");
			}
			bf.append(ngram[ngram.length-1]);
			bf.append("} ");
		}
		bf.append("\n");
		return bf.toString();
	}
	
	class TrieNode{
		int number;
		TrieNode parent;
		int tagId;
		ArrayList<TrieNode> childs;
		public TrieNode(TrieNode parent, int tagId, int number){
			this.parent = parent;
			this.number = number;
			this.tagId = tagId;
		}
		public TrieNode(TrieNode parent, int tagId){
			this.parent = parent;
			childs = new ArrayList<TrieNode>();
			this.tagId = tagId;
		}	
		
		/**
		 * Return the node with this particular tag id or creates a new node.
		 * @param tagId
		 * @return
		 */
		public TrieNode addChildById(int tagId){
			
			TrieNode node = search(tagId);
			if(node == null){
				node = new TrieNode(this, tagId);
				childs.add(node);
			}
			return node;
		}
		
		
		
		public TrieNode addLeafById(int tagId){
			TrieNode node = search(tagId);
			if(node == null){
				node = new TrieNode(this, tagId, numberToNGram.size());
				numberToNGram.add(node);
				childs.add(node);
			}
			return node;
		}
		
		
		private TrieNode search(int tagId){
			
			for (int i = 0; i < childs.size(); i++) {
				if(childs.get(i).tagId == tagId){
					return childs.get(i);
				}
			}
			return null;
		}
		
	}
	
	public static void main(String[] args) {
		int[] test = {2,3};
		int[] test2 = {2,4};
		int[] test3 = {2,3};
		NgramTrieDictionary dict = new NgramTrieDictionary(2,8);
		System.out.println(dict.addNGramId(test));
		System.out.println(dict.addNGramId(test2));
		System.out.println(dict.addNGramId(test3));
		int[] res = dict.getNgram(0);
		res = dict.getNgram(1);
		System.out.println(dict);
		
	}
	
	
}
