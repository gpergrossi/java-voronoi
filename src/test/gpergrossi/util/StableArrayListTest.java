package test.gpergrossi.util;

import java.util.List;
import java.util.Scanner;
import java.util.function.IntFunction;

import com.gpergrossi.util.data.StableArrayList;

public class StableArrayListTest {
		
	public static void main(String[] args) {
		
		System.out.println("Press enter to continue");
		try (Scanner sc = new Scanner(System.in)) {
			sc.nextLine();
		}

		IntFunction<String[]> allocator = (size -> { return new String[size]; });
		
		for (int num = 100; num <= 1000000; num += 500) {
			List<String> all = new StableArrayList<>(allocator, 8);
			for (int i = 0; i < num; i++) {
				String d = Integer.toHexString(i);
				all.add(d);
			}
			
			long start = 0, end = 0;
			System.gc();
			
			start = System.nanoTime();
			for (int i = 0; i < num; i++) {
				all.remove(i);
			}
			end = System.nanoTime();

			long dur = end-start;
			double time = dur*0.000000001;
			System.out.println(num+", "+time);
		}
	}
	
}
