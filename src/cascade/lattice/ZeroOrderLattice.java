package cascade.lattice;

import gnu.trove.TIntArrayList;

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
	private static final long serialVersionUID = 4L;
	public static final long classID = 1;

	/**
	 * Whether or not the actual state IDs are stored (they aren't strictly necessary)
	 */
	public boolean storeStateIDs = true;
	private int _nstates;
	
	public ZeroOrderLattice(DataInput in) throws IOException {

		long id=in.readLong();
		if (id!= serialVersionUID) throw new IOException("Wrong serial version, got "+id);
		seqHash = in.readInt();
		length = in.readInt();
		_nstates = in.readInt();
		storeStateIDs = in.readBoolean();
		
		statePosOffsets = ArrayUtil.readIntArray(in);
		if (storeStateIDs)
			stateIDs = ArrayUtil.readIntArray(in);
		fv = ArrayUtil.readFeatureVectorArray(in);
		fvPos = ArrayUtil.readFeatureVectorArray(in);
		fvState = ArrayUtil.readFeatureVectorArray(in);
		
		// scores are never saved
		stateScores = null;
		edgeScores = null;
	}
	
	public ZeroOrderLattice(Sequence seq, ZeroOrderModel m) {
		this(seq, m, false);
	}
	
	public ZeroOrderLattice(Sequence seq, ZeroOrderModel m, boolean [][] mask) {
		super(seq, m);
		
		length = seq.length();
		
		statePosOffsets = new int[length + 1];
		edgePosOffsets = new int[length + 2]; // all edges offsets are zero because there are no edges
		
		leftEdgeIdx = null;
		rightEdgeIdx = null;
		edgeLeftStates = null;
		edgeRightStates = null;

		// we WILL be storing state IDs
		storeStateIDs = true;
		
		TIntArrayList newStateIDs = new TIntArrayList();

		for (int pos = 0; pos < length; pos++) {
			int[] states = m.possibleStates(seq, pos);
			for (int s = 0; s < states.length; s++)
				if (mask[pos][s]) 
					newStateIDs.add(s);
			
			statePosOffsets[pos+1] = newStateIDs.size();
		}
		
		stateIDs = newStateIDs.toNativeArray();
	}

	/**
	 * Builds a zero order lattice for a given sequence (i.e. no edges)
	 * 
	 * @param seq
	 * @param model
	 */
	public ZeroOrderLattice(Sequence seq, ZeroOrderModel m, boolean storeIDs){
		super(seq, m);
		
		length = seq.length();
		
		statePosOffsets = new int[length + 1];
		edgePosOffsets = new int[length + 2]; // all edges offsets are zero because there are no edges
		
		leftEdgeIdx = null;
		rightEdgeIdx = null;
		edgeLeftStates = null;
		edgeRightStates = null;

		storeStateIDs = storeIDs;
	
		if (storeStateIDs) {

			// store only VALID states at each position
			TIntArrayList newStateIDs = new TIntArrayList();
			for (int pos = 0; pos < length; pos++) {
				int[] states = m.possibleStates(seq, pos);
				
				newStateIDs.add(states);
				statePosOffsets[pos+1] = newStateIDs.size();
			}
			
			stateIDs = newStateIDs.toNativeArray();
			
		} else {
			// # of states shoudl NOT change for a given position
			_nstates = m.getNumberOfStates(seq, 0);
			for (int pos = 0; pos < length; pos++) {
				statePosOffsets[pos+1] = statePosOffsets[pos]+_nstates;
			}

			stateIDs = null;
		}
		
	}	
	
	@Override
	public int getStateID(int idx) {
		
		if (storeStateIDs)
			return stateIDs[idx];
		else
			return idx - ((idx/_nstates)*_nstates);
	}

	@Override
	public int[] getArgmaxStates(int[] alphaArgs, double[] edgeMarginalVals) {
		// TODO Auto-generated method stub
		return super.getArgmaxStates(alphaArgs, edgeMarginalVals);
	}

	@Override
	public int findStatePosOffset(int idx) {
		// TODO Auto-generated method stub
		return super.findStatePosOffset(idx);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(classID);
		out.writeLong(serialVersionUID);
		out.writeInt(seqHash);
		out.writeInt(length);
		out.writeInt(_nstates);
		out.writeBoolean(storeStateIDs);

		ArrayUtil.writeIntArray(out, statePosOffsets);
		if (storeStateIDs)
			ArrayUtil.writeIntArray(out, stateIDs);
		
		ArrayUtil.writeFeatureVectorArray(out,fv);
		ArrayUtil.writeFeatureVectorArray(out,fvPos);
		ArrayUtil.writeFeatureVectorArray(out,fvState);
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
					//System.out.printf("[%d]: state %d\n", idx, getStateID(idx));
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

			if (fv != null && fv.length == length) {
				System.out.printf("Features at position %d:\n", pos);
				System.out.println("\t" + fv[pos].toString(model.featureAlphabet));
			}

			System.out.printf("States at position %d:\n", pos);

			int start = getStateOffset(pos);
			int end = getStateOffset(pos + 1);

			for (int idx = start; idx < end; idx++) {

				if (model == null) {
					System.out.printf("[%d]: state %d\n", idx, getStateID(idx));
				} else {
					
					System.out.printf("[%d]: state %s", idx, model.stateToString(this, getStateID(idx)));
					if (stateScores != null)
						System.out.printf(" score=%g ", stateScores[idx]);
					if (fv != null && fv.length == getNumStates()) 
						System.out.print("\nFeatures: " + fv[idx].toString(model.featureAlphabet));
					
					System.out.println();
				}

			}
		}
	}

}
