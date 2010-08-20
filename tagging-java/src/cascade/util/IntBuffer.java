package cascade.util;

import java.util.Arrays;

public class IntBuffer {

	private int [] data;
	private int pos;

	public IntBuffer(int capacity) {

		data = new int[capacity];
		pos = 0;
		
	}
	public void add(int i){
		data[pos++] = i;
	}
	
	public int size() {
		return pos;
	}
	
	public void reset(int pos){
		this.pos = pos;
	}
	
	public void reset() {
		reset(0);
	}
	
	public int [] toNativeArray() {
		return Arrays.copyOf(data, pos);
	}
	
	public static void main(String args[]) {
		
		// test
		
		IntBuffer i = new IntBuffer(100);
		
		i.add(0);
		i.add(1);
		i.add(2);
		i.add(3);
		i.add(4);
		i.add(5);
		
		System.out.println(Arrays.toString(i.toNativeArray()));
	}
	
}
