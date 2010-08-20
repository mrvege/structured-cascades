package cascade.lattice;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;


import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import cascade.features.FeatureVector;
import cascade.io.Sequence;
import cascade.model.CascadeModel;
import cascade.model.NOrderPOS;
import cascade.util.ArrayUtil;

/**
 *         Efficient representation for fast computations on a sparse sequence
 *         graph.
 * 
 *         Edges on the graph are stored as (left, right), where left is a
 *         linear index into [pos-1, left state], and right is a linear index
 *         into [pos, right state].
 * 
 *         e.g. edges for position zero are all (null, x), where x is a state.
 *         there are no right edges for the final position.
 *         
 *         
 * 
 * @author djweiss
 */
public class Lattice {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	protected static final long classID = 0;
	
	public static final int NULL_IDX = -1;

	protected int length;

	// state scoring component
	/**
	 * maps from the linear indexing of nodes in the graph to states ID in Model. 
	 * length=number of vertices in lattice
	 * values=0..(e.g. #postags)
	 */
	protected int stateIDs[]; // for each state the actual model ID for that state
	
	/**
	 * maps from a position in the lattice (e.g. first word, second word etc) to 
	 * an index into the stateIDs array. 
	 */
	protected int statePosOffsets[]; 

	/**
	 * edges coming into the state identified by position in the sentence, 
	 * and its index (e.g. if there are 3 states at position 5, statenum would be 0..2).
	 * last entry is an array of lattice edge indices. 
	 * See edgeLeftStates and edgeRightStates.
	 */
	protected int leftEdgeIdx[][][]; // pos x statenum x # left edges
	/** see leftEdgeIdx */
	protected int rightEdgeIdx[][][]; // pos x statenum x # right edges

	// edge storing component
	/** maps from a position in the lattice to the first edge at that position 
	 * (regardless of which state it corresponds to) */
	protected int edgePosOffsets[];
	/**
	 * mapping from edges to states on the left (see also edgeRightStates)
	 */
	public int edgeLeftStates[]; // left states
	public int edgeRightStates[]; // right states	

	// pointers for interpretability
	public Sequence seq = null;
	public int seqHash = 0;
	public CascadeModel model = null;
	
	// features
	/**
	 * Features associated with this lattice. Note that based on the model,
	 * there may be one feature vector associated with each position, state, or edge.
	 * 
	 */
	public FeatureVector [] fv = null;
	
	// scores
	public double [] stateScores = null;
	public double [] edgeScores = null;
	
	public double meanEdgeScore = Double.NaN;
	public double maxEdgeScore = Double.NaN;

	/**
	 * Default constructor; does nothing.
	 */
	public Lattice() {}
	
	public Lattice(Sequence seq, CascadeModel m) {
		this.seq = seq;
		this.seqHash = seq.hashCode();
		this.model = m;
	}
	
	/**
	 * This replaces the functionality of having an ObjectStream, to avoid memory leaks.
	 * 
	 * Any Lattice class, in order to function properly, MUST FIRST write out a "classID" identifier
	 * so that this function will call the appropriate constructor.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Lattice readLattice(DataInput in) throws IOException {
		
		long classID = in.readLong();
		
		if (classID == Lattice.classID)
			return new Lattice(in);
		else if (classID == ZeroOrderLattice.classID)
			return new ZeroOrderLattice(in);
		return null;
		
	}
	
	/**
	 * Builds a full sequence graph from a given Model over a sequence of
	 * specific length.
	 * 	 
	 * @deprecated
	 */
	private Lattice(NOrderPOS m, int length) {
		this.length = length;

		statePosOffsets = new int[length + 1];
		edgePosOffsets = new int[length + 2];

		leftEdgeIdx = new int[length][][];
		rightEdgeIdx = new int[length][][];

		// ------------------------------------------------------
		// pass 1: compute the valid states (any NON NULL state)

		TIntArrayList newStateIDs = new TIntArrayList();

		int numPossibleStates = m.numPossibleStates();

		int nulltagID = m.getPOSAlphabet().lookupIndex(NOrderPOS.NULLTAG);
		
		// starting states
		for (int s : m.getStartStates()) {
			if (m.computeTagFromNGramID(this, m.order, s,0) != nulltagID)
				newStateIDs.add(s);
		}
		
		statePosOffsets[1] = newStateIDs.size();

		// adds all states that don't have [NULLTAG] at position 0
		for (int pos = 1; pos < length; pos++) {

			for (int s = 0; s < numPossibleStates; s++) {
				if (m.computeTagFromNGramID(this, m.order, s,0) != nulltagID)
					newStateIDs.add(s);
			}

			statePosOffsets[pos + 1] = newStateIDs.size();
		}

		stateIDs = newStateIDs.toNativeArray();

		// trim states that might have [NULLTAG]'s in them, just not at offset 0, and 
		// so can't be reached (the first few positions will have [NULLTAG] but can be reached) 
		checkForUnreachableStates(m);

		// ------------------------------------------------------
		// pass 2: compute the valid edges
		// if (m.getOrder() > 0)
		computeValidEdges(m);
	}

