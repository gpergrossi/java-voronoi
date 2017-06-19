package dev.mortus.test;

import java.util.ArrayList;

public class ArrayListTest {

	static class Dummy {}
	
	public static void main(String[] args) {
		for (int num = 100; num <= 1000000; num += 100) {
						
			ArrayList<Dummy> all = new ArrayList<>();
			ArrayList<Dummy> toFind = new ArrayList<>();
			for (int i = 0; i < num; i++) {
				Dummy d = new Dummy();
				all.add(d);
				toFind.add(d);
			}
			
			long start = 0, end = 0;
			System.gc();
			
			int i = 0;
			start = System.nanoTime();
			for (Dummy d : toFind) {
				if (all.contains(d)) i++;
			}
			if (i != all.size()) System.err.println("test error");
			end = System.nanoTime();

			long dur = end-start;
			double time = dur*0.000000001;
			System.out.println(num+", "+time);
		}
	}
	
}
