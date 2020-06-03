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
	long LenScale = (long) 10000;
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
		//WCDelayConstraint(Wperiod, Wlength, Woffset);
		WindowDurationConstraint(Wperiod, Wlength, Woffset);
		FixedPeriodConstraint(Wperiod, Wlength, Woffset);
		PortSamePeriodConstraint(Wperiod, Wlength, Woffset);
		
		WindowMaxPeriodConstriant(Wperiod, Wlength, Woffset);
		WindowPropertyConstraint(Wperiod, Wlength, Woffset);
		FrameHandleCosntraint(Wperiod, Wlength, Woffset);
		//LinkHandleConstraint(Wperiod, Wlength, Woffset);
		NoOverlappingWidnows(Wperiod, Wlength, Woffset);
		WCDelayConstraint(Wperiod, Wlength, Woffset);
		
	}
	public void LinkHandleConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		for (Stream s : Current.streams) {
			IntVar totalOffset = null;
			for (String node : s.routingList) {
				int portcounter = 0;
				for (Switches sw : Current.SW) {
					for (Port port : sw.ports) {
						if(port.outPort) {
							if (sw.Name.equals(node)) {
								int UsedQCounter = 0;
								for (Que q : port.ques) {
									if(q.isUsed()) {
										if(q.HasStream(s)) {
											if(totalOffset == null) {
												totalOffset = woffset[portcounter][UsedQCounter];
											}else {
												totalOffset = solver.makeSum(totalOffset, woffset[portcounter][UsedQCounter]).var();
											}
										}
										UsedQCounter++;
									}
								}
							}
							portcounter++;
						}
					}
				}
			}
			solver.addConstraint(solver.makeLess(totalOffset, s.Deadline));
			
		}
	}
	public void WCDelayConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		IntVar[][] streamInstance = new IntVar[Current.streams.size()][];
		int streamcounter = 0;
		for (Stream s : Current.streams) {
			streamInstance[streamcounter] = new IntVar[s.N_instances];
			IntVar s_total_percent = null;
			
			for (int i = 0; i < s.N_instances; i++) {
				long[] stream_release = new long[1];
				stream_release[0] = i * s.Period;
				IntVar s_r = solver.makeIntVar(stream_release);
				IntVar previousRelease = null;
				//IntVar s_r = solver.makeIntConst(stream_release);
				for (String node : s.routingList) {
					int portcounter = 0;
					for (Switches sw : Current.SW) {
						for (Port port : sw.ports) {
							if(port.outPort) {
								if (sw.Name.equals(node) && port.HasStream(s)) {
									int UsedQCounter = 0;
									for (Que q : port.ques) {
										if(q.isUsed()) {
											if(q.HasStream(s)) {
												IntVar Winstance = solver.makeDiv(s_r.var(), wperiod[portcounter][UsedQCounter].var()).var();												
												IntVar Wopen = solver.makeSum(woffset[portcounter][UsedQCounter].var() , solver.makeProd(Winstance, wperiod[portcounter][UsedQCounter])).var();
												IntVar Wclose = solver.makeSum(woffset[portcounter][UsedQCounter].var() , wlength[portcounter][UsedQCounter]).var();
												Wclose = solver.makeSum(Wclose, solver.makeProd(Winstance, wperiod[portcounter][UsedQCounter]).var()).var();
												

												
												
												
												if(previousRelease != null) {
												    solver.addConstraint(solver.makeGreaterOrEqual(Wopen.var(), previousRelease.var()));
													IntVar Dec1 = solver.makeIsGreaterVar(Wopen, previousRelease.var()).var();
													IntVar NDec1 = solver.makeIsLessOrEqualVar(Wopen, previousRelease.var()).var();
													IntVar latestRelease = solver.makeSum(solver.makeProd(Dec1, Wopen), solver.makeProd(NDec1, previousRelease.var())).var();
													IntVar relativeOpen = solver.makeSum(latestRelease , (GetQueSingleTransmitionDuration(q) + GetQuemaxSingleTransmitionDuration(q))).var();
													IntVar Dec2 = solver.makeIsGreaterVar(relativeOpen.var(), Wclose.var()).var();
													IntVar NDec2 = solver.makeIsLessOrEqualVar(relativeOpen.var(), Wclose.var()).var();
													
													IntVar _relativeOpen = solver.makeSum(Wopen.var(), GetQueSingleTransmitionDuration(q)+ GetQuemaxSingleTransmitionDuration(q)).var();
													_relativeOpen = solver.makeSum(_relativeOpen.var(), wperiod[portcounter][UsedQCounter].var()).var();
													
													relativeOpen = solver.makeSum(solver.makeProd(Dec2.var(), _relativeOpen.var()).var() , solver.makeProd(NDec2.var(), relativeOpen.var()).var()).var();
													
													s_r = relativeOpen.var();
													previousRelease = relativeOpen.var();

												    
												}else {
													IntVar relativeOpen = solver.makeSum(Wopen.var(), ( 2 * GetQueSingleTransmitionDuration(q)+ GetQuemaxSingleTransmitionDuration(q))).var();
													//IntVar relativeOpen = solver.makeDifference(Wclose.var(), solver.makeIntConst(GetQueSingleTransmitionDuration(q) + GetQuemaxSingleTransmitionDuration(q))).var();
													IntVar Dec2 = solver.makeIsGreaterVar(relativeOpen.var(), Wclose.var()).var();
													IntVar NDec2 = solver.makeIsLessOrEqualVar(relativeOpen.var(), Wclose.var()).var();
													relativeOpen = solver.makeSum(relativeOpen.var(), solver.makeProd(Dec2.var(), wperiod[portcounter][UsedQCounter].var())).var();
													s_r = relativeOpen.var();
													previousRelease = relativeOpen.var();
												}
												
												
												

												
												
												

												
												
												streamInstance[streamcounter][i] = previousRelease.var();
												
												//IntVar Dec1 = solver.makeIsGreaterVar(s_r, Wopen).var();
												//IntVar Dec1_1 = solver.makeIsLessVar(s_r, Wopen).var();
												//IntVar release = solver.makeSum(solver.makeProd(Dec1, s_r), solver.makeProd(Dec1_1, Wopen)).var();
												//release = solver.makeSum(release, solver.makeIntConst(GetQuemaxTransmitionDuration(q)+GetQueTransmitionDuration(q))).var();
												//IntVar Dec2 = solver.makeIsGreaterVar(release, Wclose).var();
												//IntVar Dec2_1 = solver.makeIsLessVar(release, Wclose).var();
												
												
												//IntVar newrelease = solver.makeSum(Wopen, solver.makeIntConst(GetQuemaxTransmitionDuration(q)+GetQueTransmitionDuration(q))).var();
												//newrelease = solver.makeSum(newrelease, solver.makeProd(Dec2, wperiod[portcounter][UsedQCounter])).var();
												//release = solver.makeSum(solver.makeProd(Dec2, newrelease), solver.makeProd(Dec2_1, release)).var();
												
												
												//solver.addConstraint(solver.makeGreaterOrEqual(wOpen, relativeOpen));
												//relativeOpen = solver.makeSum(woffset[portcounter][UsedQCounter] , wlength[portcounter][UsedQCounter]).var();
												//relativeOpen = solver.makeSum(relativeOpen, solver.makeProd(Winstance, wperiod[portcounter][UsedQCounter])).var();
												//IntVar windowhalf = solver.makeSum(woffset[portcounter][UsedQCounter], solver.makeDiv(wlength[portcounter][UsedQCounter], 2)).var();
												//IntVar Dec = solver.makeIsGreaterVar(s_r, relativeOpen).var();
												//Wclose = solver.makeSum(Wclose, solver.makeProd(Dec, wperiod[portcounter][UsedQCounter])).var();			
												//s_r = release;

												
												
											}
											UsedQCounter++;
										}
									}
								}
								portcounter++;
							}
						}
					}
				}
				streamInstance[streamcounter][i] = solver.makeDifference(streamInstance[streamcounter][i], solver.makeIntVar(stream_release)).var();
				solver.addConstraint(solver.makeLessOrEqual(streamInstance[streamcounter][i], ((int) ( 1.0 * s.Deadline))));
				int scaled_deadline = (s.Deadline * s.N_instances) / 10000;
				IntVar s_r_percent = solver.makeDiv(streamInstance[streamcounter][i], scaled_deadline).var();
				if(s_total_percent == null) {
					s_total_percent = s_r_percent;
				}else {
					s_total_percent = solver.makeSum(s_total_percent, s_r_percent).var();
				}
			}
			
			//solver.addConstraint(solver.makeLessOrEqual(s_total_percent, (long) 4000 ));
			streamcounter++;
						
		}
	}

	public void FrameHandleCosntraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							int mindeadline = minStreamDeadLine(q);
							IntVar scaledOffset= solver.makeProd(woffset[portcounter][UsedQCounter], sw.microtick).var();
							solver.addConstraint(solver.makeLess(scaledOffset, mindeadline));
							IntVar scaledLength= solver.makeProd(wlength[portcounter][UsedQCounter], sw.microtick).var();
							IntVar scaledPeriod= solver.makeProd(wperiod[portcounter][UsedQCounter], sw.microtick).var();
							//IntVar WClose = solver.makeSum(scaledLength , scaledOffset).var();
							IntVar WFree = solver.makeDifference(scaledPeriod, scaledLength).var();
							solver.addConstraint(solver.makeLess(WFree, mindeadline));							
							UsedQCounter++;
						}
					}
					portcounter++;
				}
			}
		}
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
								IntVar FC = solver.makeIsGreaterVar(iClose, jOpen).var();
								IntVar SC = solver.makeIsGreaterVar(jClose, iOpen).var();
								
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
	public void FixedPeriodConstraint(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					for (int i = 0; i < NusedQ; i++) {
							IntVar hyperIntVar = solver.makeIntConst(Current.Hyperperiod);
							IntVar PInstances = solver.makeDiv(hyperIntVar, wperiod[portcounter][i]).var();
							IntVar ScaledPeriod = solver.makeProd(wperiod[portcounter][i], PInstances).var();
							solver.addConstraint(solver.makeEquality(hyperIntVar, ScaledPeriod));	
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
							IntVar hyperIntVar = solver.makeIntConst(Current.Hyperperiod);
							IntVar PInstances = solver.makeDiv(hyperIntVar, wperiod[portcounter][UsedQCounter]).var();
							IntVar scaledLength= solver.makeProd(wlength[portcounter][UsedQCounter], sw.microtick).var();
							solver.addConstraint(solver.makeGreaterOrEqual(scaledLength , (GetQueSingleTransmitionDuration(q) + GetQuemaxSingleTransmitionDuration(q))));
							IntVar scaledLengthtoHP= solver.makeProd(scaledLength, PInstances).var();
							solver.addConstraint(solver.makeGreaterOrEqual(scaledLengthtoHP , (GetQueTransmitionDuration(q) + GetQuemaxTransmitionDuration(q))));
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
		MinimizeWCD(Wperiod, Wlength, Woffset, Costs);
		//Costs[1].setValue(Integer.MAX_VALUE);
		costVar = CostMinimizer(Wperiod, Wlength, Woffset, Costs);
		//excAssessment = new ExternalAssessment(solver, Costs, Wperiod, Wlength, Woffset, Current);

	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		Flat2DArray(Wperiod, x);
		Flat2DArray(Wlength, y);
		Flat2DArray(Woffset, z);
		long allvariables = 3 * TotalVars;
		//IntVar[] allvars = new IntVar[(int) allvariables];
		//MergeArray(x, y, z, allvars);
		
		System.out.println("There are " + allvariables + "Variables");
		DecisionBuilder[] dbs = new DecisionBuilder[3];
		dbs[0] = solver.makePhase(x,  solver.CHOOSE_RANDOM, solver.ASSIGN_CENTER_VALUE); // The systematic search method
		//DecisionBuilder dbstemp2 = solver.makePhase(x,  solver.CHOOSE_RANDOM, solver.ASSIGN_MAX_VALUE); // The systematic search method
		dbs[1] = solver.makePhase(y,  solver.CHOOSE_RANDOM, solver.ASSIGN_CENTER_VALUE); // The systematic search method
		//dbs[2] = solver.makePhase(z,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_MIN_VALUE); // The systematic search method
		DecisionBuilder dbstemp = solver.makePhase(z,  solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_MIN_VALUE); // The systematic search method
		//dbs[0]  = solver.makePhase(allvars,  solver.CHOOSE_RANDOM, solver.ASSIGN_RANDOM_VALUE);
		dbs[2] = solver.makeSolveOnce(dbstemp);
		db = solver.compose(dbs);
	}
	public void addSolverLimits() {
		int hours = 2;
		int minutes = 30;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		SearchMonitor[] searchVar = new SearchMonitor[3];
		searchVar[0] = limit;
		searchVar[1] = excAssessment;	
		searchVar[2] = costVar;
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
		//Costs[3].setValue(excAssessment.getExternalCost2());
		//Costs[4].setValue(excAssessment.getExternalCost3());
		return AssignSolution(Wperiod, Wlength, Woffset, Costs);
	}

	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);    	
		if(TotalRuns >= 200){
			return true;
			//return false;
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
							wperiod[portcounter][UsedQCounter] = solver.makeIntVar(0 , ((port.getHPeriod()) / sw.microtick), ("F_" + portcounter + "_"+ UsedQCounter));
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
	private OptimizeVar CostMinimizer(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar Cost1 = solver.makeProd(cost[0], 1).var();
		IntVar Cost2 = solver.makeProd(cost[1], 0).var();
		IntVar totalCost = solver.makeSum(Cost1, Cost2).var();
		//IntVar totalCost = Cost1;
		cost[2] = totalCost;
		//solver.addConstraint(solver.makeLessOrEqual(cost[2], 6500));
		//solver.addConstraint(solver.makeLessOrEqual(cost[1], 3400));
		//solver.addConstraint(solver.makeLessOrEqual(cost[0], 6500));
		//solver.addConstraint(solver.makeGreaterOrEqual(cost[1], 3000));

		return solver.makeMinimize(totalCost, 10);
	}
	private OptimizeVar MinimizeWindowPercentage(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar percent = null;
		for (int i = 0; i < woffset.length; i++) {
			for (int j = 0; j < woffset[i].length; j++) {
				IntVar scaeldLength = solver.makeProd(wlength[i][j], LenScale).var();
				IntVar scaledPeriod = solver.makeProd(wperiod[i][j], TotalVars).var();
				IntVar crr_per = solver.makeDiv(scaeldLength ,scaledPeriod).var();
				if(percent == null) {
					percent = crr_per;
				}else {
					percent = solver.makeSum(percent, crr_per).var();
				}
				
			}
			
		}
		cost[0] = percent;
		return solver.makeMinimize(percent, 6);
	}
	public OptimizeVar MinimizeWCD(IntVar[][] wperiod, IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar TotalDelayCost = null;
		int logC = 10000;
		for (Stream s : Current.streams) {
			IntVar TotalD = null;
			for (int i = 0; i < s.N_instances; i++) {	
				int stream_release = i * s.Period;
				IntVar s_r = solver.makeIntConst(stream_release);
				for (String node : s.routingList) {
					int portcounter = 0;
					for (Switches sw : Current.SW) {
						for (Port port : sw.ports) {
							if(port.outPort) {
								if (sw.Name.equals(node) && port.HasStream(s)) {
									int UsedQCounter = 0;
									for (Que q : port.ques) {
										if(q.isUsed()) {
											if(q.HasStream(s)) {
												IntVar Winstance = solver.makeDiv(s_r, wperiod[portcounter][UsedQCounter]).var();
												
												IntVar Wclose = solver.makeSum(woffset[portcounter][UsedQCounter] , wlength[portcounter][UsedQCounter]).var();
												Wclose = solver.makeSum(Wclose, solver.makeProd(Winstance, wperiod[portcounter][UsedQCounter])).var();			

												IntVar Dec = solver.makeIsGreaterOrEqualVar(s_r, Wclose).var();
												Wclose = solver.makeSum(Wclose, solver.makeProd(Dec, wperiod[portcounter][UsedQCounter])).var();			
												s_r = Wclose;
												
											}
											UsedQCounter++;
										}
									}
								}
								portcounter++;
							}
						}
					}
				}
				//s_r = solver.makeProd(s_r, 10000).var();
				int scaled_deadline = (s.Deadline * s.N_instances) / 10000;
				IntVar s_r_now = solver.makeDifference(s_r, solver.makeIntConst(stream_release)).var();
				s_r_now = solver.makeProd(s_r_now, 1).var();
				IntVar s_percent = solver.makeDiv(s_r_now, scaled_deadline).var();
				//s_percent = solver.makeProd(s_percent, 10).var();
				if(TotalD == null) {
					TotalD = s_percent;
				}else {
					TotalD = solver.makeSum(TotalD, s_percent).var();
				}
				
			}
			
			TotalD = solver.makeDiv(TotalD, Current.streams.size()).var();
			//TotalD = solver.makeProd(TotalD, logC).var();
			//IntVar sDelayPercent = solver.makeDiv(TotalD, (s.N_instances)).var();
			//TotalD = solver.makeDiv(TotalD, 1000).var();
			//TotalD = solver.makeSquare(TotalD).var();
			
			//TotalD = solver.makeDiv(TotalD, 10).var();
			IntVar weightedTotalDIntVar = solver.makeDiv(TotalD, 1000).var();
			weightedTotalDIntVar = solver.makeSum(weightedTotalDIntVar, 1).var();
			//IntVar sDelayPercent = weightedTotalDIntVar;
			IntVar sDelayPercent = solver.makeProd(TotalD, weightedTotalDIntVar).var();
			//IntVar sDelayPercent = TotalD;
			//sDelayPercent = solver.makeDiv(sDelayPercent, (s.Deadline)).var();
			if(TotalDelayCost == null) {
				TotalDelayCost = sDelayPercent;
			}else {
				TotalDelayCost = solver.makeSum(TotalDelayCost, sDelayPercent).var();
			}
		}
		//TotalDelayCost = solver.makeDiv(TotalDelayCost, Current.streams.size()).var();
		cost[1] = TotalDelayCost;
		return solver.makeMinimize(TotalDelayCost, 4);
	}
	private int GetQueTransmitionDuration(Que q) {
		int Totalst = 0;
		for (Stream s : q.assignedStreams) {
			Totalst += (s.Transmit_Time * s.N_instances);
		}

		return Totalst;
	}
	private int GetQueSingleTransmitionDuration(Que q) {
		int Totalst = 0;
		for (Stream s : q.assignedStreams) {
			Totalst += (1 * s.Transmit_Time);
		}

		return Totalst;
	}
	private int minStreamDeadLine(Que q) {
		int minD = Integer.MAX_VALUE;
		for (Stream stream : q.assignedStreams) {
				if (stream.Deadline <= minD) {
					minD = stream.Deadline;
				}
		}
		return minD;
	}
	private int GetQuemaxTransmitionDuration(Que q) {
		int maxSt = 0;
		Stream best = null;
		for (Stream stream : q.assignedStreams) {
				if (stream.Transmit_Time >= maxSt) {
					maxSt = stream.Transmit_Time;
					best = stream;
				}
		}
		return (maxSt * best.N_instances);
	}
	private int GetQuemaxSingleTransmitionDuration(Que q) {
		int maxSt = 0;
		Stream best = null;
		for (Stream stream : q.assignedStreams) {
				if (stream.Transmit_Time >= maxSt) {
					maxSt = stream.Transmit_Time;
					best = stream;
				}
		}
		return (int) (1 * maxSt);
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
			per += (( (double) (s.Transmit_Time)) / s.Period ) ;	
			
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
	private void MergeArray(IntVar[] source1, IntVar[] source2, IntVar[] source3, IntVar[] destination) {
		int counter = 0;
		for (int i = 0; i < source1.length; i++) {
			destination[counter] = source1[i];
			counter++;
		}
		for (int i = 0; i < source2.length; i++) {
			destination[counter] = source2[i];
			counter++;
		}
		for (int i = 0; i < source3.length; i++) {
			destination[counter] = source3[i];
			counter++;
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
