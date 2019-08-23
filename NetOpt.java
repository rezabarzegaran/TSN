package TSN;

import java.util.ArrayList;
import java.util.List;

public class NetOpt {

	public static void main(String[] args) {
        
		//DataConverter converter = new DataConverter();
		//converter.txt2xml("orion.streams", "orion.vls", "src/TSN/orion.xml");
		
		
		
		
        //String inputPath = "src/TSN/input.xml";
        //String inputPath = "src/TSN/GM.xml";
        String inputPath = "src/TSN/GMM.xml";
        List<Solution> OptimizedSolutions = new ArrayList<Solution>();
        
        
        
        DataLoader inData = new DataLoader();
        inData.Load(inputPath);
        Solution initial_Solution = new Solution();
        String solutionName = "Romon";
        ORSolver optimizer = new ORSolver(solutionName);
        initial_Solution.Create(inData.getMessages(), inData.getRoutes(), inData.getApps());
        initial_Solution.Initialize();
        optimizer.setSolution(initial_Solution);
        optimizer.Run();
        OptimizedSolutions = optimizer.getOptimizedSolutions();
        System.out.println("total Solution is: " + OptimizedSolutions.size() );
        //System.out.println("Best Cost is " + OptimizedSolutions.get(OptimizedSolutions.size() - 1).getCost());
        
        //DataUnloader outData = new DataUnloader();
        //outData.UnloadAll(OptimizedSolutions, solutionName);
        inData.UnloadLuxi();
        inData.exportGraph();
        

        

        //System.out.println("Cost 0 is " + current_Solution.costValues[0]);
        //System.out.println("Cost 1 is " + current_Solution.costValues[1]);

	}

}
