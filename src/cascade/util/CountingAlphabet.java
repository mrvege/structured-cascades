package cascade.util;

public class CountingAlphabet extends Alphabet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int [] counts;
	private int junkid = -1;
	
	@Override
	public int lookupIndex(String entry) {
		
		int idx = super.lookupIndex(entry);
		if (!growthStopped){
		
			counts = ArrayUtil.ensureCapacity(counts, numEntries);
			counts[idx]++;
			
		}
		
		return idx;
	}

	public int getJunkId() { return junkid; }
	
	public int getCount(String key) {
		int idx = lookupIndex(key);
		return (idx == -1 ) ? -1 : counts[idx];
	}
	
	
	/**
	 * Generates a new Alphabet from the current CountingAlphabet by remapping all keys below some cutoff to a single new key.
	 * @param junkCutoff the cutoff threshold
	 * @param key what to replace the keys with
	 * @return
	 */
	public Alphabet remapInfrequentKeys (int junkCutoff, String key) {
	
		CountingAlphabet a = new CountingAlphabet();
		a.allowGrowth();
			
		// figure out which tags survive
		for (int i = 0; i < size(); i++) 
			if (counts[i] > junkCutoff) { 
				int idx =a.lookupIndex(reverseLookup(i)); 
				a.counts[idx] = counts[i];
			}

		// introduce the junktag
		junkid = a.lookupIndex(key);
		a.stopGrowth();

		// remap all junk tags to the junktag
		for (int i = 0; i < size(); i++) 
			if (counts[i] <= junkCutoff)
				a.addExtraMapping(reverseLookup(i), junkid);

		return a;
	}
}
