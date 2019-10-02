package TSN;

import TSN.ORSolver.methods;

public class NetOpt {

	public static void main(String[] args) {
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//Test Cases
        String testcase1 = "src/TSN/input.xml";
        String testcase2 = "src/TSN/input1.xml";
        String testcase3 = "src/TSN/input2.xml";
        String testcase4 = "src/TSN/orion.xml";
        String testcase5 = "src/TSN/GM.xml";
        String testcase6 = "src/TSN/GMM.xml";
        String testcase7 = "src/TSN/testcase1.xml";
        String testcase8 = "src/TSN/testcase2.xml";
        String testcase9a = "src/TSN/orion.streams";
        String testcase9b = "src/TSN/orion.vls";
        String testcase10 = "src/TSN/GMM2.xml";
		
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		// Loading Data
        DataLoader dataLoader = new DataLoader();
        dataLoader.Load(testcase10);
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
        
        boolean debugmode = true;
        
        ORSolver optimizer = new ORSolver(chosenMethods, initial_Solution, debugmode);
        
        
        //Run optimizer
        optimizer.Run();
        
        //optimization Finished
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	}

}