	/**
	 * Builds a new sparse Latitice from an existing one, according to the states generated
	 * by a CascadeModel.  
	 * 
	 * Checks for unreachable states and fails if found b/c lattice is broken.
	 *
	 * This would be used to generate a higher order lattice from a lower order one, for instance.
	 */
	public Lattice(Lattice base, CascadeModel m, boolean mask[]) {
		this(base.seq, m);

		// # of positions stays the same
		length = base.length;
		statePosOffsets = new int[length + 1];
		edgePosOffsets = new int[length + 2]; // handle starting and ending edges

		leftEdgeIdx = new int[length][][];
		rightEdgeIdx = new int[length][][];

		// ----------------------------------------------------------------------
		// Pass 1: compute the number of valid states in the graph at each
		// position in the new graph,
		// including the filtering.
		
		TIntArrayList newStateIDs = new TIntArrayList();
		TDoubleArrayList newStateScores = new TDoubleArrayList();

		m.generateValidStates(base, mask, newStateIDs, newStateScores, statePosOffsets);

		// dump stack of stateIDs / scores to native array
		stateIDs = newStateIDs.toNativeArray();
		stateScores = newStateScores.toNativeArray();
		
		// ----------------------------------------------------------------------
		// Pass 3: compute valid edges between states
		computeValidEdges(m);	
		
		// ----------------------------------------------------------------------
		// Pass 2: check for unreachable states (fail if found)
		checkForUnreachableStates(m);		
		
		if (statePosOffsets[length] == 0) {
			base.printEdgeMask(mask);
			//FIXME does not give any information
			throw new RuntimeException("Lattice is broken, cannot proceed");
		}
	}

