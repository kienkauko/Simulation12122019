package fil.testJava;

public class TestArray {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int []a = {1,2,3};
		System.out.println("before: ");
		printArray(a);
		test(a);
		System.out.println("after: ");
		printArray(a);
	}
	
	public static void test(int [] pass) {
		pass[0] ++;
	}
	
	public static void printArray(int[] a) {
      System.out.print("[ ");
      for (int i = 0; i < a.length; i++) {
         System.out.print(a[i] + " ");
      }
      System.out.println("]");
   }
}
