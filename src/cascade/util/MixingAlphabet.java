package cascade.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MixingAlphabet extends Alphabet {
		
	public String [] reverseLookup;
	public int capacity;

	public MixingAlphabet(){
	}
	
	public MixingAlphabet(int capacity) {
		this(capacity,true);
	}

	public MixingAlphabet(int capacity, boolean keepReverseMap) {
		this.capacity = capacity;
		if (keepReverseMap) reverseLookup = new String[capacity];
	}
	
	/** No such thing as not being present. */
	public int lookupIndex(String entry) {
		int res = entry.hashCode()%capacity;
		if (res < 0) res+=capacity;
		if (reverseLookup!=null) reverseLookup[res] = entry;
		return res;
	}

	public Object[] toArray() {
		return reverseLookup;
	}

	public boolean contains(String entry) {
		// FIXME: what is this used for?
		return true;
	}

	public int size() {
		return capacity;
	}

	public void stopGrowth() {
	}

	public void allowGrowth() {
	}

	public boolean growthStopped() {
		return true;
	}

	// Serialization

	private static final long serialVersionUID = 2L;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeInt(capacity);
		out.writeObject(reverseLookup);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		@SuppressWarnings("unused")
		
		int version = in.readInt();
		capacity = in.readInt();
		reverseLookup = (String[]) in.readObject();
	}

	@Override
	public String toString() {
		return "Mixing Alphabet [capacity = "+capacity+"]";
		
	}
	
	public String [] getKeysInOrder() {
		return reverseLookup;
	}

	public String reverseLookup(int idx) {
		if(reverseLookup != null) return reverseLookup[idx];
		return null;
	}

	
}