	/**
	 * Note: it's important to use raw I/O methods on DataStreams rather than java Serialization
	 * because it otherwise you get <b>MASSIVE MEMORY LEAKS AND SLOWDOWNS.</b>
	 */
	public Lattice(DataInput in) throws IOException {

		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);
		seqHash = in.readInt();
		length = in.readInt();
		stateIDs = ArrayUtil.readIntArray(in);
		statePosOffsets = ArrayUtil.readIntArray(in);
		edgePosOffsets = ArrayUtil.readIntArray(in);
        leftEdgeIdx = ArrayUtil.readIntArrayAA(in);
        rightEdgeIdx = ArrayUtil.readIntArrayAA(in);
        edgePosOffsets = ArrayUtil.readIntArray(in);
        edgeLeftStates = ArrayUtil.readIntArray(in);
        edgeRightStates = ArrayUtil.readIntArray(in);
        fv = ArrayUtil.readFeatureVectorArray(in);
		// scores are never saved
		stateScores = null;
		edgeScores = null;

	}

	/**
	 * Somewhat inefficienctly, check for unreachable states and mark them 
	 */
	protected void checkForUnreachableStates(CascadeModel m) {
			
		// compute which states are actually reachable
		boolean [] reachableLeft = computeReachableForward(m);
		boolean [] reachableRight = computeReachableBackward(m);
		
		// now build up a new list of state IDs
//		TIntArrayList reachableStateIDs = new TIntArrayList(stateIDs.length);
//		TDoubleArrayList reachableStateScores = new TDoubleArrayList(stateIDs.length);
//		
//		int [] reachablePosOffsets = new int[statePosOffsets.length];
		if (!reachableLeft[0] || !reachableRight[0]){
		System.out.print("");
		reachableLeft = computeReachableForward(m);
		reachableRight = computeReachableBackward(m);
		}
		// loop through each position's valid EDGES or STATES (order 0) in the BASE matrix
		for (int pos = 0; pos < length; pos++) {
			
			int start = statePosOffsets[pos];
			int end = statePosOffsets[pos + 1];
			
			for (int idx = start; idx < end; idx++) {
				
				if (!reachableLeft[idx] || !reachableRight[idx]) {
					print();
					throw new RuntimeException("Position " + pos + " state " + idx + " = Unreachable state detected!");

//					reachableStateIDs.add(stateIDs[idx]);
//					reachableStateScores.add(stateIDs[idx]);
				}
			}
			
//			reachablePosOffsets[pos + 1] = reachableStateIDs.size();
		}
		
//		stateIDs = reachableStateIDs.toNativeArray();
//		stateScores = reachableStateScores.toNativeArray();
//		
//		statePosOffsets = reachablePosOffsets;
	}

	protected boolean[] computeReachableForward(CascadeModel m) {
		
		boolean [] reachable = new boolean[statePosOffsets[length]]; 

		// special case for length = 1
		if(length == 1){
			for (int i = 0; i < stateIDs.length; i++) {
				reachable[i] = true;
			}
		}
		
		// loop through each position's valid EDGES 
		for (int pos = 0; pos < (length-1); pos++) {

			int start = statePosOffsets[pos];
			int end =  statePosOffsets[pos+1];

			for (int idx = start; idx < end; idx++) {

				// if this state is a start state, then mark as reachable 
				if (pos == 0)
					reachable[idx] = true;

				// if reachable mark neighbors as reachable
				if (reachable[idx]) {
					int state = stateIDs[idx];
					
					// search of valid next (right) states
					for (int nextstate : m.getNextStates(seq, pos, state)) {
						
						// check if previous state is valid at previous position
						int nextIdx = findStateIdx(pos+1, nextstate);
							
						if (nextIdx != NULL_IDX)  
							reachable[nextIdx] = true;
					}
				}
			}
		}
		
		return reachable;
	}

	protected boolean[] computeReachableBackward(CascadeModel m) {
	
		boolean [] reachable = new boolean[statePosOffsets[length]]; 

		// special case for length = 1
		if(length == 1){
			for (int i = 0; i < stateIDs.length; i++) {
				reachable[i] = true;
			}
		}
		
		// loop through each position's valid EDGES 
		for (int pos = length-1; pos > 0; pos--) {

			int start = statePosOffsets[pos];
			int end =  statePosOffsets[pos+1];

			boolean anyReachable = false;
			for (int idx = start; idx < end; idx++) {

				// if this state is a end state, then mark as reachable 
				if (pos == (length-1))
					reachable[idx] = true;

				// if reachable mark neighbors as reachable
				if (reachable[idx]) {
						
					int state = stateIDs[idx];
					
					// search of valid prev (left) states
					for (int prevstate : m.getPreviousStates(seq, pos, state)) {
						
						// check if previous state is valid at previous position
						int prevIdx = findStateIdx(pos-1, prevstate);
							
						if (prevIdx != NULL_IDX) {
							reachable[prevIdx] = true;
							anyReachable = true;
						}
					}
				}
			}
			  
			if (!anyReachable) {
				throw new RuntimeException("lattice (seq " + seq.id + " hash " + seqHash + " broken at position " + pos);
			}
		}
		
		return reachable;

	}

	/**
	 * Given initialize sparse state representation, computes all of the valid
	 * edges according to the given Model.
	 * 
	 * @param m
	 */
	protected void computeValidEdges(CascadeModel m) {

		TIntArrayList newEdgeLeftStates = new TIntArrayList();
		TIntArrayList newEdgeRightStates = new TIntArrayList();

		TIntArrayList newIdx = new TIntArrayList();

		// pass 1: -------------------------------------
		// edges need to be computed LEFT first
		
		for (int pos = 0; pos < length; pos++) {

			int numStates = getNumValidStates(pos);

			leftEdgeIdx[pos] = new int[numStates][];
			rightEdgeIdx[pos] = new int[numStates][];

			for (int i = 0; i < numStates; i++) {
				int idx = statePosOffsets[pos] + i; // idx of current state

				int state = stateIDs[idx];

				// ------------- left edges
				if (pos == 0) {

					leftEdgeIdx[pos][i] = new int[1];
					leftEdgeIdx[pos][i][0] = addEdge(pos, NULL_IDX, idx,
							newEdgeLeftStates, newEdgeRightStates);

				} else {

					// reset capacity to zero
					newIdx.resetQuick();

					// search of valid previous (left) states
					for (int prevstate : m.getPreviousStates(seq, pos, state)) {

						// check if previous state is valid at previous position
						int prevIdx = findStateIdx(pos - 1, prevstate);
						if (prevIdx != NULL_IDX)
							newIdx.add(addEdge(pos, prevIdx, idx,
									newEdgeLeftStates, newEdgeRightStates));

					}

					leftEdgeIdx[pos][i] = newIdx.toNativeArray();
				}

				edgePosOffsets[pos+1] = newEdgeLeftStates.size();						
			}			
		}
		
		// ------------- right edges
		for (int pos = 0; pos < length; pos++) {

			int numStates = getNumValidStates(pos);

			for (int i = 0; i < numStates; i++) {
				int idx = statePosOffsets[pos] + i; // idx of current state

				int state = stateIDs[idx];

				// ------------- right edges
				if (pos == (length - 1)) {
	
					// all states go to the finish.
					rightEdgeIdx[pos][i] = new int[1];
					rightEdgeIdx[pos][i][0] = addEdge(pos, idx, NULL_IDX,
							newEdgeLeftStates, newEdgeRightStates);
	
				} else {
	
					newIdx.resetQuick();
	
					// search of valid next (right) states
					for (int nextstate : m.getNextStates(seq, pos, state)) {
	
						// check if previous state is valid at previous position
						int nextIdx = findStateIdx(pos+1, nextstate);
						if (nextIdx != NULL_IDX)
							newIdx.add(findEdgeIdx(pos+1, idx, nextIdx,
									newEdgeLeftStates, newEdgeRightStates));
	
					}
	
					rightEdgeIdx[pos][i] = newIdx.toNativeArray();
				}
			}

		}
		
		edgePosOffsets[length+1] = newEdgeLeftStates.size();

		edgeLeftStates = newEdgeLeftStates.toNativeArray();
		edgeRightStates = newEdgeRightStates.toNativeArray();
	}

	protected int findEdgeIdx(int pos, int prevIdx, int idx,
			TIntArrayList newEdgeLeftStates, TIntArrayList newEdgeRightStates) {

		int relativeIdx = idx - statePosOffsets[pos];

		int[] checkIdx = leftEdgeIdx[pos][relativeIdx];
		for (int i = 0; i < checkIdx.length; i++)
			if (newEdgeLeftStates.get(checkIdx[i]) == prevIdx)
				return checkIdx[i];

		return NULL_IDX;
	}
	
