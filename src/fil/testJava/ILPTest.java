package fil.testJava;

import ilog.concert.*;
import ilog.cplex.*;

/**
 * Farm LP example.
 * 
 * @author trucvietle
 *
 */
public class ILPTest {
	public static void main(String[] args) {
		int n = 3;
		int m = 4;
		double [] c = {1, 0, 0};
		double[][]A = {{3, 8, 13.6}, {47.3, -31, -16.3}, {-1, 1, 0}, {0, -1, 1}};
		double[] b = {100, 100, 0, 0}; // capacity constraints
		solveModel(n, m, c, A, b);
	}
	
	/**
	 * Defines the solves the LP.
	 * 
	 * @param n Number of variables
	 * @param m Number of constraints
	 * @param c Cost vector
	 * @param A Constraint coefficient matrix
	 * @param b Capacity constraint vector
	 */
	public static void solveModel(int n, int m, double[] c,
			double[][] A, double[] b) {
		try {
			// Instantiate an empty model
			IloCplex model = new IloCplex();
			
			// Define an array of decision variables
			IloNumVar[] x = new IloNumVar[n];
			for(int i = 0; i < n; i++) {
				// Define each variable's range from 0 to +Infinity
//				x[i] = model.numVar(0, Double.MAX_VALUE);
				x[i] = model.intVar(0, Integer.MAX_VALUE);
			}
			
			// Define the objective function
			IloLinearNumExpr obj = model.linearNumExpr();
			// Add expressions to the objective function
			for(int i = 0; i < n; i++) {
				obj.addTerm(c[i], x[i]);
			}
			// Define a maximization problem
			model.addMaximize(obj);
			
			// Define the constraints
			for(int i = 0; i < m; i++) { // for each constraint
				IloLinearNumExpr constraint = model.linearNumExpr();
				for(int j = 0; j < n; j++) { // for each variable
					constraint.addTerm(A[i][j], x[j]);
				}
				// Define the RHS of the constraint
				model.addLe(constraint, b[i]);
			}
			
			// Suppress the auxiliary output printout
			model.setParam(IloCplex.IntParam.SimDisplay, 0);
			
			// Solve the model and print the output
			boolean isSolved = model.solve();
			if(isSolved) {
				double objValue = model.getObjValue();
				System.out.println("obj_val = " + objValue);
				
				for(int i = 0; i < n; i++) {
					System.out.println("x[" + (i+1) + "] = " + model.getValue(x[i]));
				}
			} else {
				System.out.println("Model not solved :(");
			}
		} catch(IloException ex) {
			ex.printStackTrace();
		}
		double [] result = model.getValue(x);
	}
}