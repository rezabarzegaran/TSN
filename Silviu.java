package TSN;

import java.util.Optional;
import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Silviu {
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Silviu(Solver _solver) {
		solver = _solver;
	}
	public void setInit(Solution init) {
		Current = init;
	}
	public void initVariables() {
		NOutports = Current.getNOutPorts();
		Topen = new IntVar[NOutports][];
		Tclose = new IntVar[NOutports][];
		Paff = new IntVar[NOutports][];
		Jitters = new IntVar[4];
		TotalVars = AssignVars(Topen, Tclose, Paff);
	}
	public void addConstraints() {
		Constraint0(Topen, Tclose, Paff);
		Constraint1(Topen, Tclose, Paff);
		Constraint2(Topen, Tclose, Paff);
		Constraint3(Topen, Tclose, Paff);
		Constraint4(Topen, Tclose, Paff);
		Constraint5(Topen, Tclose, Paff);
		Constraint6(Topen, Tclose, Paff);
	}
	public void addCosts() {
		Opt1 = Cost0(Topen, Tclose, Paff, Jitters);
		Opt2 = Cost1(Topen, Tclose, Paff, Jitters);
		Opt3 = Cost2(Topen, Tclose, Paff, Jitters);
		Opt4 = CostMinimizer(Jitters);
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		IntVar[] T = new IntVar[3*TotalVars];
		FlatArray(Topen, x, NOutports);
		FlatArray(Tclose, y, NOutports);
		FlatArray(Paff, z, NOutports);
		FlatAll(x, y, z, T);
		long allvariables = TotalVars * TotalVars * TotalVars;
		System.out.println("There are " + allvariables + "Variables");
		long timer = (allvariables * Current.Hyperperiod * Current.Hyperperiod * 8 ) / 5000000;
		System.out.println("It takes " + timer + " minutes to solve");
		db = solver.makePhase(T,  solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    //DecisionBuilder db1 = solver.makePhase(w, solver.CHOOSE_FIRST_UNBOUND, solver.ASSIGN_RANDOM_VALUE);
	    //DecisionBuilder db2 = solver.makePhase(x, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    //DecisionBuilder db3 = solver.makePhase(y, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    //DecisionBuilder db4 = solver.makePhase(z, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    //DecisionBuilder db5 = solver.compose(db1, db2);
	    //DecisionBuilder db6 = solver.compose(db5, db3);
	    //db = solver.compose(db6, db4);
	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public Solution cloneSolution() {
		return AssignSolution(Topen, Tclose, Paff, Jitters);
	}
	int NOutports;
	int TotalVars;
	
	IntVar[][] Topen;
	IntVar[][] Tclose;
	IntVar[][] Paff;
	IntVar[] Jitters;
	OptimizeVar Opt1;
	OptimizeVar Opt2;
	OptimizeVar Opt3;
	OptimizeVar Opt4;
	int TotalRuns = 0;
	
	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);
		
		//return false;
    	
		if((TotalRuns >= 10)){
			return true;
		}else {
			return false;

		}

	}

	
	private int AssignVars(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		int counter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					Topen[counter] = new IntVar[port.GCLSize];
					Tclose[counter] = new IntVar[port.GCLSize];
					Paff[counter] = new IntVar[port.GCLSize];					
					for (int i = 0; i < port.GCLSize; i++) {

						Topen[counter][i] = solver.makeIntVar(0, Current.Hyperperiod, ("O_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Tclose[counter][i] = solver.makeIntVar(0, Current.Hyperperiod, ("C_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Paff[counter][i] = solver.makeIntVar(0, port.GetNQue(), ("A_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Totalvars++;
					}
					
					
					counter++;
					System.out.println(port.GCLSize);
				}
			}
		}
		return Totalvars;
	}
	private void FlatAll(IntVar[] source1, IntVar[] source2, IntVar[] source3, IntVar[] destination) {
		int counter = 0;
		int totalvars = 0;
		for (int i = 0; i < source1.length; i++) {
			destination[counter] = source1[i];
			counter++;
			totalvars += Current.Hyperperiod;
		}
		for (int i = 0; i < source2.length; i++) {
			destination[counter] = source2[i];
			counter++;
			totalvars += Current.Hyperperiod;
		}
		for (int i = 0; i < source3.length; i++) {
			destination[counter] = source3[i];
			counter++;
			totalvars += 8;
		}
		System.out.println("Total Number of Variables are:" + totalvars);
	}
	private void FlatArray(IntVar[][] source, IntVar[] destination, int sourcesize) {
		int counter = 0;
		for (int i = 0; i < sourcesize; i++) {
			for (int j = 0; j < source[i].length; j++) {
				destination[counter] = source[i][j];
				counter++;
			}
		}
	}
	private Solution AssignSolution(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[] Jitter)  {
		Current.costValues.clear();
		for (int i = 0; i < Jitter.length; i++) {
			Current.costValues.add(Jitter[i].value());
		}
		
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.GCLSize; i++) {
						port.Topen[i] = (int) Topen[counter][i].value();
						port.Tclose[i] = (int) Tclose[counter][i].value();
						port.affiliatedQue[i]= (int) Paff[counter][i].value();
					}
					
					
					counter++;
				}
			}
		}
		return Current.Clone();

	}
	private void Constraint0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
	//Frame Constraint
	//Equation Number 0 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							IntVar aVar = solver.makeSum(Topen[counter][port.indexMap[i][j]], port.AssignedStreams.get(i).Transmit_Time).var();
							solver.addConstraint(solver.makeLessOrEqual(aVar,Tclose[counter][port.indexMap[i][j]]  ));					
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		//Link Constraint
		//Equation Number 1 of the paper Scheduling Real-time communication in IEEE 802.1 Qbc Time sensitive networks
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.size(); j++) {
							if(i != j) {
								int hp = LCM(port.AssignedStreams.get(i).Period, port.AssignedStreams.get(j).Period);
								for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
									for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
										int hpi = hp/port.AssignedStreams.get(i).Period;
										int hpj = hp/port.AssignedStreams.get(j).Period;
										for (int a = 0; a < hpi; a++) {
											for (int b = 0; b < hpj; b++) {
												IntVar aVar = solver.makeSum(Topen[counter][port.indexMap[i][k]], (a * port.AssignedStreams.get(i).Period)).var();
												IntVar bVar = solver.makeSum(Topen[counter][port.indexMap[j][l]], (b * port.AssignedStreams.get(j).Period + port.AssignedStreams.get(j).Transmit_Time )).var();
												IntVar cVar = solver.makeIsGreaterOrEqualVar(aVar, bVar);
												IntVar dVar = solver.makeSum(Topen[counter][port.indexMap[j][l]], (b * port.AssignedStreams.get(j).Period)).var();
												IntVar eVar = solver.makeSum(Topen[counter][port.indexMap[i][k]], (a * port.AssignedStreams.get(i).Period + port.AssignedStreams.get(i).Transmit_Time )).var();
												IntVar fVar = solver.makeIsGreaterOrEqualVar(dVar, eVar);
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
	private void Constraint2(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
							for (int j = 0; j < (port.AssignedStreams.get(i).N_instances - 1); j++) {
								IntVar aVar = solver.makeSum(Topen[preportindex][prePort.indexMap[preindex][j]], (port.AssignedStreams.get(i).Transmit_Time + sw.clockAsync)).var();
								solver.addConstraint(solver.makeGreaterOrEqual(Topen[counter][port.indexMap[i][j]], aVar));
							}
						}
	
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint3(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
					IntVar aVar = solver.makeSum(Topen[firstportindex][firstPort.indexMap[firststreamindex][i]], stream.Deadline).var();
					IntVar bVar = solver.makeSum(Topen[lastportindex][lastPort.indexMap[laststreamindex][i]], stream.Transmit_Time).var();
					solver.addConstraint(solver.makeGreaterOrEqual(aVar, bVar));
				}


			}
		}
		
	}
	private void Constraint4(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
									int hp = LCM(port.AssignedStreams.get(i).Period, port.AssignedStreams.get(j).Period);
									for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
										for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
											for (int a = 0; a < (hp / port.AssignedStreams.get(i).Period); a++) {
												for (int b = 0; b < port.AssignedStreams.get(j).Period; b++) {
													IntVar aVar = solver.makeSum(Topen[counter][port.indexMap[j][l]], (a * port.AssignedStreams.get(j).Period + sw.clockAsync)).var();
													IntVar bVar = solver.makeSum(Topen[preportindexI][prePortI.indexMap[prestreamindexI][k]], (b * port.AssignedStreams.get(i).Period)).var();
													IntVar cVar = solver.makeIsLessOrEqualVar(aVar, bVar);
													IntVar dVar = solver.makeSum(Topen[counter][prePortI.indexMap[prestreamindexI][k]], (b * port.AssignedStreams.get(i).Period + sw.clockAsync)).var();
													IntVar eVar = solver.makeSum(Topen[preportindexJ][prePortJ.indexMap[prestreamindexJ][l]], (a * port.AssignedStreams.get(j).Period)).var();
													IntVar fVar = solver.makeIsLessOrEqualVar(dVar, eVar);
													IntVar gVar = solver.makeSum(cVar, fVar).var();
													
													if (port.AssignedStreams.get(i).Priority != port.AssignedStreams.get(j).Priority){
														solver.addConstraint(solver.makeEquality(gVar, 0));

													}else {
														solver.addConstraint(solver.makeEquality(gVar, 1));

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
	private void Constraint5(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							solver.addConstraint(solver.makeEquality(Paff[counter][port.indexMap[i][j]], port.AssignedStreams.get(i).Priority ));							
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint6(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							solver.addConstraint(solver.makeGreaterOrEqual(Topen[counter][port.indexMap[i][j]], j * port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeLess(Tclose[counter][port.indexMap[i][j]], (j+1) * port.AssignedStreams.get(i).Period));
							for (int k = (j + 1); k < port.AssignedStreams.get(i).N_instances; k++) {
								//IntVar aVar = solver.makeSum(Topen[counter][port.indexMap[i][j]], port.AssignedStreams.get(i).Transmit_Time).var();
								//solver.addConstraint(solver.makeLessOrEqual(aVar,Topen[counter][port.indexMap[i][k]] ));



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
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[2], 1).var()).var();
		Costs[3] = tempIntVar;
		return solver.makeMinimize(Costs[3],1);
		

	}
	private OptimizeVar Cost0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff,IntVar[] SenderJitter) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						
						IntVar aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][j]])).var();
						
						IntVar bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]])).var();

						IntVar cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr)).var();
						IntVar dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr)).var();
						if(eExpr == null) {
							eExpr = dExpr.var();
						}else {
							eExpr = solver.makeSum(eExpr, dExpr.var()).var();
						}

					}
				}
			}
			
		}
		SenderJitter[0] = eExpr;
		return solver.makeMinimize(SenderJitter[0],1);
	}
	private OptimizeVar Cost1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[] ReciverJitter) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {

			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						IntVar aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][j]])).var();
						IntVar bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]])).var();
						IntVar cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr)).var();
						IntVar dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr)).var();
						if(eExpr == null) {
							eExpr = dExpr;
						}else {
							eExpr = solver.makeSum(eExpr, dExpr).var();
						}

					}
				}
			}
			
		}
		ReciverJitter[1] = eExpr;
		return solver.makeMinimize(ReciverJitter[1], 1);
	}
	private OptimizeVar Cost2(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[] ReciverJitter) {
		IntVar bExpr= null;
		for (Stream stream : Current.streams) {
			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					IntVar aExpr = solver.makeDifference(Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]).var();
					if(bExpr == null) {
						bExpr = aExpr;
					}else {
						bExpr = solver.makeSum(bExpr, aExpr.var()).var();
					}

				}
			}	
		}
		ReciverJitter[2] = bExpr;
		return solver.makeMinimize(ReciverJitter[2], 1);

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