//	public int findMissingEdgeIdx(int pos, int prevStateID, int stateID) {
//		int edgeIdx = NULL_IDX;
//
//		int prevStateIdx = NULL_IDX;
//		if (pos > 0)
//			prevStateIdx = findStateIdx(pos-1, prevStateID);
//		
//		if (prevStateIdx != NULL_IDX) {
//			
//		}
//		return edgeIdx;
//	}
	/**
	 * Tries to locate an edge between two states
	 * @param pos
	 * position of the edge
	 * @param prevIdx
	 * previous state linear index
	 * @param idx
	 * next state linear index
	 * @return
	 * edge linear index or NULL_IDX if it could not be found
	 */
	public int findEdgeIdx(int pos, int prevIdx, int idx) {

		if (pos < length) { // check from left
			
			int relativeIdx = idx - statePosOffsets[pos];
			
			int[] checkIdx = leftEdgeIdx[pos][relativeIdx];
			for (int i = 0; i < checkIdx.length; i++)
				if (edgeLeftStates[checkIdx[i]] == prevIdx)
					return checkIdx[i];
			return NULL_IDX;
			
		} else { // check from right
			
			int relativeIdx = prevIdx - statePosOffsets[pos-1];

			int[] checkIdx = rightEdgeIdx[pos-1][relativeIdx];
			for (int i = 0; i < checkIdx.length; i++)
				if (edgeRightStates[checkIdx[i]] == idx)
					return checkIdx[i];
			return NULL_IDX;
			
		}
		
	}
	
	/**
	 * Converts a path given by state id's into a path given in terms of linear edge indexes. Includes a
	 * start edge (-1, state) but not a final edge (state,-1).
	 * 
	 * */
	public int [] findEdgeIdx(int [] path) {
		return findEdgeIdx(path, false);
	}
	
	/**
	 * Finds the linear edge indexes of a path through a lattice given in terms of state id's.
	 * 
	 * @param path 
	 * The path of state id's.
	 * @param finalEdge
	 * Whether or not the final edge (state,-1) should be searched for. If true, then the number
	 * of edges is greater than the length of the original path.
	 * @return Array of linear edge indices corresponding to the path.
	 * 
	 */
	public int [] findEdgeIdx(int [] path, boolean finalEdge) {
		assert(path.length == length);
		
		int [] edgeIdx = (finalEdge) ? new int[path.length+1] : new int[path.length];
		
		int prevIdx = NULL_IDX;
		
		// find the edges involved in this path and score
		for (int pos = 0; pos < path.length; pos++) {

			int idx = NULL_IDX;
			int stateIdx = findStateIdx(pos, path[pos]);
//			if (pos!=length-1 && stateIdx==NULL_IDX) throw new AssertionError("Null state at position "+pos+" / "+length);

			
			if (stateIdx != NULL_IDX)
				idx = findEdgeIdx(pos, prevIdx, stateIdx);

			// store edge idx
			if (edgeIdx != null)
				edgeIdx[pos] = idx;
			
			prevIdx = stateIdx;
		}

		if (finalEdge)
			edgeIdx[length] = findEdgeIdx(length, prevIdx, -1);
		
		return edgeIdx;
	}

	/**
	 * Calls findStateIdx for all states along a path.
	 */
	public int [] findStateIdx(int states[]) {
		
		int[] idx = new int[states.length];
		
		for (int pos = 0; pos < length; pos++)
			idx[pos] = findStateIdx(pos, states[pos]);
		
		return idx;
	}
	
	/**
	 * Find linear state index of a given state id number at a given position
	 * @param pos
	 * @param state
	 * @return linear index of state at position i, or NULL_IDX if doesn't exist
	 */
	public int findStateIdx(int pos, int state) {

		for (int idx = statePosOffsets[pos]; idx < statePosOffsets[pos + 1]; idx++)
			if (stateIDs[idx] == state)
				return idx;

		return NULL_IDX;
	}

	protected int addEdge(int pos, int sl, int sr, TIntArrayList left,
			TIntArrayList right) {

		left.add(sl);
		right.add(sr);

		int edgeidx = left.size();
		assert (left.size() == right.size());

		return edgeidx - 1;
	}

	protected int getNumValidStates(int pos) {
		return statePosOffsets[pos + 1] - statePosOffsets[pos];
	}

	protected int getNumValidEdges(int pos) {
		return edgePosOffsets[pos + 1] - edgePosOffsets[pos];
	}


	public int getStateID(int idx) {
		return stateIDs[idx];
	}

	public int length() {
		return length;
	}

	public int getStateOffset(int pos) {
		return statePosOffsets[pos];
	}

	public int[] getLeftEdges(int pos, int state) {
		if (leftEdgeIdx != null && leftEdgeIdx.length > 0)
			return leftEdgeIdx[pos][state];
		else return null;
	}

	public int[] getRightEdges(int pos, int state) {
		if (rightEdgeIdx != null && rightEdgeIdx.length > 0)
			return rightEdgeIdx[pos][state];
		else return null;
	}

	public int getLeftStateIdx(int edgeIdx) {
		return edgeLeftStates[edgeIdx];
	}

	public int getRightStateIdx(int edgeIdx) {
		return edgeRightStates[edgeIdx];
	}

	public int getEdgeOffset(int pos) {
		return edgePosOffsets[pos];
	}

	public int getNumEdges() {
		return edgePosOffsets[length+1];
	}

	public int getNumStates() {
		return statePosOffsets[length];
	}
	
	/**
	 * Prints a list of all edges indicating which would be pruned by the given mask.
	 * @param mask
	 */
	public void printEdgeMask(boolean mask []) {
		for (int pos = 0; pos < length+1; pos++) {

			System.out.printf("\nEdges at position %d:\n\n", pos);

			int start = getEdgeOffset(pos);
			int end = getEdgeOffset(pos + 1);

			boolean allPruned = true;
			double max = Double.NEGATIVE_INFINITY;
			int argmax = -1;
			for (int edgeIdx = start; edgeIdx < end; edgeIdx++) {
					
				int leftIdx = getLeftStateIdx(edgeIdx);
				int rightIdx = getRightStateIdx(edgeIdx);
				String leftState = (leftIdx != -1) ? model.stateToString(this, getStateID(leftIdx)) : "-1";
				String rightState = (rightIdx != -1) ? model.stateToString(this, getStateID(rightIdx)) : "-1";
				
				String pstr = (!mask[edgeIdx] ? "XXX" : "");
				
//				System.out.printf("edge: %s [%d] = (%s,%s) [%g]\n", pstr, edgeIdx, leftState, rightState, 
//						edgeScores[edgeIdx]);//				
//			
				if (edgeScores[edgeIdx] > max) {
					max = edgeScores[edgeIdx];
					argmax = edgeIdx;
				}
					
					
				if (mask[edgeIdx])
					allPruned = false;
//				if (fv != null && fv.length == getNumEdges()) 
//				System.out.println("\t Features: " + fv[edgeIdx].toString(model.featureAlphabet));
			}
			if (allPruned)
				System.out.println("*********** ALL EDGES AT POSITION " + pos + " ARE PRUNED ***********");
			
			int edgeIdx = argmax;
			
			int leftIdx = getLeftStateIdx(edgeIdx);
			int rightIdx = getRightStateIdx(edgeIdx);
			String leftState = (leftIdx != -1) ? model.stateToString(this, getStateID(leftIdx)) : "-1";
			String rightState = (rightIdx != -1) ? model.stateToString(this, getStateID(rightIdx)) : "-1";
			String pstr = (!mask[edgeIdx] ? "XXX" : "");

			System.out.printf("ARGMAX edge: %s [%d] = (%s,%s) [%g]\n", pstr, edgeIdx, leftState, rightState, 
					edgeScores[edgeIdx]);				

		}

		
	}
	
	/**
	 * Prints out a list of states at each location.
	 */
	public void printStateGrid() {
		System.out.println(statesToString());
	}
	
	public String statesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pos | States\n");
		for (int pos = 0; pos < length; pos++) {
				int start = getStateOffset(pos);
				int end = getStateOffset(pos + 1);		
				sb.append("["+pos+"]");
				for (int idx = start; idx < end; idx++) {
					//System.out.println("going by states " + idx);
					sb.append(model.stateToString(this, stateIDs[idx])).append(" ");
				}
				sb.append("\n");
		}
		return sb.toString();		
	}
	
	public String edgesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Pos | edge");
		for (int pos = 0; pos < edgePosOffsets.length -1; pos++) {
				int start = getEdgeOffset(pos);
				int end = getEdgeOffset(pos + 1);
				for (int idx = start; idx < end; idx++) {
					sb.append("["+pos+"|"+idx+"]");
					int lState, rState;
					if(getLeftStateIdx(idx)>=0) lState = stateIDs[getLeftStateIdx(idx)];
					else lState = -1;
					if(getRightStateIdx(idx)>=0) rState = stateIDs[getRightStateIdx(idx)];
					else rState = -1;
					sb.append(model.stateToString(this, lState)).append(" -> ");
					sb.append(model.stateToString(this, rState)).append(" : ");
					if(fv!=null)
						sb.append(fv2string(fv[idx]));
					sb.append("\n");
				}
				sb.append("\n");
		}
		return sb.toString();		
	}
	
	// FIXME: testing only.. but maybe should be elsewhere.. 
	public String fv2string(FeatureVector fv){
		StringBuilder sb = new StringBuilder();
		int[] keys = fv.getKeys();
		for (int i = 0; i < keys.length; i++) {
			sb.append(model.featureAlphabet.reverseLookup(keys[i])).append(" ");
		}
		return sb.toString();
	}
	
	
	
	/**
	 * Dumps all information about the current lattice to stdout, including features, etc.
	 */
	public void print() {

		System.out.printf("Sequence dump: length %d\n", length);
		System.out.printf("State offsets: %s\n", Arrays
				.toString(statePosOffsets));
		System.out
				.printf("Edge offsets: %s\n", Arrays.toString(edgePosOffsets));

		for (int pos = 0; pos < length; pos++) {

			System.out.printf("States at position %d:\n", pos);

			if (model != null && fv != null && fv.length == length) {
				System.out.println("\t" + fv[pos].getKeys().length + " Position Features:" + fv[pos].toString(model.featureAlphabet));
			}
			
			int start = getStateOffset(pos);
			int end = getStateOffset(pos + 1);

			for (int idx = start; idx < end; idx++) {

				if (model == null) {
				
					System.out.printf("[%d]: state %d\n", idx, getStateID(idx));

					for (int edgeIdx : getLeftEdges(pos, idx - start))
					System.out
					.printf("\t left edge: %d = (%d,%d)\n", edgeIdx,
							getLeftStateIdx(edgeIdx),
							getRightStateIdx(edgeIdx));
					
					for (int edgeIdx : getRightEdges(pos, idx - start))
						System.out
						.printf("\t right edge: %d = (%d,%d)\n", edgeIdx,
								getLeftStateIdx(edgeIdx),
								getRightStateIdx(edgeIdx));

				} else {
					
					System.out.printf("[%d]: state %s\n", idx, model.stateToString(this, getStateID(idx)));
					if (fv != null && fv.length == getNumStates()) 
						System.out.println("Features: " + fv[idx].toString(model.featureAlphabet));
					
						for (int edgeIdx : getLeftEdges(pos, idx - start)) {
						
						int leftIdx = getLeftStateIdx(edgeIdx);
						int rightIdx = getRightStateIdx(edgeIdx);
						String leftState = (leftIdx != -1) ? model.stateToString(this, getStateID(leftIdx)) : "-1";
						String rightState = (rightIdx != -1) ? model.stateToString(this, getStateID(rightIdx)) : "-1";
						System.out.printf("\t left edge: %d = (%s,%s)\n", edgeIdx, leftState, rightState);

						if (fv != null && fv.length == getNumEdges()) 
							System.out.println("\t Features: " + fv[edgeIdx].toString(model.featureAlphabet));

					}

					for (int edgeIdx : getRightEdges(pos, idx - start)) {
						
						int leftIdx = getLeftStateIdx(edgeIdx);
						int rightIdx = getRightStateIdx(edgeIdx);
						String leftState = (leftIdx != -1) ? model.stateToString(this, getStateID(leftIdx)) : "-1";
						String rightState = (rightIdx != -1) ? model.stateToString(this, getStateID(rightIdx)) : "-1";
						System.out.printf("\t right edge: %d = (%s,%s)\n", edgeIdx, leftState, rightState);		

						if (fv != null && fv.length == getNumEdges()) 
							System.out.println("\t Features: " + fv[edgeIdx].toString(model.featureAlphabet));
						
					}
				
				}
				
			}
		}

		System.out.println("edge dump:");
		for (int pos = 0; pos <= length; pos++) {
			for (int edgeIdx = getEdgeOffset(pos); edgeIdx < getEdgeOffset(pos+1); edgeIdx++) {
				System.out.printf("pos %d: [%d] (%d,%d)\n", pos, edgeIdx,
						getLeftStateIdx(edgeIdx), getRightStateIdx(edgeIdx));
			}
		}



	}

	/**
	 * Finds the edge indices of the argmax path through the lattice. Starts from the (X,-1) edge marginals
	 * to determine the argmax path. If finalEdge is true, it includes this edge in the path index array.
	 * 
	 * @param alphaArgs
	 * @param edgeMarginals
	 * @param finalEdge
	 * @return
	 */
	public int[] getArgmaxEdgeIdx(int[] alphaArgs, double[] edgeMarginals, boolean finalEdge) {
		int idx[] = new int[(finalEdge ? (length+1) : length)];
		
		// compute argmax over final state
		int start = getEdgeOffset(length);
		int end = getEdgeOffset(length+1);
		
		int argmax = -1;
		double max = Double.NEGATIVE_INFINITY;
		for (int edgeIdx = start; edgeIdx < end; edgeIdx++) {
			if (edgeMarginals[edgeIdx] > max) {
				max = edgeMarginals[edgeIdx];
				argmax = edgeIdx;
			}
		}
		if (argmax == -1)
			throw new RuntimeException("unable to compute argmax!");
		
		int pos = idx.length-1;
		if (finalEdge)
			idx[pos--] = argmax;
		
		argmax = alphaArgs[getLeftStateIdx(argmax)];

		// trace argmax path backwards from the end		
		for (int i = pos; i >= 0; i--) {
			idx[i] = argmax;
			if (i > 0)
				argmax = alphaArgs[getLeftStateIdx(argmax)]; 
		}
			
		return idx; 
	}
	
	/**
	 * Gets the states associated with the argmax.
	 * 
	 * @param alphaArgs
	 * @param alphaVals
	 * @return
	 */
	public int[] getArgmaxStates(int[] alphaArgs, double[] edgeMarginalVals) {

		// flat model result
		int [] path = getArgmaxEdgeIdx(alphaArgs, edgeMarginalVals, false);

		// convert to state IDs
		for (int i = 0; i < path.length; i++)
			path[i] = stateIDs[getRightStateIdx(path[i])];
		
		return path;
	}
	

	/**
	 * Computes the mean and max of state scores for each position. Assumes input already
	 * have enough memory allocated to hold the result.
	 * 
	 * @param mean
	 * @param max
	 */
	public void computePerPositionStateMeanMax(double[] mean, double[] max) {

		for (int pos = 0; pos < length; pos++) {
			
			int start = getStateOffset(pos);
			int end = getStateOffset(pos+1);
			
			double mn = 0, mx = Double.NEGATIVE_INFINITY;
			for (int idx = start; idx < end; idx++) {
				mn += stateScores[idx];
				if (stateScores[idx] > mx)
					mx = stateScores[idx];
			}
			
			mean[pos] = mn/(end-start);
			max[pos] = mx;
		}
		
	}

	/**
	 * Computes the mean and max edge score based on the current edgeScore property.
	 */
	public void computeEdgeMeanMax() {

		meanEdgeScore = 0;
		maxEdgeScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < getNumEdges(); i++) {
			double v = edgeScores[i];
			meanEdgeScore += v;
			if (maxEdgeScore < v)
				maxEdgeScore = v;
		}
		
		meanEdgeScore /= getNumEdges();
	}
	
	/**
	 * Fills in the witnessCount argument with the # of times each edge is included in a max
	 * marginal path (witness).
	 * 
	 * Note: uses a slow, naive recursion algorithm to do so.
	 * 
	 * @param alphaArgs
	 * forward argmax results
	 * @param betaArgs
	 * backward argmax results
	 * @param witnessCount
	 * will be filled with result
	 */
	public void computeEdgeWitnesses(int [] alphaArgs, int [] betaArgs, int [] witnessCount) {
	
		// uses naive recursive algorithm...
		for (int pos = 0; pos <= this.length(); pos++) {
			
			int start = this.getEdgeOffset(pos);
			int end = this.getEdgeOffset(pos+1);
			
			for (int idx = start; idx < end; idx++) {
				
				int leftIdx = this.getLeftStateIdx(idx);
				int rightIdx = this.getRightStateIdx(idx);

				witnessCount[idx]++;
				
				if (rightIdx != Lattice.NULL_IDX)
					incrementForward(betaArgs, betaArgs[rightIdx], witnessCount);
				if (leftIdx != Lattice.NULL_IDX)
					incrementBackward(alphaArgs, alphaArgs[leftIdx], witnessCount);
			}
		}

	}
	
	private void incrementBackward(int [] alphaArgs, int edgeIdx, int [] witnessCount) {
		witnessCount[edgeIdx]++;
		if (this.getLeftStateIdx(edgeIdx) != Lattice.NULL_IDX)
			incrementBackward(alphaArgs, alphaArgs[this.getLeftStateIdx(edgeIdx)], witnessCount);
		
	}

	private void incrementForward(int [] betaArgs, int edgeIdx, int [] witnessCount) {
		witnessCount[edgeIdx]++;		
		if (this.getRightStateIdx(edgeIdx) != Lattice.NULL_IDX)
			incrementForward(betaArgs, betaArgs[this.getRightStateIdx(edgeIdx)], witnessCount);
		
	}

	/**
	 * 
	 * Like computeEdgeWitnesses but assumes marginals are computed over <i>states</i>. 
	 * Not used by any of the current implementations.
	 * 
	 * @deprecated
	 */
	public void computeStateWitnesses(int [] alphaArgs, int [] betaArgs,
			int [] witnessCount) {
			
		// create temporary forward and backward arrays
		int left[] = new int[this.getNumStates()];
		int right[] = new int[this.getNumStates()];

		// -----------------------------------------------------
		// right: forward pass
		//   --> record # of times each state is listed as an alpha argmax
		for (int pos = 0; pos < this.length(); pos++) {
			
			int start = this.getStateOffset(pos);
			int end = this.getStateOffset(pos+1);
	
			for (int idx = start; idx < end; idx++) {
				int edgeIdx = betaArgs[idx];
				
				int leftIdx = this.getLeftStateIdx(edgeIdx);
				int rightIdx = this.getRightStateIdx(edgeIdx);
				if (rightIdx != Lattice.NULL_IDX)
					right[rightIdx] += right[leftIdx] + 1;
				
				// increment: count = 1 + # of previous paths going into this state
				witnessCount[edgeIdx] += right[leftIdx] + 1;
			}
		} // end loop over position
		
		//
		
		// left: backward pass
		//   --> record # of times each state is listed as an alpha argmax
		for (int pos = this.length()-1; pos >= 0; pos--) {
			
			int start = this.getStateOffset(pos);
			int end = this.getStateOffset(pos+1);

			for (int idx = start; idx < end; idx++) {
				int edgeIdx = alphaArgs[idx];
				
				int leftIdx =this.getLeftStateIdx(edgeIdx); 
				int rightIdx = this.getRightStateIdx(edgeIdx);
				
				if (leftIdx != Lattice.NULL_IDX)
					left[leftIdx] += left[rightIdx] + 1;
				
				// increment: count = 1 + # of previous paths going into this state
				witnessCount[edgeIdx] += left[rightIdx] + 1;
			}
		} // end position loop
	}

	/**
	 * Replaces the serialization logic.
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void write(DataOutput out) throws IOException {
		
		out.writeLong(classID);
		
		out.writeLong(serialVersionUID);
		out.writeInt(seqHash);
		out.writeInt(length);
		ArrayUtil.writeIntArray(out, stateIDs);
		ArrayUtil.writeIntArray(out, statePosOffsets);
		ArrayUtil.writeIntArray(out, edgePosOffsets);
		ArrayUtil.writeIntArrayAA(out, leftEdgeIdx);
		ArrayUtil.writeIntArrayAA(out, rightEdgeIdx);
		ArrayUtil.writeIntArray(out,edgePosOffsets);
		ArrayUtil.writeIntArray(out,edgeLeftStates);
		ArrayUtil.writeIntArray(out,edgeRightStates);
		ArrayUtil.writeFeatureVectorArray(out,fv);
		// scores are never saved. 
		
		
	}

	/**
	 * Find the corresponding position of the state linearly indexed by <b>idx</b>.
	 * 
	 * Throws a RuntimeException if the index is out of range of the statePosOffsets array.
	 * 
	 * @param idx
	 * Linear state index 
	 */
	public int findStatePosOffset(int idx) {

		int pos = 0;

		while (idx >= statePosOffsets[pos]) {
			if (pos >= statePosOffsets.length)
				throw new RuntimeException("index " + idx + " is outside the range of states for this lattice");
			else
				pos++;
		}
		
		return pos-1; // pos will have advanced one extra step;
	}

	/**
	 * Find the corresponding position of the edge linearly indexed by <b>idx</b> (see findStatePosOffset)
	 */
	public int findEdgePosOffset(int idx) {
		int pos = 0;

		while (idx >= edgePosOffsets[pos]) {
			if (pos >= edgePosOffsets.length)
				throw new RuntimeException("index " + idx + " is outside the range of edge for this lattice");
			else
				pos++;
		}
		
		return pos-1; // pos will have advanced one extra step;
	}


}
