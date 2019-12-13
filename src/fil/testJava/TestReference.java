package fil.testJava;

public class TestReference {
	
	public static void test(Integer a) {
		Integer b = 5;
		a = b;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Integer a = 100;
		
		System.out.println("a before is: " + a);
		test(a);
		System.out.println("a after is: " + a);
	}

}
