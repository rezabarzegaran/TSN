package TSN;

import java.util.ArrayList;
import java.util.List;

import com.google.ortools.constraintsolver.Solver;

public class ORSolver {
	//CpModel model;
    Solver solver;
    Solution Current;
    List<Solution> optSolutions;
    Romon method;
    //Niklas method;
	static { System.loadLibrary("jniortools");}

	public ORSolver(String _method) {
		//model = new CpModel();
	    solver = new Solver(_method);
	    optSolutions = new ArrayList<Solution>();
	    switch (_method) {
		case "Romon":
			method = new Romon(solver);
			break;
		case "Niklas":
			//method = new Niklas(solver);
			break;
		default:
			method = new Romon(solver);

		}
		
	}
	public void setSolution(Solution current) {
		Current = current;
	}
	public void Run() {
		
		method.setInit(Current);
		method.initVariables();
		
		method.addConstraints();
		
		method.addCosts();

		
		method.addDecision();
		
		System.out.println(solver.model_name() + " Initiated");
		
		
	    
	    solver.newSearch(method.getDecision());

 
	    
	    
	    while (solver.wallTime() <= 1000) {
	    	solver.nextSolution();
			optSolutions.add(method.cloneSolution());
		    

	    }
	    solver.endSearch();
    	

	    // Statistics
	    System.out.println();
	    System.out.println("Solutions: " + solver.solutions());
	    System.out.println("Failures: " + solver.failures());
	    System.out.println("Branches: " + solver.branches());
	    System.out.println("Wall time: " + solver.wallTime() + "ms");

	

	}






	
	public List<Solution> getOptimizedSolutions(){
		return optSolutions;
	}
}
