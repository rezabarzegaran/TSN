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
import com.google.ortools.constraintsolver.FirstSolutionStrategy.Value;

public class HybridFraction extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	OptimizeVar OptVar;
	int NOutports;
	int TotalVars;
	IntVar[] Costs;
	OptimizeVar costVar;
	int TotalRuns = 0;
	IntVar[][] Wlength;
	IntVar[][] Woffset;
	long LenScale = (long) 10000;
	
	public HybridFraction(Solver _solver) {
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
		Wlength = new IntVar[NOutports][];
		Woffset = new IntVar[NOutports][];
		Costs = new IntVar[2];
		TotalVars = AssignVars(Wlength, Woffset);
	}
	public void addConstraints() {
		
		WindowPropertyConstraint(Wlength, Woffset);
		BandwidthConstraint(Wlength, Woffset);
		NoOverlappingWidnows(Wlength, Woffset);
		DelayConstraint(Wlength, Woffset);	
	}
	public void BandwidthConstraint(IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int qCounter=0;
					for (Que q : port.ques) {
						if (q.isUsed()) {
							IntVar GB = solver.makeIntConst(GetQuemaxSingleTransmitionDuration(q));
							IntVar aVar = solver.makeDifference(wlength[portcounter][qCounter], GB ).var();
							IntVar bVar = solver.makeMax(GB, aVar).var();
							IntVar cVar = solver.makeProd(bVar, 100).var();
							IntVar dVar = solver.makeDiv(cVar, q.getPeriod()).var();
							solver.addConstraint(solver.makeGreaterOrEqual(dVar,GetQuePercentage(q)));
							qCounter++;
						}
					}
					portcounter++;
				}
			}
		}
	}
		
	public IntVar getServiceCurve(Port port, Que q) {
		IntVar Value = null;
		int Instances = Current.Hyperperiod / q.getPeriod();
		
		IntVar GB = solver.makeIntConst(GetQuemaxSingleTransmitionDuration(q));
		
		
		IntVar UseableLengthCandid = solver.makeDifference(Wlength[PortObj2Num(port)][QueuObj2Num(q)], GB).var();

		IntVar UseableLength = solver.makeMax(GB, UseableLengthCandid).var();
		IntVar Len = solver.makeProd(UseableLength, Wlength[PortObj2Num(port)][QueuObj2Num(q)]).var();
		Len = solver.makeDiv(Len, 2).var();
		Len = solver.makeProd(Len, Instances).var();
		
		
		IntVar Len2 = solver.makeProd(UseableLength, q.getPeriod()).var();
		int repeat = (Instances * (Instances - 1))/2;
		Len2 = solver.makeProd(Len2, repeat).var();
		Value = solver.makeSum(Len, Len2).var();
		IntVar FreeDist = solver.makeDifference(q.getPeriod(), Woffset[PortObj2Num(port)][QueuObj2Num(q)]).var();
		FreeDist = solver.makeDifference(FreeDist, Wlength[PortObj2Num(port)][QueuObj2Num(q)]).var();
		IntVar Len3 = solver.makeProd(UseableLength, FreeDist).var();
		//IntVar Len3 = solver.makeProd(Wlength[PortObj2Num(port)][QueuObj2Num(q)], FreeDist).var();
		Len3 = solver.makeProd(Len3, Instances).var();
		Value = solver.makeSum(Value, Len3).var();
		return Value;
		
	}
	public long getArrivalCurve(Switches SW, Port port, Que q) {
		
		return getStreamCurve(SW, port, q);
	}

	
	
	public long getStreamCurve(Switches SW,  Port port, Que q) {
		long Value = 0;
		for (Stream s : q.assignedStreams) {
			Value += ((s.N_instances * (s.N_instances + 1 ))/2)*(s.Transmit_Time * s.Period);

			if(s.isThisFirstSwtich(SW.Name)) {

			}else {
				int currentSWNumber = s.routingList.indexOf(SW.Name);
				Value += ((s.N_instances * (s.N_instances - 1 ))/2)*(s.Transmit_Time  * s.Period);
			}

		}
		return Value;
	}
	
	
	public int PortObj2Num(Port port) {
		int itterport = 0;
		for (Switches itterSW : Current.SW) {
			for (Port itterP : itterSW.ports) {
				if (itterP.outPort) {
					if(itterP.equals(port)) {
							return itterport;	
					}
					itterport++;	
				}
			}
		}
		return -1;
	}
	public int QueuObj2Num(Que q) {
		for (Switches itterSW : Current.SW) {
			for (Port itterP : itterSW.ports) {
				if (itterP.outPort) {
					int Useditterq = 0;
					for ( Que itterq : itterP.ques) {
						if(itterq.isUsed()) {
							if(itterq.equals(q)) {
								return Useditterq;
							}
							Useditterq++;
						}
					}
				}
			}
		}
		return -1;
	}
	public List<Port> getPreviousPorts(Switches sw, Port p, Que q){
		List<Port> ports = new ArrayList<Port>();
		for (Stream s : q.assignedStreams) {
			if(!s.isThisFirstSwtich(sw.Name)) {
				String previousSwitchName = s.getpreviousSwitch(sw.Name);
				Switches Pre_SW = Current.getSwithObject(previousSwitchName);
				if(Pre_SW != null) {
					for (Port port : Pre_SW.ports) {
						if(port.HasStream(s)) {
							if(!ports.contains(port)) {
								ports.add(port);
							}
						}
					}
				}
			}
		}
		return ports;
	}
	public Que getQueofPort(Port p, int priority) {
		for (Que q : p.ques) {
			if(q.Priority == priority) {
				return q;
			}
		}
		return null;
	}
	public int GetStreamsNeededDurationInterval(Switches sw, Que q, int time_0, int time_1) {
		int PureServiceValue = 0;
		if(time_0 == -1) {
			PureServiceValue = GetStreamsNeededDuration(sw, q, time_1);
		}else {
			PureServiceValue = GetStreamsNeededDuration(sw, q, time_1) - GetStreamsNeededDuration(sw, q, time_0);
		}
		
		PureServiceValue = PureServiceValue * PureServiceValue;
		return PureServiceValue;
	}
	public int GetStreamsNeededDuration(Switches sw, Que q, int time) {
		int duration = 0;
		for (Stream s : q.assignedStreams) {
			int instance = (time / s.Period) + 1;
			duration += instance * s.Transmit_Time;
			if(!s.isThisFirstSwtich(sw.Name)) {
				//duration += instance * 1 * s.Transmit_Time;
			}
			//for (int i = 0; i < s.N_instances; i++) {
				//int deadline = s.Deadline + (i * s.Period);
				//int release = (i * s.Period) + s.Transmit_Time;
				//if(s.isThisFirstSwtich(sw.Name)) {
					//if(release <= time) {
						//duration += s.Transmit_Time;
					//}
				//}else {
					//if(deadline <= time) {
						//duration += s.Transmit_Time;
					//}else if((release+s.Transmit_Time) >= time){
						
					//}
				//}
				

			//}


		}
		return duration;
	}
	public List<Integer> GetAllDeadlines(Que q){
		List<Integer> Events = new ArrayList<Integer>();
		for (Stream s : q.assignedStreams) {
			for (int i = 0; i < s.N_instances; i++) {
				int deadline = s.Deadline + (i * s.Period);
				if(!Events.contains(deadline)) {
					Events.add(deadline);
					
				}
			}
		}
		Collections.sort(Events);
		return Events;
	}
	public int getEarliestDeadline(Que q) {
		int earlydeadline = Integer.MAX_VALUE;
		Stream S = null;
		for (Stream s : q.assignedStreams) {
			//int D =  (int) (s.Deadline * (1 - (s.routingList.size()-1) * 0.2 ));
			int D = s.Deadline /s.routingList.size();
			//D = (int) (3 * D) ;
			if(D < earlydeadline) {
				earlydeadline = D;
				S = s;
			}
		}
		//int D =  (int) (S.Deadline * (1 - (S.routingList.size()-1) * 0.1 ));
		int D =  (int) (S.Deadline  * 0.5 );

		return D;
	}
	
	public void DelayConstraint(IntVar[][] wlength, IntVar[][] woffset) {
		
		int portcounter = 0;
		for (var sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort){
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							IntVar WindowServiceValue = getServiceCurve(port, q);
							WindowServiceValue = solver.makeProd(WindowServiceValue, 10).var();
							long TotalServiceNeeded = getArrivalCurve(sw, port, q) * 10;
							solver.addConstraint(solver.makeGreaterOrEqual(WindowServiceValue, TotalServiceNeeded));
							
							
							UsedQCounter++;
							}
								
						}
					portcounter++;
					}
					
				}
			}

		
	}
	
	
	public int getPortNumber(String swName, Stream s) {
		int itterport = 0;
		for (Switches itterSW : Current.SW) {
			for (Port itterP : itterSW.ports) {
				if (itterP.outPort) {
					if(itterSW.Name.equals(swName)&& itterP.HasStream(s)) {
							return itterport;	
					}
					itterport++;	
				}
			}
		}
		return -1;
	}
	public int getQueueNumber(String swName, Stream s) {
		int itterport = 0;
		for (Switches itterSW : Current.SW) {
			for (Port itterP : itterSW.ports) {
				if (itterP.outPort) {
					int Useditterq = 0;
					for ( Que itterq : itterP.ques) {
						if(itterq.isUsed()) {
							if(itterSW.Name.equals(swName)&& itterq.HasStream(s)) {
								return Useditterq;
							}
							Useditterq++;
						}
					}
					itterport++;	
				}
			}
		}
		return -1;
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
	public void NoOverlappingWidnows(IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					
					int UsedQCounteri = 0;
					for (Que qi : port.ques) {
						if(qi.isUsed()) {
							int UsedQCounterj = 0;
							for (Que qj : port.ques) {
								if(qj.isUsed()) {
									
									if (UsedQCounteri != UsedQCounterj) {
										int H_i_j = LCM(qi.getPeriod(), qj.getPeriod());
										
										for (int ki = 0; ki < (H_i_j / qi.getPeriod()); ki++) {
											IntVar iOpen = woffset[portcounter][UsedQCounteri];
											iOpen = solver.makeSum(iOpen, (ki*qi.getPeriod())).var();
											IntVar iClose = solver.makeSum(iOpen, wlength[portcounter][UsedQCounteri]).var();

											for (int kj = 0; kj < (H_i_j / qj.getPeriod()); kj++) {
												IntVar jOpen = woffset[portcounter][UsedQCounterj];
												jOpen = solver.makeSum(jOpen, (kj*qj.getPeriod())).var();
												IntVar jClose = solver.makeSum(jOpen, wlength[portcounter][UsedQCounterj]).var();
												IntVar FC = solver.makeIsGreaterVar(iClose, jOpen).var();
												IntVar SC = solver.makeIsGreaterVar(jClose, iOpen).var();
												IntVar DC = solver.makeSum(FC, SC).var();
												solver.addConstraint(solver.makeEquality(DC, 1));

											}
										}
										
										
										
									}
									

									UsedQCounterj++;
								}
							}
							

							UsedQCounteri++;
						}
					}
					
					portcounter++;
				}
			}
		}
	}
	public void WindowPropertyConstraint(IntVar[][] wlength, IntVar[][] woffset) {
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							IntVar aVar = solver.makeSum(wlength[portcounter][UsedQCounter], woffset[portcounter][UsedQCounter]).var();
							solver.addConstraint(solver.makeLessOrEqual(aVar, q.getPeriod()));
							UsedQCounter++;
						}
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
							//IntVar hyperIntVar = solver.makeIntConst(Current.Hyperperiod);
							//IntVar PInstances = solver.makeDiv(hyperIntVar, wperiod[portcounter][UsedQCounter]).var();
							IntVar scaledLength= solver.makeProd(wlength[portcounter][UsedQCounter], sw.microtick).var();
							solver.addConstraint(solver.makeGreaterOrEqual(scaledLength , (GetQueBacklog(sw, q)+ GetQuemaxSingleTransmitionDuration(q))));
							//IntVar scaledLengthtoHP= solver.makeProd(scaledLength, PInstances).var();
							//solver.addConstraint(solver.makeGreaterOrEqual(scaledLengthtoHP , (GetQueTransmitionDuration(q) + GetQuemaxTransmitionDuration(q))));
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
		MinimizeWindowPercentage(Wlength, Woffset, Costs);
		costVar = CostMinimizer(Wlength, Woffset, Costs);

	}
	public void addDecision() {
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		Flat2DArray(Wlength, y);
		Flat2DArray(Woffset, z);
		long allvariables = 2 * TotalVars;
		IntVar[] allvars = new IntVar[(int) allvariables];
		//MergeArray(x, y, z, allvars);
		MergeArray(y, z, allvars);
		System.out.println("There are " + allvariables + "Variables");
		DecisionBuilder[] dbs = new DecisionBuilder[2];
		dbs[0] = solver.makePhase(y,  solver.CHOOSE_RANDOM, solver.ASSIGN_RANDOM_VALUE); // The systematic search method
		dbs[1] = solver.makePhase(z,  solver.CHOOSE_RANDOM, solver.ASSIGN_RANDOM_VALUE); // The systematic search method
		db = solver.compose(dbs);
	}
	public void addSolverLimits() {
		int hours = 0;
		int minutes = 1;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		SearchMonitor[] searchVar = new SearchMonitor[2];
		searchVar[0] = limit;
		searchVar[1] = costVar;
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
		return AssignSolution(Wlength, Woffset, Costs);
	}

	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);    	
		if(TotalRuns >= 1){
			return true;
		}else {
			return false;

		}

	}
	private int AssignVars(IntVar[][] wlength, IntVar[][] woffset) {
		
		for (int i = 0; i < Costs.length; i++) {
			Costs[i] = solver.makeIntVar(0, Integer.MAX_VALUE);
		}
		
		int portcounter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					wlength[portcounter] = new IntVar[NusedQ];
					woffset[portcounter] = new IntVar[NusedQ];
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							wlength[portcounter][UsedQCounter] = solver.makeIntVar((GetQuemaxSingleTransmitionDuration(q)/ sw.microtick), ((port.getPeriod()) / sw.microtick), ("L_" + portcounter + "_"+ UsedQCounter));
							woffset[portcounter][UsedQCounter] = solver.makeIntVar(0, (((port.getPeriod()) / sw.microtick) - (GetQuemaxSingleTransmitionDuration(q)/ sw.microtick)), ("O_" + portcounter + "_"+ UsedQCounter));
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
	private OptimizeVar CostMinimizer(IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar Cost1 = solver.makeProd(cost[0], 1).var();
		IntVar totalCost = Cost1;
		cost[1] = totalCost;
		//solver.addConstraint(solver.makeLessOrEqual(cost[0], 835));

		return solver.makeMinimize(totalCost, 2);
	}
	private OptimizeVar MinimizeWindowPercentage(IntVar[][] wlength, IntVar[][] woffset, IntVar[] cost) {
		IntVar percent = null;
		
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							IntVar scaeldLength = solver.makeProd(wlength[portcounter][UsedQCounter], LenScale).var();
							int scaledPeriod = q.getPeriod() * TotalVars;
							IntVar crr_per = solver.makeDiv(scaeldLength ,scaledPeriod).var();
							if(percent == null) {
								percent = crr_per;
							}else {
								percent = solver.makeSum(percent, crr_per).var();
							}
							UsedQCounter++;
						}
					}

					portcounter++;
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
		Stream minS = null;
		for (Stream stream : q.assignedStreams) {
			int D =  stream.Deadline / stream.routingList.size();
				if (D <= minD) {
					minD = D;
					minS = stream;
				}
		}
		int D =  (int) (minS.Deadline * (1 - (minS.routingList.size()-1) * 0.25 ));
		return D;
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
	private int GetQueBacklog(Switches sw, Que q) {
		int total = 0;
		for (Stream stream : q.assignedStreams) {
			if(!stream.isThisFirstSwtich(sw.Name)){
				total += stream.Transmit_Time;
			}

		}
		return total;
	}
	private int GetQuemaxSingleTransmitionSize(Que q) {
		int maxSt = 0;
		Stream best = null;
		for (Stream stream : q.assignedStreams) {
				if (stream.Size >= maxSt) {
					maxSt = stream.Size;
					best = stream;
				}
		}
		return (int) (1 * maxSt);
	}
	private Solution AssignSolution(IntVar[][] wlength, IntVar[][] woffset, IntVar[] costs)  {
		Current.costValues.clear();
		Current.Variables = TotalVars;
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
					int Hyperperiod = port.getPeriod();
					int NUsedQ = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							int WW_period = q.getPeriod();
							int N_instances = Hyperperiod / WW_period;
							for (int j = 0; j < N_instances; j++) {
								port.Topen[gclcounter] = WW_period * j + (int) woffset[portcounter][NUsedQ].value() * sw.microtick;
								port.Tclose[gclcounter] = port.Topen[gclcounter] + (int) wlength[portcounter][NUsedQ].value() * sw.microtick;
								port.affiliatedQue[gclcounter] = q.Priority;
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
	private int GetQuePercentage(Que q) {
		double per = 0;
		for (Stream s : q.assignedStreams) {
			per += (( (double) (s.Transmit_Time)) / s.Period ) ;	
			
		}
		
		int percentage =(int) Math.ceil(per * 100) ;

		
		return percentage;
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
	private void MergeArray(IntVar[] source1, IntVar[] source2, IntVar[] destination) {
		int counter = 0;
		for (int i = 0; i < source1.length; i++) {
			destination[counter] = source1[i];
			counter++;
		}
		for (int i = 0; i < source2.length; i++) {
			destination[counter] = source2[i];
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
