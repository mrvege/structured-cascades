/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

/** 
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package cascade.util;

import gnu.trove.TObjectIntProcedure;

import java.io.*;


public class Alphabet implements Serializable {

	static int DEFAULT_CAPACITY = 1000;
		
	public gnu.trove.TObjectIntHashMap<String> map;
	public String [] reverseLookup;
	
	int numEntries = 0;
	boolean growthStopped = false;

	public Alphabet(int capacity) {
		this.map = new gnu.trove.TObjectIntHashMap<String>(capacity);
		reverseLookup = new String[capacity];
	}

	public Alphabet() {
		this(DEFAULT_CAPACITY);
	}
	
	/** Return -1 if entry isn't present. */
	public int lookupIndex(String entry) {
	
		if (entry == null) {
			throw new IllegalArgumentException(
					"Can't lookup \"null\" in an Alphabet.");
		}

		// System.out.println("feature lookup:" + entry.toString());

		if (map.contains(entry)) 
			return map.get(entry);
		else if (!growthStopped){

			int id = numEntries++;
			map.put(entry, id);
			
			reverseLookup = ArrayUtil.ensureCapacity(reverseLookup, numEntries);
			reverseLookup[id] = entry;
			
			return id;
		} else
			return -1;
		
	}

	public Object[] toArray() {
		return map.keys();
	}

	public boolean contains(String entry) {
		return map.contains(entry);
	}

	public int size() {
		return numEntries;
	}

	public void stopGrowth() {
		growthStopped = true;
		map.compact();
	}

	public void allowGrowth() {
		growthStopped = false;
	}

	public boolean growthStopped() {
		return growthStopped;
	}

	// Serialization

	private static final long serialVersionUID = 2L;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeInt(numEntries);
		out.writeObject(map);
		out.writeObject(reverseLookup);
		out.writeBoolean(growthStopped);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		@SuppressWarnings("unused")
		
		int version = in.readInt();
		numEntries = in.readInt();
		map = (gnu.trove.TObjectIntHashMap<String>) in.readObject();
		reverseLookup = (String[]) in.readObject();
		growthStopped = in.readBoolean();
	}

	@Override
	public String toString() {
		return "Alphabet [growthStopped=" + growthStopped + ", numEntries="
				+ numEntries + "]\nMAP: " + map;
		
	}
	
	public String [] getKeysInOrder() {

		MappingProcedure mapper = new MappingProcedure(this);
		map.forEachEntry(mapper);
		 
		return mapper.keys;
	}

	public String reverseLookup(int idx) {
		if (idx < 0)
			return null;
		else return reverseLookup[idx];
	}

	/**
	 * Adds an extra mapping from a given key string to a given int val. NOTE:
	 * This is potentially VERY UNSAFE, as it does not increase the SIZE of the alphabet.
	 * 
	 * @param key
	 * @param id
	 */
	public void addExtraMapping(String key, int id) {
		map.put(key, id);
	}
	
}

class MappingProcedure implements TObjectIntProcedure<String> {

	Alphabet a;
	public String [] keys;
	public MappingProcedure(Alphabet alphabet) {
		this.a = alphabet;
		keys = new String[a.size()];
	}

	public boolean execute(String key, int val) {
		keys[val] = key;
		return true;
	}
	
}
