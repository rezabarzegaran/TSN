package TSN;

import java.util.Optional;
import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Silviu extends SolutionMethod{
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Silviu(Solver _solver) {
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
		NOutports = Current.getNOutPorts();
		Offset = new IntVar[NOutports][][];
		Costs = new IntVar[5];
		TotalVars = AssignVars(Offset);
	}
	public void addConstraints() {
		Constraint0(Offset);
		Constraint1(Offset);
		Constraint2(Offset);
		Constraint3(Offset);
	}
	public void addCosts() {
		Cost0(Offset, Costs);
		Cost1(Offset, Costs);
		Cost2(Offset, Costs);
		Cost3(Offset, Costs);
		OptVar = CostMinimizer(Costs);
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		FlatArray(Offset, x, NOutports);
		long allvariables = TotalVars;
		System.out.println("There are " + allvariables + "Variables");
		db = solver.makePhase(x,  solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
		//db = solver.makePhase(x,  solver.CHOOSE_RANDOM, solver.ASSIGN_RANDOM_VALUE);

	}
	public void addSolverLimits() {
		int hours = 10;
		int minutes = 20;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		solver.newSearch(getDecision(),OptVar, limit);
	    System.out.println(solver.model_name() + " Initiated");
	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public int getSolutionNumber() {
		return TotalRuns;
	}
	public Solution cloneSolution() {
		return AssignSolution(Offset, Costs);
	}
	int NOutports;
	int TotalVars;
	IntVar[][][] Offset;
	IntVar[] Costs;
	OptimizeVar OptVar;
	int TotalRuns = 0;
	
	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);
		
		//return false;
    	
		if((TotalRuns >= 1)){
			return true;
		}else {
			return false;

		}

	}

	
	private int AssignVars(IntVar[][][] Offset) {
		int counter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					Offset[counter] = new IntVar[port.AssignedStreams.size()][];				
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						Offset[counter][i]  = new IntVar[port.AssignedStreams.get(i).N_instances];
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							Offset[counter][i][j] = solver.makeIntVar(0, ((port.AssignedStreams.get(i).Period - port.AssignedStreams.get(i).Transmit_Time) / sw.microtick), ("F_" + counter + "_"+ i + "_"+j));
							Totalvars++;
						}
						
					}
		
					counter++;
				}
			}
		}
		return Totalvars;
	}
	private void FlatArray(IntVar[][][] source, IntVar[] destination, int sourcesize) {
		int counter = 0;
		for (int i = 0; i < sourcesize; i++) {
			for (int j = 0; j < source[i].length; j++) {
				for (int j2 = 0; j2 < source[i][j].length; j2++) {
					destination[counter] = source[i][j][j2];
					counter++;
				}

			}
		}
	}
	private Solution AssignSolution(IntVar[][][] Offset, IntVar[] costs)  {
		Current.costValues.clear();
		Current.Variables = TotalVars;
		for (int i = 0; i < costs.length; i++) {
			Current.costValues.add(costs[i].value());
		}
		
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					
					int TotalGCL =0;
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						TotalGCL += port.AssignedStreams.get(i).N_instances;
					}
					
					port.SetGCLs(TotalGCL);
					int gclcounter = 0;
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							int offsetvalue = (int) Offset[counter][i][j].value();
							port.Topen[gclcounter] = (offsetvalue * sw.microtick) + j * port.AssignedStreams.get(i).Period;
							port.Tclose[gclcounter] = port.Topen[gclcounter] + port.AssignedStreams.get(i).Transmit_Time;
							port.affiliatedQue[gclcounter] = port.AssignedStreams.get(i).Priority;
							port.setPeriod(Current.Hyperperiod);
							gclcounter++;
						}
					}				
					counter++;
				}
			}
		}
		return Current.Clone();

	}
	private void Constraint0(IntVar[][][] Offset) {
	//Link Constraint
	//Equation Number 1 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {		
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.size(); j++) {
							if(i != j) {
								int hp = LCM((port.AssignedStreams.get(i).Period / sw.microtick), (port.AssignedStreams.get(j).Period / sw.microtick));
								
								for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
									for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
										int hpi = hp/(port.AssignedStreams.get(i).Period / sw.microtick);
										
										int hpj = hp/(port.AssignedStreams.get(j).Period / sw.microtick);
										for (int a = 0; a < hpi; a++) {
											for (int b = 0; b < hpj; b++) {
												IntVar aVar = solver.makeSum(Offset[counter][i][k], (a * (port.AssignedStreams.get(i).Period / sw.microtick))).var();
												IntVar bVar = solver.makeSum(Offset[counter][j][l], (b * (port.AssignedStreams.get(j).Period / sw.microtick) + (port.AssignedStreams.get(j).Transmit_Time / sw.microtick) )).var();
												IntVar cVar = solver.makeIsGreaterOrEqualVar(aVar, bVar).var();
												IntVar dVar = solver.makeSum(Offset[counter][j][l], (b * (port.AssignedStreams.get(j).Period / sw.microtick))).var();
												IntVar eVar = solver.makeSum(Offset[counter][i][k], (a * (port.AssignedStreams.get(i).Period / sw.microtick) + (port.AssignedStreams.get(i).Transmit_Time / sw.microtick) )).var();
												IntVar fVar = solver.makeIsGreaterOrEqualVar(dVar, eVar).var();
												IntVar gVar = solver.makeSum(cVar, fVar).var();
												solver.addConstraint(solver.makeEquality(gVar, 1));
											}
										}
									}
								}
							}
							
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint1(IntVar[][][] Offset) {
		//Flow Transmission Constraint
		//Equation Number 2 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						String prevSWname = null;
						if (!port.AssignedStreams.get(i).isThisFirstSwtich(sw.Name)) {
							prevSWname = port.AssignedStreams.get(i).getpreviousSwitch(sw.Name);

						}
						if(prevSWname != null) {
							Port prePort = getPortObject(prevSWname, port.AssignedStreams.get(i).Id);
							int preportindex = FindPortIndex(prevSWname, port.AssignedStreams.get(i).Id);
							int preindex = getStreamIndex(prevSWname, port.AssignedStreams.get(i).Id);
							
							for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
								IntVar aVar = solver.makeProd(Offset[counter][i][j], sw.microtick).var();
								IntVar bVar = solver.makeProd(Offset[preportindex][preindex][j], sw.microtick).var();
								IntVar cVar = solver.makeSum(bVar, prePort.AssignedStreams.get(preindex).Transmit_Time).var();
								IntVar dVar = solver.makeSum(cVar, (sw.clockAsync + port.propagationDelay)).var();
								solver.addConstraint(solver.makeGreaterOrEqual(aVar, dVar));
							}
						}
	
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint2(IntVar[][][] Offset) {
		//End to End Constraint
		//Equation Number 3 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		
		for (Stream stream : Current.streams) {
			String firstSWName = stream.getFirstSwitch();		
			String lastSWName = stream.getLastSwitch();
			if ((firstSWName != null) && (lastSWName != null)) {
				Port firstPort = getPortObject(firstSWName, stream.Id);
				int firstportindex = FindPortIndex(firstSWName, stream.Id);
				int firststreamindex = getStreamIndex(firstSWName, stream.Id);
				Port lastPort = getPortObject(lastSWName, stream.Id);
				int lastportindex = FindPortIndex(lastSWName, stream.Id);
				int laststreamindex = getStreamIndex(lastSWName, stream.Id);
				for (int i = 0; i < stream.N_instances; i++) {
					IntVar aVar = solver.makeProd(Offset[firstportindex][firststreamindex][i], firstPort._microtick).var();
					IntVar bVar = solver.makeSum(aVar, stream.Deadline).var();
					IntVar cVar = solver.makeProd(Offset[lastportindex][laststreamindex][i], lastPort._microtick).var();
					IntVar dVar = solver.makeSum(cVar, stream.Transmit_Time).var();
					solver.addConstraint(solver.makeGreaterOrEqual(bVar, dVar));
				}


			}
		}
		
	}
	private void Constraint3(IntVar[][][] Offset) {
		//Isolation Constraint
		//Equation Number 6 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.size(); j++) {
							if (i != j) {
								String preswitchI = null;
								String preswitchJ = null;
								if (!port.AssignedStreams.get(i).isThisFirstSwtich(sw.Name)) {
									preswitchI = port.AssignedStreams.get(i).getpreviousSwitch(sw.Name);

								}
								if (!port.AssignedStreams.get(j).isThisFirstSwtich(sw.Name)) {
									preswitchJ = port.AssignedStreams.get(j).getpreviousSwitch(sw.Name);

								}
								
								if((preswitchI != null) && (preswitchJ != null)) {
									Port prePortI = getPortObject(preswitchI, port.AssignedStreams.get(i).Id);
									int preportindexI = FindPortIndex(preswitchI, port.AssignedStreams.get(i).Id);
									int prestreamindexI = getStreamIndex(preswitchI, port.AssignedStreams.get(i).Id);
									Port prePortJ = getPortObject(preswitchJ, port.AssignedStreams.get(j).Id);
									int preportindexJ = FindPortIndex(preswitchJ, port.AssignedStreams.get(j).Id);
									int prestreamindexJ = getStreamIndex(preswitchJ, port.AssignedStreams.get(j).Id);
									int hp = LCM((port.AssignedStreams.get(i).Period / sw.microtick), (port.AssignedStreams.get(j).Period / sw.microtick));
									for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
										for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
											int hpi = hp/(port.AssignedStreams.get(i).Period / sw.microtick);
											int hpj = hp/(port.AssignedStreams.get(j).Period / sw.microtick);
											for (int a = 0; a < hpi; a++) {
												for (int b = 0; b < hpj; b++) {
													IntVar aVar = solver.makeProd(Offset[counter][j][l], sw.microtick).var();
													IntVar bVar = solver.makeSum(aVar, (a * port.AssignedStreams.get(j).Period + sw.clockAsync)).var();
													IntVar cVar = solver.makeProd(Offset[preportindexI][prestreamindexI][k], sw.microtick).var();
													IntVar dVar = solver.makeSum(cVar, (b * port.AssignedStreams.get(i).Period + port.propagationDelay)).var();
													IntVar eVar = solver.makeIsLessOrEqualVar(bVar, dVar);
													
													IntVar fVar = solver.makeProd(Offset[counter][i][k], sw.microtick).var();
													IntVar gVar = solver.makeSum(fVar, (b * port.AssignedStreams.get(i).Period + sw.clockAsync)).var();
													IntVar hVar = solver.makeProd(Offset[preportindexJ][prestreamindexJ][l], sw.microtick).var();
													IntVar iVar = solver.makeSum(hVar, (a * port.AssignedStreams.get(j).Period + port.propagationDelay)).var();
													IntVar jVar = solver.makeIsLessOrEqualVar(gVar, iVar);
													
													IntVar kVar = solver.makeSum(eVar, jVar).var();
													
													
													if (port.AssignedStreams.get(i).Priority != port.AssignedStreams.get(j).Priority){
														//solver.addConstraint(solver.makeEquality(kVar, 0));

													}else {
														solver.addConstraint(solver.makeEquality(kVar, 1));

													}
														
												}													
													
													
													
										}
									
										}
									}

								}
							}
						}
					}
					counter++;
				}
			}
			
		}
	}

	private OptimizeVar CostMinimizer(IntVar[] Costs) {
		IntVar tempIntVar = null;
		tempIntVar = solver.makeProd(Costs[0], 1).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[1], 1).var()).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[2], 0).var()).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[3], 1).var()).var();
		Costs[4] = tempIntVar;
		return solver.makeMinimize(Costs[4],3);
		

	}
	private OptimizeVar Cost0(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {

			String firstswitch = stream.getFirstSwitch();
			int firstindex = FindPortIndex(firstswitch, stream.Id);
			if(firstindex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						int streamindex = getStreamIndex(firstswitch, stream.Id);
						IntVar aVar = solver.makeAbs(solver.makeDifference(0, Offset[firstindex][streamindex][i])).var();
						IntVar bVar = solver.makeAbs(solver.makeDifference(0, Offset[firstindex][streamindex][j])).var();
						IntVar cExpr = solver.makeAbs(solver.makeDifference(aVar, bVar)).var();
						if(eExpr == null) {
							eExpr = cExpr;
						}else {
							eExpr = solver.makeSum(eExpr, cExpr).var();
						}

					}
				}
			}
			
		}
		Costs[0] = eExpr;
		return solver.makeMinimize(Costs[0], 1);
	}
	private OptimizeVar Cost1(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {

			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						int streamindex = getStreamIndex(lastswitch, stream.Id);
						IntVar aVar = solver.makeAbs(solver.makeDifference(0, Offset[lastIndex][streamindex][i])).var();
						IntVar bVar = solver.makeAbs(solver.makeDifference(0, Offset[lastIndex][streamindex][j])).var();
						IntVar cExpr = solver.makeAbs(solver.makeDifference(aVar, bVar)).var();
					
						if(eExpr == null) {
							eExpr = cExpr;
						}else {
							eExpr = solver.makeSum(eExpr, cExpr).var();
						}

					}
				}
			}
			
		}
		Costs[1] = eExpr;
		return solver.makeMinimize(Costs[1], 1);
	}
	
	private OptimizeVar Cost2(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {		
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							int streamUsage = (port.AssignedStreams.get(i).Transmit_Time * 10000) / port.AssignedStreams.get(i).Period;
							IntVar aVar = solver.makeIntConst(streamUsage);
							if(eExpr == null) {
								eExpr = aVar;
							}else {
								eExpr = solver.makeSum(eExpr, aVar).var();
							}
						}
						
					}
		
					counter++;
				}
			}
		}
		eExpr = solver.makeDiv(eExpr, TotalVars).var();
		Costs[2] = eExpr;
		return solver.makeMinimize(Costs[2], 1);
	}
	
	private OptimizeVar Cost3(IntVar[][][] Offset, IntVar[] Costs) {
		
		IntVar eExpr = null;
		int totalinstances = 0;
		for (Stream stream : Current.streams) {
			String firstSWName = stream.getFirstSwitch();		
			String lastSWName = stream.getLastSwitch();
			if ((firstSWName != null) && (lastSWName != null)) {
				Port firstPort = getPortObject(firstSWName, stream.Id);
				int firstportindex = FindPortIndex(firstSWName, stream.Id);
				int firststreamindex = getStreamIndex(firstSWName, stream.Id);
				Port lastPort = getPortObject(lastSWName, stream.Id);
				int lastportindex = FindPortIndex(lastSWName, stream.Id);
				int laststreamindex = getStreamIndex(lastSWName, stream.Id);
				for (int i = 0; i < stream.N_instances; i++) {
					
					IntVar aVar = solver.makeProd(Offset[lastportindex][laststreamindex][i], lastPort._microtick).var();
					aVar = solver.makeSum(aVar, stream.Transmit_Time).var();
					totalinstances++;
					if(eExpr == null) {
						eExpr = aVar;
					}else {
						eExpr = solver.makeSum(eExpr, aVar).var();
					}
				}


			}
		}
		
		eExpr = solver.makeDiv(eExpr, totalinstances).var();
		
		Costs[3] = eExpr;
		return solver.makeMinimize(Costs[3], 1);
	}

	private int FindPortIndex(String swName, int mID) {
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					if(sw.Name.equals(swName)) {
		        		Optional<Stream> tempStream = port.AssignedStreams.stream().filter(x -> x.Id == mID).findFirst();
		        		if (!tempStream.isEmpty()) {
		        			return counter;
		        		}
					}
					counter++;
				}
			}
		}
		return -1;
	}
	private Port getPortObject(String swName, int mID) {
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					if(sw.Name.equals(swName)) {
		        		Optional<Stream> tempStream = port.AssignedStreams.stream().filter(x -> x.Id == mID).findFirst();
		        		if (!tempStream.isEmpty()) {
		        			return port;
		        		}
					}
				}
			}
		}
		return null;
	}
	private int getStreamIndex(String swName, int mID) {
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					if(sw.Name.equals(swName)) {
						for (int i = 0; i < port.AssignedStreams.size(); i++) {
							if(port.AssignedStreams.get(i).Id == mID) {
								return i;
							}
						}
					}
				}
			}
		}
		return -1;
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
