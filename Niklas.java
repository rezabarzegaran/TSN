package TSN;
import java.io.OutputStream;
import java.util.*;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.IntVarElement;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.SearchMonitor;
import com.google.ortools.constraintsolver.Solver;

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
	long LenScale = (long) 100000;
	ExternalAssessment excAssessment;

	
	
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
		Costs = new IntVar[3];
		TotalVars = AssignVars(Wperiod, Wlength, Woffset);
	}
	public void addConstraints() {
		WindowDurationConstraint(Wperiod, Wlength, Woffset);
		PortSamePeriodConstraint(Wperiod, Wlength, Woffset);
		WindowMaxPeriodConstriant(Wperiod, Wlength, Woffset);
		WindowPropertyConstraint(Wperiod, Wlength, Woffset);	
	}
	public void NoOverlappingWidnows(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NUsedQ = port.getUsedQ();
					for (int i = 0; i < NUsedQ; i++) {
						for (int j = 0; j < NUsedQ; j++) {	
							if (i != j) {
								IntVar iOpen = woffset[portcounter][i];
								IntVar iClose = solver.makeSum(iOpen, wlength[portcounter][i]).var();
								IntVar jOpen = woffset[portcounter][j];
								IntVar jClose = solver.makeSum(jOpen, wlength[portcounter][j]).var();
								IntVar FC = solver.makeIsGreaterOrEqualVar(iClose, jOpen).var();
								IntVar SC = solver.makeIsGreaterOrEqualVar(jClose, iOpen).var();
								
								IntVar DC = solver.makeSum(FC, SC).var();
								solver.addConstraint(solver.makeEquality(DC, 1));
							}
						}
					}
					portcounter++;
				}
			}
		}
	}
	public void PortSamePeriodConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					for (int i = 0; i < NusedQ; i++) {
						for (int j = 0; j < NusedQ; j++) {
							solver.addConstraint(solver.makeEquality(wperiod[portcounter][i], wperiod[portcounter][j]));
						}
						
					}
		
					portcounter++;
				}
			}
		}
	}
	public void WindowPropertyConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					for (int i = 0; i < NusedQ; i++) {
						IntVar aVar = solver.makeSum(wlength[portcounter][i], woffset[portcounter][i]).var();
						solver.addConstraint(solver.makeGreaterOrEqual(wperiod[portcounter][i], aVar));
					}
		
					portcounter++;
				}
			}
		}
	}
	public void WindowDurationConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							IntVar scaledLength= solver.makeProd(wlength[portcounter][UsedQCounter], sw.microtick).var();
							solver.addConstraint(solver.makeGreaterOrEqual(scaledLength , (GetQueTransmitionDuration(q) + GetQuemaxTransmitionDuration(q))));
							UsedQCounter++;
						}
					}
					portcounter++;
				}
			}
		}
	}
	public void WindowMaxPeriodConstriant(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							int percentage = GetQuePercentage(q);

							//IntVar scaledLength = solver.makeProd(wlength[portcounter][UsedQCounter], percentage).var();
							//int scaledGB = GetQuemaxTransmitionDuration(q) * percentage;
							//IntVar _period = solver.makeSum(wperiod[portcounter][UsedQCounter], scaledGB).var();			
							//solver.addConstraint(solver.makeGreaterOrEqual(scaledLength, _period));
							
							IntVar _length = solver.makeProd(wlength[portcounter][UsedQCounter], 100).var();
							int _Gb = GetQuemaxTransmitionDuration(q) * 100;
							IntVar _period = solver.makeProd(wperiod[portcounter][UsedQCounter], percentage).var();
							IntVar _periodGB = solver.makeSum(_period , _Gb).var();
							solver.addConstraint(solver.makeGreaterOrEqual(_length, _periodGB));
							
							
							
							UsedQCounter++;
						}
					}
					portcounter++;
				}
			}
		}
	}

	public void addCosts() {
		//Cost[0]
		MinimizeWindowPercentage(Wperiod, Wlength, Woffset, Costs);
		//Cost[1]
		//Costs[1].setValue(Integer.MAX_VALUE);
		//costVar = CostMinimizer(Wperiod, Wlength, Woffset, Costs);
		excAssessment = new ExternalAssessment(solver, Costs, Wperiod, Wlength, Woffset, Current);

	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		Flat2DArray(Wperiod, x);
		Flat2DArray(Wlength, y);
		Flat2DArray(Woffset, z);
		long allvariables = 3 * TotalVars;
		System.out.println("There are " + allvariables + "Variables");
		DecisionBuilder[] dbs = new DecisionBuilder[3];
		dbs[0] = solver.makePhase(x,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_MAX_VALUE); // The systematic search method
		dbs[1] = solver.makePhase(y,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_RANDOM_VALUE); // The systematic search method
		//dbs[2] = solver.makePhase(z,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_RANDOM_VALUE); // The systematic search method
		DecisionBuilder dbstemp = solver.makePhase(z,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_RANDOM_VALUE); // The systematic search method

		dbs[2] = solver.makeSolveOnce(dbstemp);
		db = solver.compose(dbs);
	}
	public void addSolverLimits() {
		int hours = 1;
		int minutes = 0;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		SearchMonitor[] searchVar = new SearchMonitor[2];
		searchVar[0] = limit;
		searchVar[1] = excAssessment;	
		//searchVar[2] = costVar;
		solver.newSearch(getDecision(),searchVar);
	    System.out.println(solver.model_name() + " Initiated");
	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public int getSolutionNumber() {
		return TotalRuns;
	}
	
	public Solution cloneSolution() {
		//Costs[0].setValue(excAssessment.getExternalCost1());
		Costs[1].setValue(excAssessment.getExternalCost2());
		Costs[2].setValue(excAssessment.getExternalCost3());
		return AssignSolution(Wperiod, Wlength, Woffset, Costs);
	}

	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);    	
		if((TotalRuns >= 5000)){
			return true;
		}else {
			return false;

		}

	}
	private int AssignVars(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		
		for (int i = 0; i < Costs.length; i++) {
			Costs[i] = solver.makeIntVar(0, Integer.MAX_VALUE);
		}
		
		int portcounter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					wperiod[portcounter] = new IntVar[NusedQ];
					wlength[portcounter] = new IntVar[NusedQ];
					woffset[portcounter] = new IntVar[NusedQ];
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							wperiod[portcounter][UsedQCounter] = solver.makeIntVar(0, ((port.getHPeriod()) / sw.microtick), ("F_" + portcounter + "_"+ UsedQCounter));
							wlength[portcounter][UsedQCounter] = solver.makeIntVar(0, ((port.getHPeriod()) / sw.microtick), ("F_" + portcounter + "_"+ UsedQCounter));
							woffset[portcounter][UsedQCounter] = solver.makeIntVar(0, ((port.getHPeriod()) / sw.microtick), ("F_" + portcounter + "_"+ UsedQCounter));
							Totalvars++;
							UsedQCounter++;
						}
					}

		
					portcounter++;
				}
			}
		}
		return Totalvars;
	}
	private OptimizeVar MinimizeWindowPercentage(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar percent = null;
		for (int i = 0; i < woffset.length; i++) {
			for (int j = 0; j < woffset[i].length; j++) {
				IntVar scaeldLength = solver.makeProd(wlength[i][j], LenScale).var();
				IntVar crr_per = solver.makeDiv(scaeldLength ,wperiod[i][j]).var();
				if(percent == null) {
					percent = crr_per;
				}else {
					percent = solver.makeSum(percent, crr_per).var();
				}
				
			}
			
		}
		cost[0] = percent;
		return solver.makeMinimize(percent, 1);
	}
	private int GetQueTransmitionDuration(Que q) {
		int Totalst = 0;
		for (Stream s : q.assignedStreams) {
			Totalst += s.Transmit_Time;
		}

		return Totalst;
	}
	private int GetQuemaxTransmitionDuration(Que q) {
		int maxSt = 0;
		for (Stream stream : q.assignedStreams) {
				if (stream.Transmit_Time >= maxSt) {
					maxSt = stream.Transmit_Time;
				}
		}
		return maxSt;
	}
	private Solution AssignSolution(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] costs)  {
		Current.costValues.clear();
		for (int i = 0; i < costs.length; i++) {
			long val = 0;
			if(costs[i] != null) {
				val = (int) costs[i].value();
			}
			Current.costValues.add(val);
		}
		
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int gclcounter = 0;
					int Hyperperiod = GetPortHyperperiod(wperiod[portcounter]);

					port.SetGCLs(GetPortGCLSize(wperiod[portcounter], Hyperperiod));
					int NUsedQ = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							int WW_period = (int) wperiod[portcounter][NUsedQ].value() * sw.microtick;
							int N_instances = Hyperperiod / WW_period;
							for (int j = 0; j < N_instances; j++) {
								port.Topen[gclcounter] = WW_period * j + (int) woffset[portcounter][NUsedQ].value() * sw.microtick;
								port.Tclose[gclcounter] = port.Topen[gclcounter] + (int) wlength[portcounter][NUsedQ].value() * sw.microtick;
								port.affiliatedQue[gclcounter] = q.Priority;
								port.setPeriod(Hyperperiod);
								gclcounter++;
							}
							NUsedQ++;
						}
					}

					portcounter++;
				}			
				
			}
		}
		return Current.Clone();
	}
	private int GetPortHyperperiod(IntVar[] portPeriod) {
		int hyperperiod = 1;
		for (int i = 0; i < portPeriod.length; i++) {
			int tempperiod = (int) portPeriod[i].value();
			if (tempperiod != 0) {
				hyperperiod = LCM(hyperperiod, tempperiod);
			}
			
		}
		return hyperperiod;
	}
	private int GetQuePercentage(Que q) {
		double per = 0;
		for (Stream s : q.assignedStreams) {
			per += ( s.Transmit_Time / s.Period ) ;	
		}
		
		int percentage =(int) Math.ceil(per * 100) ;
		
		
		return percentage;
	}
	private int GetPortGCLSize(IntVar[] portPeriod, int hyper) {
		int GCLSize = 0;
		for (int i = 0; i < portPeriod.length; i++) {
			int crr_P = (int) portPeriod[i].value();
			GCLSize += hyper / crr_P;
		}
		return GCLSize;
	}
	private void Flat2DArray(IntVar[][] source, IntVar[] destination) {
		int counter = 0;
		for (int i = 0; i < source.length; i++) {
			for (int j = 0; j < source[i].length; j++) {
				destination[counter] = source[i][j];
				counter++;
			}
		}
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
