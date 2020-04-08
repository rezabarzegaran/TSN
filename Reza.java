package TSN;

import java.util.Optional;
import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.SearchMonitor;
import com.google.ortools.constraintsolver.Solver;


public class Reza extends SolutionMethod{
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Reza(Solver _solver) {
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
		Costs = new IntVar[3];
		TotalVars = AssignVars(Offset);
	}
	public void addConstraints() {
		Constraint0(Offset);
		Constraint1(Offset);
		Constraint2(Offset);
		Constraint3(Offset);
		Constraint4(Offset);
		//MyConstraint4(Offset, Costs);
	}
	public void addCosts() {
		Cost0(Offset, Costs);
		Cost1(Offset, Costs);
		//E2ECost(Offset, Costs);
		//Cost2(Offset, Costs);
		//Cost3(Offset, Costs);
		costVar = CostMinimizer(Costs);
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		FlatArray(Offset, x, NOutports);
		long allvariables = TotalVars;
		System.out.println("There are " + allvariables + "Variables");
		//db = solver.makePhase(x,  solver.INT_VALUE_DEFAULT, solver.ASSIGN_RANDOM_VALUE); // The systematic search method
		db = solver.makePhase(x,  solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT); // The systematic search method

	}
	public void addSolverLimits() {
		int hours = 1;
		int minutes = 0;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		SearchMonitor[] searchVar = new SearchMonitor[2];
		searchVar[0] = costVar;
		// Simulated Annealing
		//searchVar[0] = solver.makeSimulatedAnnealing(false, Costs[3], 4, 200000);
		
		
		// TABU SEARCH
		//long keep_tenure = (long) (GetImportatnVarsSize() * 0.6);
		//long forbid_tenure = (long) (GetImportatnVarsSize() * 0.15);
		//searchVar[0] = solver.makeTabuSearch(false, Costs[3], 4, GetImportatnVars(), keep_tenure, forbid_tenure, 0.15);
		
		
		//Other Limits
		searchVar[1] = limit;
		//searchVar[2] = solver.makeConstantRestart(500);
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
		return AssignSolution(Offset, Costs);
	}
	int NOutports;
	int TotalVars;
	IntVar[][][] Offset;
	IntVar[] Costs;
	OptimizeVar costVar;
	int TotalRuns = 0;
	
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
		for (int i = 0; i < costs.length; i++) {
			Current.costValues.add(costs[i].value());
		}
		
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					port.SetGCLs(GetPortGCLSize(port));
					int gclcounter = 0;
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							int offsetvalue = (int) Offset[counter][i][j].value();
							port.Topen[gclcounter] = (offsetvalue * sw.microtick) + j * port.AssignedStreams.get(i).Period;
							port.Tclose[gclcounter] = port.Topen[gclcounter] + port.AssignedStreams.get(i).Transmit_Time;
							port.affiliatedQue[gclcounter] = port.AssignedStreams.get(i).Priority;
							gclcounter++;
						}
					}				
					counter++;
				}
			}
		}
		return Current.Clone();

	}
	private int GetPortGCLSize(Port port) {
		int counter = 0;
		for (int i = 0; i < port.AssignedStreams.size(); i++) {
			counter += port.AssignedStreams.get(i).N_instances ;
		}
		return counter;
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
								IntVar dVar = solver.makeSum(cVar, sw.getDelay(prePort.AssignedStreams.get(preindex))).var();
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
			String lastSWName = stream.getFirstSwitch();
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
													IntVar dVar = solver.makeSum(cVar, (b * port.AssignedStreams.get(i).Period + sw.getDelay(port.AssignedStreams.get(i)))).var();
													IntVar eVar = solver.makeIsLessOrEqualVar(bVar, dVar);
													
													IntVar fVar = solver.makeProd(Offset[counter][i][k], sw.microtick).var();
													IntVar gVar = solver.makeSum(fVar, (b * port.AssignedStreams.get(i).Period + sw.clockAsync)).var();
													IntVar hVar = solver.makeProd(Offset[preportindexJ][prestreamindexJ][l], sw.microtick).var();
													IntVar iVar = solver.makeSum(hVar, (a * port.AssignedStreams.get(j).Period + sw.getDelay(port.AssignedStreams.get(j)))).var();
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
	private void Constraint4(IntVar[][][] Offset) {
		//Flow Transmission Constraint
		//Equation Number 2 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		for (App CA : Current.Apps) {
			for (int act_id : CA.outputMessages) {
	    		Optional<Stream> tempStream = Current.streams.stream().filter(x -> (x.Id == act_id)).findFirst();
	    		if (tempStream.isPresent()) {
	    			Stream actStream = tempStream.get();
					for (int sen_id : CA.inputMessages) {
			    		Optional<Stream> tempStream2 = Current.streams.stream().filter(x -> (x.Id == sen_id)).findFirst();
			    		if (tempStream2.isPresent()) {
			    			Stream senStream = tempStream2.get();
			    			String actSwitchString = actStream.getFirstSwitch();
			    			Switches actSwitches = getSwitchObject(actSwitchString);
			    			Port actPort = getPortObject(actSwitchString, act_id);
			    			int actPortIndex = FindPortIndex(actSwitchString, act_id);
			    			int actPortStremIndex = getStreamIndex(actSwitchString, act_id);
			    			String senSwitchString = senStream.getLastSwitch();
			    			Switches senSwitches = getSwitchObject(senSwitchString);
			    			Port senPort = getPortObject(senSwitchString, sen_id);
			    			int senPortIndex = FindPortIndex(senSwitchString, sen_id);
			    			int senPortStreamIndex = getStreamIndex(senSwitchString, sen_id);
			    			
			    			
			    			for (int i = 0; i < actStream.N_instances; i++) {
			    				IntVar aVar = solver.makeProd(Offset[senPortIndex][senPortStreamIndex][i], senPort._microtick).var();
			    				IntVar bVar = solver.makeSum(aVar, (senStream.Transmit_Time + senSwitches.getDelay(senStream))).var();
			    				IntVar cVar = solver.makeProd(Offset[actPortIndex][actPortStremIndex][i], actPort._microtick).var();
			    				solver.addConstraint(solver.makeGreaterOrEqual(cVar, bVar));
			    				
			    				
							}
						}

					}

				}

			}
		}

	}

	private OptimizeVar CostMinimizer(IntVar[] Costs) {
		IntVar tempIntVar = null;
		tempIntVar = solver.makeProd(Costs[0], 1).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[1], 1).var()).var();
		//tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[2], 4).var()).var();
		//tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[3], 1).var()).var();
		Costs[2] = tempIntVar;
		//CostLimiter(Costs);
		return solver.makeMinimize(Costs[2],4);
	}
	private void CostLimiter(IntVar[] Costs) {
		solver.addConstraint(solver.makeLessOrEqual(Costs[4], 2809));
	}
	private OptimizeVar Cost0(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {
			
			if(!isCAStream(stream)) {
				String firstswitch = stream.getLastSwitch();
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

			
		}
		Costs[0] = eExpr;
		return solver.makeMinimize(Costs[0], 1);
	}
	private OptimizeVar Cost1(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {
			if(!isCAStream(stream)) {
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
	
		}
		Costs[1] = eExpr;
		return solver.makeMinimize(Costs[1], 1);
	}
	
	private OptimizeVar Cost2(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar fVar = null;
		for (App CA : Current.Apps) {
			for (int act_id : CA.outputMessages) {
	    		Optional<Stream> tempStream = Current.streams.stream().filter(x -> (x.Id == act_id)).findFirst();
	    		if (tempStream.isPresent()) {
	    			Stream actStream = tempStream.get();
					for (int sen_id : CA.inputMessages) {
			    		Optional<Stream> tempStream2 = Current.streams.stream().filter(x -> (x.Id == sen_id)).findFirst();
			    		if (tempStream2.isPresent()) {
			    			Stream senStream = tempStream2.get();
			    			String actSwitchString = actStream.getFirstSwitch();
			    			Switches actSwitches = getSwitchObject(actSwitchString);
			    			Port actPort = getPortObject(actSwitchString, act_id);
			    			int actPortIndex = FindPortIndex(actSwitchString, act_id);
			    			int actPortStremIndex = getStreamIndex(actSwitchString, act_id);
			    			String senSwitchString = senStream.getLastSwitch();
			    			Switches senSwitches = getSwitchObject(senSwitchString);
			    			Port senPort = getPortObject(senSwitchString, sen_id);
			    			int senPortIndex = FindPortIndex(senSwitchString, sen_id);
			    			int senPortStreamIndex = getStreamIndex(senSwitchString, sen_id);
			    			
			    			
			    			for (int i = 0; i < actStream.N_instances; i++) {
			    				IntVar aVar = solver.makeProd(Offset[senPortIndex][senPortStreamIndex][i], senPort._microtick).var();
			    				IntVar bVar = solver.makeSum(aVar, (senStream.Transmit_Time + senSwitches.getDelay(senStream))).var();
			    				IntVar cVar = solver.makeProd(Offset[actPortIndex][actPortStremIndex][i], actPort._microtick).var();
			    				IntVar dVar = solver.makeAbs(solver.makeDifference(cVar, bVar)).var();
			    				IntVar eVar = solver.makeAbs(solver.makeDifference(senStream.Period, dVar)).var();
			    				
			    				
			    				if(fVar == null) {
			    					fVar = eVar;
			    				}else {
			    					fVar = solver.makeSum(fVar, eVar).var();
			    				}
			    				
			    				
							}
						}

					}

				}

			}
		}
		Costs[2] = fVar;
		return solver.makeMaximize(Costs[2], 1);
	}
	private OptimizeVar Cost3(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eVar = null;
		for (App CA : Current.Apps) {
			for (int act_id : CA.outputMessages) {
	    		Optional<Stream> tempStream = Current.streams.stream().filter(x -> (x.Id == act_id)).findFirst();
	    		if (tempStream.isPresent()) {
	    			Stream actStream = tempStream.get();
					for (int sen_id : CA.inputMessages) {
			    		Optional<Stream> tempStream2 = Current.streams.stream().filter(x -> (x.Id == sen_id)).findFirst();
			    		if (tempStream2.isPresent()) {
			    			Stream senStream = tempStream2.get();
			    			String actSwitchString = actStream.getLastSwitch();
			    			Switches actSwitches = getSwitchObject(actSwitchString);
			    			Port actPort = getPortObject(actSwitchString, act_id);
			    			int actPortIndex = FindPortIndex(actSwitchString, act_id);
			    			int actPortStremIndex = getStreamIndex(actSwitchString, act_id);
			    			String senSwitchString = senStream.getFirstSwitch();
			    			Switches senSwitches = getSwitchObject(senSwitchString);
			    			Port senPort = getPortObject(senSwitchString, sen_id);
			    			int senPortIndex = FindPortIndex(senSwitchString, sen_id);
			    			int senPortStreamIndex = getStreamIndex(senSwitchString, sen_id);
			    			IntVar baseTime = null;
			    			
			    			for (int i = 0; i < actStream.N_instances; i++) {
			    				IntVar aVar = solver.makeProd(Offset[senPortIndex][senPortStreamIndex][i], senPort._microtick).var();
			    				IntVar bVar = solver.makeProd(Offset[actPortIndex][actPortStremIndex][i], actPort._microtick).var();
			    				IntVar cVar = solver.makeSum(bVar, (actStream.Transmit_Time + actSwitches.getDelay(actStream))).var();
			    				IntVar dVar = solver.makeAbs(solver.makeDifference(cVar, aVar)).var();
			    				if(baseTime == null) {
			    					baseTime = dVar;
			    				}
			    				IntVar fVar = solver.makeAbs(solver.makeDifference(baseTime, dVar)).var();
			    				if(eVar == null) {
			    					eVar = fVar;
			    				}else {
			    					eVar = solver.makeSum(eVar, fVar).var();
			    				}
			    				
			    				
							}
						}

					}

				}

			}
		}
		Costs[3] = eVar;
		return solver.makeMaximize(Costs[3], 1);
	}
	
	private OptimizeVar E2ECost(IntVar[][][] Offset, IntVar[] Costs) {
		IntVar eExpr = null;
		
		
		for (Stream stream : Current.streams) {
			String firstSWName = stream.getFirstSwitch();		
			String lastSWName = stream.getFirstSwitch();
			if ((firstSWName != null) && (lastSWName != null)) {
				Port firstPort = getPortObject(firstSWName, stream.Id);
				int firstportindex = FindPortIndex(firstSWName, stream.Id);
				int firststreamindex = getStreamIndex(firstSWName, stream.Id);
				Port lastPort = getPortObject(lastSWName, stream.Id);
				int lastportindex = FindPortIndex(lastSWName, stream.Id);
				int laststreamindex = getStreamIndex(lastSWName, stream.Id);
				for (int i = 0; i < stream.N_instances; i++) {
					IntVar aVar = solver.makeProd(Offset[firstportindex][firststreamindex][i], firstPort._microtick).var();
					IntVar bVar = solver.makeProd(Offset[lastportindex][laststreamindex][i], lastPort._microtick).var();
					IntVar cVar = solver.makeSum(bVar, stream.Transmit_Time).var();
					
					IntVar cExpr = solver.makeAbs(solver.makeDifference(cVar, aVar)).var();
					if(eExpr == null) {
						eExpr = cExpr;
					}else {
						eExpr = solver.makeSum(eExpr, cExpr).var();
					}

				}


			}
		}
		
		Costs[2] = eExpr;
		return solver.makeMinimize(Costs[2], 1);
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
	private Switches getSwitchObject(String swName) {
		Optional<Switches> tempSwitch = Current.SW.stream().filter(x -> x.Name.equals(swName)).findFirst();
		if(tempSwitch.isPresent()) {
			return tempSwitch.get();
		}
		return null;
		
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
	private boolean isCAStream(Stream s) {
		for (App CA : Current.Apps) {
			if(CA.isIncluded(s.Id)) {
				return true;
			}
		}
		return false;
	}
	private int GetImportatnVarsSize() {
		int numberofImportantVars = 0;
		for (Stream stream : Current.streams) {
			numberofImportantVars += stream.N_instances;		
		}
		return numberofImportantVars;
	}
	private IntVar[] GetImportatnVars() {
		int counter = 0;
		int VarCounter = 0;
		int numberofImportantVars = GetImportatnVarsSize();
		
		IntVar[] tabuVars = new IntVar[numberofImportantVars];
		

		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						if(port.AssignedStreams.get(i).isThisFirstSwtich(sw.Name)) {
							for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
								tabuVars[VarCounter] = Offset[counter][i][j].var();
								VarCounter++;
							}
						}
					}

					counter++;
				}
			}
		}
		
		return tabuVars;	
		
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
