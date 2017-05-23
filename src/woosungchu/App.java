package woosungchu;

import java.io.File;

public class App {
	
	private static final String A = "cash.wav";
	private static final String B = "outside.wav";
	private static final String C = "puth.wav";
	private static final String D = "JessieJBangBang.wav";
	
	public static void main(String[] args) {
		Lora runner = new Lora();
		
		runner.store(new File(A),111);
		runner.store(new File(B),222);
		runner.store(new File(C),333);
		runner.store(new File(D),444);
		
		runner.run(new File(C));
	}
	
	


}
