package cascade.util;

import cascade.lattice.Lattice;

public class RunTimeEstimatorForLattices extends RunTimeEstimator {

	double numStates;
	double numEdges;
	double numPos;
	
	public RunTimeEstimatorForLattices(int T, double updatePercentage) {
		super(T, updatePercentage);
	}
	
	public void tallyLattice(Lattice l) {
		numStates += l.getNumStates();
		numEdges += l.getNumEdges();
		numPos += l.length();
	}

	@Override
	public String getExtraInfo() {
		return String.format("[%.2f states/pos, %.2f edges/states, %.2f edges/pos]", numStates/numPos, numEdges/numStates, numEdges/numPos);
	}

	

}
