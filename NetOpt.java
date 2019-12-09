package TSN;

import TSN.ORSolver.methods;

public class NetOpt {

	public static void main(String[] args) {
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//Test Cases
        String testcase = "src/TestCases/Jorge/JorgeCase1.xml";

        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Loading Data
        DataLoader dataLoader = new DataLoader();
        dataLoader.Load(testcase);
        //Method call for old input version
        //dataLoader.Load(testcase9a, testcase9b);  

        //Loading Completed
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Creating Solutions
        Solution initial_Solution = new Solution(dataLoader);
         
        //Solution Created
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Creating Solver

        //Select Between Methods
        methods chosenMethods = methods.Jorge;
        
        boolean debugmode = false;
        
        ORSolver optimizer = new ORSolver(chosenMethods, initial_Solution, debugmode);
        
        
        //Run optimizer
        optimizer.Run();
        
        //optimization Finished
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	}

}
