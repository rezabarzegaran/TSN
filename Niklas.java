package TSN;
import java.util.Optional;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.Solver;

public class Niklas extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Niklas(Solver _solver) {
		solver = _solver;
	}
	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}
	public void setInit(Solution init) {
		Current = init;
	}
	public void initVariables() {

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

}
