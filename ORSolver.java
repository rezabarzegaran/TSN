package TSN;

import java.util.ArrayList;
import java.util.List;

import com.google.ortools.constraintsolver.Solver;

public class ORSolver {

    Solver solver;
    Solution Current;
    List<Solution> optSolutions;
    DataUnloader outData = new DataUnloader();
    String name;
    
    
    Romon method;
    //Niklas method;
    //Michele method;
    
	static { System.loadLibrary("jniortools");}

	public ORSolver(String _method) {
		//model = new CpModel();
	    solver = new Solver(_method);
	    name = _method;
	    optSolutions = new ArrayList<Solution>();
	    switch (_method) {
		case "Romon":
			method = new Romon(solver);
			break;
		case "Niklas":
			//method = new Niklas(solver);
			break;
		case "Michele":
			//method = new Micehle(solver);
			break;
		default:
			method = new Romon(solver);

		}
		
	}
	public void setSolution(Solution current) {
		Current = current;
	}
	public void Run(boolean debogmode) {
		
		method.setInit(Current);
		method.initVariables();
		
		method.addConstraints();
		
		method.addCosts();

		
		method.addDecision();
		
		
		
	    solver.newSearch(method.getDecision(),method.Opt1, method.Opt2, method.Opt3);
	    //solver.newSearch(method.getDecision(),method.Opt1, method.Opt2);
	    System.out.println(solver.model_name() + " Initiated");
    
	    //int counter = 0;
	    //while (solver.wallTime() <= 15000) {
	    	//solver.nextSolution();
			////optSolutions.add(method.cloneSolution());
	    	//outData.UnloadOnce(method.cloneSolution(), name, counter);
		    //counter++;

	    //}
	    
	    
	    Solution optimomSolution = null;
	    long start=System.currentTimeMillis();
	    while (solver.nextSolution()) {
	    	
	    	if(debogmode) {
	    		//optSolutions.add(method.cloneSolution());
	    		outData.UnloadOnce(method.cloneSolution(), name, method.TotalRuns);
	    	}else {
	    		optimomSolution = method.cloneSolution();
	    	}
			
	    	
	    	if(method.Monitor(start)) {
	    		break;
	    	}

	    }
    	if(!debogmode) {
    		//optSolutions.add(method.cloneSolution());
    		outData.UnloadOnce(optimomSolution, name, method.TotalRuns);
    	}
	    solver.endSearch();

	    long end=System.currentTimeMillis();
	    int duration = (int) (end - start);
	    outData.CreateReport(duration);
    	

	    // Statistics
	    System.out.println();
	    System.out.println("Solutions: " + solver.solutions());
	    System.out.println("Failures: " + solver.failures());
	    System.out.println("Branches: " + solver.branches());
	    System.out.println("Wall time: " + solver.wallTime() + "ms");

	}
}
