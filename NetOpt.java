package TSN;

import java.util.ArrayList;
import java.util.List;

public class NetOpt {

	public static void main(String[] args) {
        
        String inputPath = "src/TSN/input.xml";
        List<Solution> OptimizedSolutions = new ArrayList<Solution>();
        
        
        
        DataLoader inData = new DataLoader();
        inData.Load(inputPath);
        Solution initial_Solution = new Solution();
        ORSolver optimizer = new ORSolver("Romon");
        initial_Solution.Create(inData.getMessages(), inData.getRoutes());
        initial_Solution.Initialize();
        optimizer.setSolution(initial_Solution);
        optimizer.Run();
        OptimizedSolutions = optimizer.getOptimizedSolutions();
        System.out.println("total Solution is: " + OptimizedSolutions.size() );

        //System.out.println("Cost 0 is " + current_Solution.costValues[0]);
        //System.out.println("Cost 1 is " + current_Solution.costValues[1]);

	}

}
