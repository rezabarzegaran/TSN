package TSN;

public class NetOpt {

	public static void main(String[] args) {
		
		
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//Input Data Loader
        DataLoader inData = new DataLoader();
		// Use the Converter If you need to Convert the test cases form Niklas to .xml
        
		//DataConverter converter = new DataConverter();
		//converter.txt2xml("orion.streams", "orion.vls", "src/TSN/orion.xml");
		
		
		//Loading test Cases
        //String inputPath = "src/TSN/input.xml";
        //String inputPath = "src/TSN/input1.xml";
        //String inputPath = "src/TSN/input2.xml";
        //String inputPath = "src/TSN/orion.xml";
        //String inputPath = "src/TSN/GM.xml";
        //String inputPath = "src/TSN/GMM.xml";
        //String inputPath = "src/TSN/testcase1.xml";
        String inputPath = "src/TSN/testcase2.xml";
        

        inData.Load(inputPath);

        //Loading Completed
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Creating Solutions
 
        Solution initial_Solution = new Solution();
        initial_Solution.Create(inData.getMessages(), inData.getRoutes(), inData.getApps());
        initial_Solution.Initialize();
         
        //Solution Created
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Creating Solver

        //Select Between Methods
        
        String solutionName = "Romon";
        //String solutionName = "Niklas";
        //String solutionName = "Silviu";
        ORSolver optimizer = new ORSolver(solutionName);
        
        
        //Run optimizer
        optimizer.setSolution(initial_Solution);
        boolean debugmode = true;
        optimizer.Run(debugmode);
        
        //optimization Finished
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Creating Reports
        //Create Input Related Reports
       inData.CreateInputReport();
        
        //Create Best Solution Report
        
          

	}

}
