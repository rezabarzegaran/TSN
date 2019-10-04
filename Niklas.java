package TSN;
import java.util.Optional;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Niklas extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	
	IntVar[][] O;
	IntVar[][] L;
	IntVar[][] P;
	IntVar[] Costs;
	OptimizeVar OptVar;
	int TotalRuns = 0;
	int NOutports;
	int TotalVars;
	
	
	
	public Niklas(Solver _solver) {
		solver = _solver;
        TotalRuns = 0;
	}
	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}
	public void setInit(Solution init) {
		Current = init;
	}
	public void initVariables() {
		NOutports = Current.getNOutPorts();
		O = new IntVar[NOutports][];
		L = new IntVar[NOutports][];
		P = new IntVar[NOutports][];
		Costs = new IntVar[3];
		TotalVars = AssignVars(O, L, P);
	}
	public void addConstraints() {

	}
	public void addCosts() {

	}
	public void addDecision() {

	}
	public void addSolverLimits() {

	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public Solution cloneSolution() {
		return Current;
	}
	private int AssignVars(IntVar[][] O, IntVar[][] L, IntVar[][] P) {
		int counter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int usedQ = port.getUsedQ();
					O[counter] = new IntVar[usedQ];
					L[counter] = new IntVar[usedQ];
					P[counter] = new IntVar[usedQ];
					
					for (int i = 0; i < usedQ; i++) {
						
						Totalvars++;
					}				
					
					counter++;
				}
			}
		}
		return Totalvars;
	}

}
