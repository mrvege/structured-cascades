package cascade.lattice;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import cascade.io.Sequence;
import cascade.model.CascadeModel;
import cascade.model.ZeroOrderModel;
import cascade.model.ZeroOrderPOS;
import cascade.util.ArrayUtil;

/**
 * A special implementation of the Lattice object for the case where no edges are needed to be stored.
 * Saves space and time by never explicitly creating edge idx's.
 *
 */
public class ZeroOrderLattice extends Lattice {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	public static final long classID = 1;
	
	
	public ZeroOrderLattice(DataInput in) throws IOException {

		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);
		seqHash = in.readInt();
		length = in.readInt();
		
		statePosOffsets = ArrayUtil.readIntArray(in);
		stateIDs = ArrayUtil.readIntArray(in);
		fv = ArrayUtil.readFeatureVectorArray(in);
		
		// scores are never saved
		stateScores = null;
		edgeScores = null;
	}
	
	/**
	 * Builds a zero order lattice for a given sequence (i.e. no edges)
	 * 
	 * @param seq
	 * @param model
	 */
	public ZeroOrderLattice(Sequence seq, ZeroOrderModel m){
		super(seq, m);
		
		length = seq.length();
		
		statePosOffsets = new int[length + 1];
		edgePosOffsets = new int[length + 2]; // all edges offsets are zero because there are no edges
		
		leftEdgeIdx = null;
		rightEdgeIdx = null;
		edgeLeftStates = null;
		edgeRightStates = null;
				
		for (int pos = 0; pos < length; pos++) {
			statePosOffsets[pos+1] = statePosOffsets[pos]+m.getNumberOfStates(seq, pos);
		}
		
		// FIXME: the part below was commented out.  Why? 
		// I uncommented it and brought it up to date, because otherwise
		// decoding didn't work -- Kuzman
		stateIDs = new int[statePosOffsets[length]];

		int idx = 0;
		for (int pos = 0; pos < length; pos++) {
			int[] states = m.possibleStates(seq, pos);
			for (int s = 0; s < states.length; s++)
				stateIDs[idx++] = states[s];
		}
		
		
	}	
	
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(classID);
		out.writeLong(serialVersionUID);
		out.writeInt(seqHash);
		out.writeInt(length);

		ArrayUtil.writeIntArray(out, statePosOffsets);
		ArrayUtil.writeIntArray(out, stateIDs);
		ArrayUtil.writeFeatureVectorArray(out,fv);
	}
	
	public String fv2string(){
		StringBuilder sb = new StringBuilder();
		for (int pos = 0; pos < statePosOffsets.length -1; pos++) {
			int start = statePosOffsets[pos];
			int end = statePosOffsets[pos+1];
			for (int stateIdx = start; stateIdx < end; stateIdx++) {
				sb.append("["+pos+"|"+stateIdx+"] ");
				if (fv != null){
					for(int k:fv[stateIdx].getKeys())
						sb.append(super.model.featureAlphabet.reverseLookup(k)+" ");
				}
				sb.append("\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public void print(boolean[] mask){
		System.out.printf("Sequence dump: length %d\n", length);
		for (int pos = 0; pos < length; pos++) {
			System.out.printf("States at position %d:\n", pos);

			int start = getStateOffset(pos);
			int end = getStateOffset(pos + 1);

			for (int idx = start; idx < end; idx++) {
//				if (model == null) {
//					System.out.printf("[%d]: state %d\n", idx, getStateID(idx));
//				} else {
//					
				if(mask[idx])
					System.out.printf("[%d]: state %s\n", idx, model.stateToString(this, getStateID(idx)));
//					if (fv != null && fv.length == getNumStates()) 
//						System.out.println("Features: " + fv[idx].toString(model.featureAlphabet));
//				}

			}
		}
		
	}

	@Override
	public void print() {
		
		System.out.printf("Sequence dump: length %d\n", length);
		System.out.printf("State offsets: %s\n", Arrays
				.toString(statePosOffsets));
		System.out
				.printf("Edge offsets: %s\n", Arrays.toString(edgePosOffsets));

		for (int pos = 0; pos < length; pos++) {

			System.out.printf("States at position %d:\n", pos);

			int start = getStateOffset(pos);
			int end = getStateOffset(pos + 1);

			for (int idx = start; idx < end; idx++) {

				if (model == null) {
					System.out.printf("[%d]: state %d\n", idx, getStateID(idx));
				} else {
					
					System.out.printf("[%d]: state %s\n", idx, model.stateToString(this, getStateID(idx)));
					if (fv != null && fv.length == getNumStates()) 
						System.out.println("Features: " + fv[idx].toString(model.featureAlphabet));
				}

			}
		}
	}

}
