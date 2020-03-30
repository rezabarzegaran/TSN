package TSN;
import java.security.KeyStore.PasswordProtection;
import java.util.*;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.IntVarElement;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;
import com.sun.tools.classfile.StackMap_attribute.stack_map_frame;


public class Niklas extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	OptimizeVar OptVar;
	int NOutports;
	int TotalVars;
	IntVar[] Costs;
	OptimizeVar costVar;
	int TotalRuns = 0;
	IntVar[][] Wperiod;
	IntVar[][] Wlength;
	IntVar[][] Woffset;
	
	
	
	public Niklas(Solver _solver) {
		solver = _solver;
        TotalRuns = 0;
	}

	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}


	public void setInit(Solution init)
	{
		Current = init;
	}


	public void initVariables() {
		NOutports = Current.getNOutPorts();
		Wperiod = new IntVar[NOutports][];
		Wlength = new IntVar[NOutports][];
		Woffset = new IntVar[NOutports][];
		Costs = new IntVar[4];
		TotalVars = AssignVars(Wperiod, Wlength, Woffset);
	}

	public void addConstraints() {
		FrameConstraint(Wperiod, Wlength, Woffset);
		LinkConstraint(Wperiod, Wlength, Woffset);
		FlowTransmissionConstraint(Wperiod, Wlength, Woffset);
		EndtoEndConstraint(Wperiod, Wlength, Woffset);
	}
	public void FrameConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < 7; i++) {
						IntVar aVar = solver.makeSum(wlength[portcounter][i], woffset[portcounter][i]).var();
						solver.addConstraint(solver.makeGreaterOrEqual(wperiod[portcounter][i], aVar));
					}
		
					portcounter++;
				}
			}
		}
	}
	public void LinkConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {

	}
	public void FlowTransmissionConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {

	}
	public void EndtoEndConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {

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
	public int getSolutionNumber() {
		return TotalRuns;
	}
	
	public Solution cloneSolution() {
		return AssignSolution(Wperiod, Wlength, Woffset, Costs);
	}

	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);
		
		//return false;
    	
    	if(Costs[2].value() == 0) {
    		return true;
    	}
    	
		if((TotalRuns >= 500)){
			return true;
		}else {
			return false;

		}

	}
	private int AssignVars(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					wperiod[portcounter] = new IntVar[7];
					wlength[portcounter] = new IntVar[7];
					woffset[portcounter] = new IntVar[7];
					for (int i = 0; i < 7; i++) {
						wperiod[portcounter][i] = solver.makeIntVar(0, ((sw.Hyperperiod) / sw.microtick), ("F_" + portcounter + "_"+ i));
						wlength[portcounter][i] = solver.makeIntVar(0, ((sw.Hyperperiod) / sw.microtick), ("F_" + portcounter + "_"+ i));
						woffset[portcounter][i] = solver.makeIntVar(0, ((sw.Hyperperiod) / sw.microtick), ("F_" + portcounter + "_"+ i));
						Totalvars++;
					}
		
					portcounter++;
				}
			}
		}
		return Totalvars;
	}

	private int GetQueTransmitionDuration(Port port, int Q) {
		int Totalst = 0;
		for (Stream stream : port.AssignedStreams) {
			if (stream.Priority == Q) {
				Totalst += stream.Transmit_Time;
			}
		}
		return Totalst;
	}
	private int GetQuemaxTransmitionDuration(Port port, int Q) {
		int maxSt = 0;
		for (Stream stream : port.AssignedStreams) {
			if (stream.Priority == Q) {
				if (stream.Transmit_Time >= maxSt) {
					maxSt = stream.Transmit_Time;
				}
			}
		}
		return maxSt;
	}
	private int GetQuePeriodPercentage(Port port, int Q) {
		int PP = 0;
		for (Stream stream : port.AssignedStreams) {
			if (stream.Priority == Q) {
				PP += stream.Transmit_Time / stream.Period;
			}
		}
		return PP;
	}
	private int GetQueGuardBand(Port port, int Q) {
		return GetQuemaxTransmitionDuration(port, Q);
	}
	private Solution AssignSolution(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] costs)  {
		Current.costValues.clear();
		for (int i = 0; i < costs.length; i++) {
			Current.costValues.add(costs[i].value());
		}
		
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int gclcounter = 0;
					int Hyperperiod = GetPortHyperperiod(wperiod[portcounter]);
					for (int i = 0; i < 7; i++) {
						int W_period = (int) wperiod[portcounter][i].value() * sw.microtick;
						int N_instances = W_period / Hyperperiod;
						for (int j = 0; j < N_instances; j++) {
							
							port.Topen[gclcounter] = W_period * j + (int) woffset[portcounter][i].value() * sw.microtick;
							port.Tclose[gclcounter] = port.Topen[gclcounter] + (int) wlength[portcounter][i].value() * sw.microtick;
							port.affiliatedQue[gclcounter] = i;
							gclcounter++;
						}
					}
				}			
				portcounter++;
			}
		}
		return Current.Clone();
	}
	private int GetPortHyperperiod(IntVar[] portPeriod) {
		int hyperperiod = 1;
		for (int i = 0; i < 7; i++) {
			int tempperiod = (int) portPeriod[i].value();
			if (tempperiod != 0) {
				hyperperiod = LCM(hyperperiod, tempperiod);
			}
			
		}
		return hyperperiod;
	}
	private int ExternalCostTotalInterval(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		int TotalInterval = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					//int Hyperperiod = GetPortHyperperiod(wperiod[portcounter]);
					for (int i = 0; i < 7; i++) {
						int W_period = (int) wperiod[portcounter][i].value();
						int interval = (int) wlength[portcounter][i].value() / W_period;
						TotalInterval += interval;
					}
					//int PortCost = TotalInterval/Hyperperiod;
				}			
				portcounter++;
			}
		}
		return TotalInterval;
	}
	private int ExternalCostWCD(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		return 0;
	}
	private int LCM(int a, int b) {
		int lcm = (a > b) ? a : b;
        while(true)
        {
            if( lcm % a == 0 && lcm % b == 0 )
            {
                break;
            }
            ++lcm;
        }
		return lcm;
	}
}
