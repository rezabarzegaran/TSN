package TSN;

import java.util.Optional;

import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.LocalSearchPhaseParameters;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.SearchMonitor;
import com.google.ortools.constraintsolver.Solver;

class Romon extends SolutionMethod{
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Romon(Solver _solver) {
		solver = _solver;
	}
	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}
	private void setInit(Solution init) {
		Current = init;	
	}
	private void initVariables() {
		NOutports = Current.getNOutPorts();
		Topen = new IntVar[NOutports][];
		Tclose = new IntVar[NOutports][];
		Paff = new IntVar[NOutports][];
		Waff = new IntVar[NOutports][][];
		Jitters = new IntVar[4];
		TotalVars = AssignVars(Topen, Tclose, Paff, Waff);
	}
	public void addConstraints() {
		Constraint0(Topen, Tclose, Paff, Waff);
		Constraint1(Topen, Tclose, Paff, Waff);
		Constraint2(Topen, Tclose, Paff, Waff);
		Constraint3(Topen, Tclose, Paff, Waff);
		Constraint4_main(Topen, Tclose, Paff, Waff);
		//Constraint4(Topen, Tclose, Paff, Waff);
		Constraint5(Topen, Tclose, Paff, Waff);
		Constraint6(Topen, Tclose, Paff, Waff);
		Constraint7(Topen, Tclose, Paff, Waff);
		//Constraint8(Topen, Tclose, Paff, Waff);
		//Constraint9(Topen, Tclose, Paff, Waff);
		Constraint10(Topen, Tclose, Paff, Waff);
		SetDefaultSolution(Topen, Tclose, Paff, Waff);
		BoundedStartJitterConstraint(Topen, Tclose, Paff, Waff);
	}
	public void addCosts() {
		Cost0(Topen, Tclose, Paff, Waff, Jitters);
		Cost1(Topen, Tclose, Paff, Waff, Jitters);
		Cost2(Topen, Tclose, Paff, Waff, Jitters);
		//Cost3(Topen, Tclose, Paff, Waff, Jitters);
		//Cost4(Topen, Tclose, Paff, Waff, Jitters);
		OptVar = CostMinimizer(Jitters);
		//CostLimiter(Topen, Tclose, Paff, Waff, Jitters);
		
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		IntVar[] w = new IntVar[get3DarraySize(Waff)];
		IntVar[] T = new IntVar[2*TotalVars];
		FlatArray(Topen, x, NOutports);
		FlatArray(Tclose, y, NOutports);
		FlatArray(Paff, z, NOutports);
		FlatArray3D(Waff, w, NOutports);
		//FlatAll(w, x, y, z, T1);
		//FlatAll(x, y, z, T2);
		FlatAll(x, y, T);
	    DecisionBuilder db0 = solver.makePhase(w, solver.CHOOSE_FIRST_UNBOUND , solver.ASSIGN_MIN_VALUE);
	    DecisionBuilder db1 = solver.makeSolveOnce(db0);
	    DecisionBuilder db2 = solver.makePhase(T, solver.CHOOSE_RANDOM , solver.ASSIGN_RANDOM_VALUE);
	    DecisionBuilder db3 = solver.makePhase(z, solver.CHOOSE_FIRST_UNBOUND , solver.ASSIGN_RANDOM_VALUE);
	    DecisionBuilder db4 = solver.makeSolveOnce(db3);
	    DecisionBuilder db5 = solver.compose(db1, db2);
	    
	    //DecisionBuilder db6 = solver.makePhase(Jitters[3], solver.CHOOSE_FIRST_UNBOUND,solver.ASSIGN_MIN_VALUE);
	    DecisionBuilder db7 = solver.compose(db5, db4);
	    //db = solver.compose(db7, db6);
	    db = db7;
	}
	public void addSolverLimits() {
		int hours = 1;
		int minutes = 0;
		int dur = (hours * 3600 + minutes * 60) * 1000; 
		var limit = solver.makeTimeLimit(dur);
		SearchMonitor[] searchVar = new SearchMonitor[2];
		// Search Type
		// Normal Search
		searchVar[0] = OptVar;
		
		// Simulated Annealing
		//searchVar[0] = solver.makeSimulatedAnnealing(false, Jitters[3], 1, 100000);
		
		// Tabu Search
		IntVar[] x = new IntVar[TotalVars];
		FlatArray(Topen, x, NOutports);
		//long keep_tenure = (long) (TotalVars * 0.6);
		//long forbid_tenure = (long) (TotalVars * 0.3);
		long keep_tenure = 20;
		long forbid_tenure = 5;
		//searchVar[0] = solver.makeTabuSearch(false, Jitters[3], 1, x, keep_tenure, forbid_tenure, 1.0);
				
				
		searchVar[1] = limit;
		solver.newSearch(getDecision(),searchVar);
	    System.out.println(solver.model_name() + " Initiated");
	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public Solution cloneSolution() {
		return AssignSolution(Topen, Tclose, Paff, Waff, Jitters);
	}
	public int getSolutionNumber() {
		return TotalRuns;
	}
	int NOutports;
	int TotalVars;
	
	IntVar[][] Topen;
	IntVar[][] Tclose;
	IntVar[][] Paff;
	IntVar[][][] Waff;
	IntVar[] Jitters;
	OptimizeVar OptVar;
	int TotalRuns = 0;
	
	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);
		
		//return false;
    	
		if((TotalRuns >= 3000)){
			return true;
		}else {
			return false;

		}

	}

	
	private int AssignVars(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		int counter = 0;
		int Totalvars = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					Topen[counter] = new IntVar[port.GCLSize];
					Tclose[counter] = new IntVar[port.GCLSize];
					Paff[counter] = new IntVar[port.GCLSize];
					Waff[counter] = new IntVar[port.AssignedStreams.size()][];
					
					for (int i = 0; i < port.GCLSize; i++) {

						Topen[counter][i] = solver.makeIntVar(0, Current.Hyperperiod, ("O_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Tclose[counter][i] = solver.makeIntVar(0, Current.Hyperperiod, ("C_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Paff[counter][i] = solver.makeIntVar(0, port.GetNQue(), ("A_" + sw.Name + "_"+ port.connectedTo + "_"+i));
						Totalvars++;
					}
					
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						Waff[counter][i] = new IntVar[port.AssignedStreams.get(i).N_instances];
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							Waff[counter][i][j] = solver.makeIntVar(0, port.GCLSize, "W_" + sw.Name + "_" + port.connectedTo+"_" + i + "_" + j);
						}
					}
					
					counter++;
					System.out.println(port.GCLSize);
				}
			}
		}
		return Totalvars;
	}
	private void FlatAll(IntVar[] source1, IntVar[] source2, IntVar[] source3, IntVar[] source4, IntVar[] destination) {
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
			totalvars += Current.Hyperperiod;
		}
		for (int i = 0; i < source4.length; i++) {
			destination[counter] = source4[i];
			counter++;
			totalvars += 8;
		}
		System.out.println("Total Number of Variables are:" + totalvars);
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
	private void FlatAll(IntVar[] source1, IntVar[] source2, IntVar[] destination) {
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
	private void FlatArray3D(IntVar[][][] source, IntVar[] destination, int sourcesize) {
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
	private int get3DarraySize(IntVar[][][] source) {
		int totallenght = 0;
		for (int i = 0; i < source.length; i++) {
			for (int j = 0; j < source[i].length; j++) {
				totallenght += source[i][j].length;
			}
		}
		
		return totallenght;
	}
	private Solution AssignSolution(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] Jitter)  {
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
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							Long temp = Waff[counter][i][j].value();
							port.indexMap[i][j] = temp.intValue();
						}
						
					}
					
					
					counter++;
				}
			}
		}
		return Current.Clone();

	}
	private void Constraint0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
	//Assign Que to Window and Frame
		//Equation Number 1 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							//solver.addConstraint(solver.makeEquality(Paff[counter][port.indexMap[i][j]], port.AssignedStreams.get(i).Priority ));
							
							
							var indexvar= solver.makeElement(Paff[counter], Waff[counter][i][j]).var();
							solver.addConstraint(solver.makeEquality(indexvar, port.AssignedStreams.get(i).Priority ));
							
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		//Assign sequence of windows for each stream
		//Equation Number 2 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							var indexvar= solver.makeElement(Topen[counter], Waff[counter][i][j]).var();
							//solver.addConstraint(solver.makeGreaterOrEqual(Topen[counter][port.indexMap[i][j]], j * port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeGreaterOrEqual(indexvar, j * port.AssignedStreams.get(i).Period));
							//solver.addConstraint(solver.makeLess(Tclose[counter][port.indexMap[i][j]], (j+1) * port.AssignedStreams.get(i).Period));
							var indexvar2= solver.makeElement(Tclose[counter], Waff[counter][i][j]).var();
							solver.addConstraint(solver.makeLess(indexvar2, (j+1) * port.AssignedStreams.get(i).Period));
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint2(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// Assign constraint for periodic GCLs
		//Equation Number 3 part 1 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < (port.AssignedStreams.get(i).N_instances - 1); j++) {
							var indexvar= solver.makeElement(Topen[counter], Waff[counter][i][j+1]).var();
							var indexvar2= solver.makeElement(Topen[counter], Waff[counter][i][j]).var();
							//solver.addConstraint(solver.makeEquality(solver.makeDifference(Topen[counter][port.indexMap[i][j+1]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Period));
							//solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(Topen[counter][port.indexMap[i][j+1]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(indexvar, indexvar2).var(), port.AssignedStreams.get(i).Period));

						}
						
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint3(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// Assign constraint for periodic GCLs, window for fixed duration
		//Equation Number 3, part 2 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {	
							var indexvar= solver.makeElement(Tclose[counter], Waff[counter][i][j]).var();
							var indexvar2= solver.makeElement(Topen[counter], Waff[counter][i][j]).var();
							//solver.addConstraint(solver.makeEquality(solver.makeDifference(Tclose[counter][port.indexMap[i][j]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Transmit_Time));
							//solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(Tclose[counter][port.indexMap[i][j]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Transmit_Time));
							//solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(indexvar, indexvar2).var(), port.AssignedStreams.get(i).Transmit_Time));
							IntVar duration = null;
							for (int k = 0; k < port.AssignedStreams.size(); k++) {
								for (int l = 0; l < port.AssignedStreams.get(k).N_instances; l++) {
									
										if (duration == null) {
											duration = solver.makeProd(solver.makeIsEqualVar(Waff[counter][i][j], Waff[counter][k][l]), port.AssignedStreams.get(k).Transmit_Time).var();
										
										}else {
											duration = solver.makeSum(duration, solver.makeProd(solver.makeIsEqualVar(Waff[counter][i][j], Waff[counter][k][l]), port.AssignedStreams.get(k).Transmit_Time).var()).var();

										}

								}
							}
							solver.addConstraint(solver.makeEquality(solver.makeDifference(indexvar, indexvar2).var(), duration));

							
						}
						
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint4_main(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// No overlaping windows
		//Equation Number 4 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
						
							for (int k = 0; k < port.AssignedStreams.size(); k++) {
								for (int l = 0; l < port.AssignedStreams.get(k).N_instances; l++) {
									
										var indexvar= solver.makeElement(Tclose[counter], Waff[counter][i][j]).var();
										var indexvar2= solver.makeElement(Topen[counter], Waff[counter][k][l]).var();
										var indexvar3= solver.makeElement(Topen[counter], Waff[counter][i][j]).var();
										var indexvar4= solver.makeElement(Tclose[counter], Waff[counter][k][l]).var();
										var aExpr = solver.makeIsEqualVar(Waff[counter][i][j], Waff[counter][k][l]).var();
										var bExpr = solver.makeDifference(1, aExpr);
										solver.addConstraint(solver.makeEquality(solver.makeSum(solver.makeIsLessOrEqualVar(indexvar, indexvar2).var(),solver.makeIsGreaterOrEqualVar(indexvar3, indexvar4).var()), bExpr));
									
								}
							}
						}
					}
										
					for (int i = 0; i < port.GCLSize; i++) {
						for (int j = 0; j < port.GCLSize; j++) {
							if(j != i) {
								
								
							
								//solver.addConstraint(solver.makeEquality(solver.makeSum(solver.makeIsLessOrEqualVar(Tclose[counter][i], Topen[counter][j]),solver.makeIsGreaterOrEqualVar(Topen[counter][i], Tclose[counter][j])), 1));
								//solver.makeSum(solver.makeIsGreaterOrEqualVar(Tclose[counter][i], Topen[counter][j]),solver.makeIsGreaterOrEqualVar(Topen[counter][i], Tclose[counter][j]));
								//solver.makeIsGreaterOrEqualVar(Tclose[counter][i], Topen[counter][j]);
								//solver.makeIsGreaterOrEqualVar(Topen[counter][i], Tclose[counter][j]);

								//solver.makeGreater(solver.makeDifference(Topen[counter][i], Topen[counter][j]), solver.makeDifference(Tclose[counter][j],Topen[counter][j]));
								//solver.makeBetweenCt(arg0, arg1, arg2);
								//solver.makeGreaterOrEqual(solver.makeDifference(Tclose[counter][i], Topen[counter][j]) && solver.makeDifference(Tclose[counter][i], Topen[counter][j]), 0);
								//solver.makeGreaterOrEqual(solver.makeAbs(solver.makeDifference(Tclose[counter][i], Topen[counter][j])), 0);
								
							}
						}
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint4(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// No overlaping windows
		//Equation Number 5 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < (port.GCLSize - 1); i++) {
						solver.addConstraint(solver.makeLessOrEqual(Tclose[counter][i], Topen[counter][i+1]));
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint5(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// No overlaping windows
		//Equation Number 10 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					if(!port.connectedToES) {
						int secondcounter = 0;
						for (Switches nextsw : Current.SW) {
							for (Port nextPort : nextsw.ports) {
								if (nextPort.outPort) {
									if(nextsw.Name.equals(port.connectedTo)) {
										for (int j = 0; j < port.AssignedStreams.size(); j++) {
											for (int k = 0; k < port.AssignedStreams.get(j).N_instances; k++) {
												for (int l = 0; l < nextPort.AssignedStreams.size(); l++) {
													if(nextPort.AssignedStreams.get(l).Id == port.AssignedStreams.get(j).Id) {
														
														var indexvar= solver.makeElement(Tclose[counter], Waff[counter][j][k]).var();
														
														var indexvar2= solver.makeElement(Topen[secondcounter], Waff[secondcounter][l][k]).var();
														
														//solver.addConstraint(solver.makeLessOrEqual(solver.makeSum(Tclose[counter][port.indexMap[j][k]],sw.clockAsync), Topen[secondcounter][nextPort.indexMap[l][k]]));

														
														solver.addConstraint(solver.makeLessOrEqual(solver.makeSum(indexvar,sw.clockAsync).var(), indexvar2));

													}
												}
											}

										}
										

									}
									secondcounter++;
								}

							}
						}
					}

					counter++;
				}
			}
			
		}
	}
	private void Constraint6(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// No overlaping windows
		//Equation Number 11 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						if(!port.AssignedStreams.get(i).isThisFirstSwtich(sw.Name)) {
							for (int j = 0; j < port.AssignedStreams.size(); j++) {
								if(!port.AssignedStreams.get(j).isThisFirstSwtich(sw.Name) && !port.AssignedStreams.get(i).isThisFirstSwtich(sw.Name) &&(i != j)) {
									int counterprej = FindPortIndex(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									Port portprevj = getPortObject(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									int indexprevj = getStreamIndex(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									
									int counterprei = FindPortIndex(port.AssignedStreams.get(i).getpreviousSwitch(sw.Name), port.AssignedStreams.get(i).Id);
									Port portprevi = getPortObject(port.AssignedStreams.get(i).getpreviousSwitch(sw.Name), port.AssignedStreams.get(i).Id);
									int indexprevi = getStreamIndex(port.AssignedStreams.get(i).getpreviousSwitch(sw.Name), port.AssignedStreams.get(i).Id);
									
									for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
										
										for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
											
											var indexvar= solver.makeElement(Tclose[counter], Waff[counter][i][k]).var();
											
											//IntExpr aExpr = solver.makeSum(Tclose[counter][port.indexMap[i][k]] ,sw.clockAsync);
											IntVar aExpr = solver.makeSum(indexvar ,sw.clockAsync).var();
											
											var indexvar2= solver.makeElement(Topen[counterprej], Waff[counterprej][indexprevj][l]).var();
											//IntExpr bExpr = solver.makeIsLessOrEqualVar(aExpr, Topen[counter2][prevport.indexMap[Previndex][l]]);
											IntVar bExpr = solver.makeIsLessOrEqualVar(aExpr, indexvar2).var();
											
											
											var indexvar3= solver.makeElement(Tclose[counter], Waff[counter][j][l]).var();
											//IntExpr cExpr = solver.makeSum(Tclose[counter2][prevport.indexMap[Previndex][l]] ,sw.clockAsync);
											IntVar cExpr = solver.makeSum(indexvar3 ,sw.clockAsync).var();
											var indexvar4= solver.makeElement(Topen[counterprei], Waff[counterprei][indexprevi][k]).var();
											//IntExpr dExpr = solver.makeIsLessOrEqualVar(cExpr, Topen[counter][port.indexMap[i][k]]);
											IntVar dExpr = solver.makeIsLessOrEqualVar(cExpr, indexvar4).var();
											
											var indexvar5= solver.makeElement(Paff[counter], Waff[counter][i][k]).var();
											var indexvar6= solver.makeElement(Paff[counter], Waff[counter][j][l]).var();
											//IntExpr eExpr = solver.makeIsEqualVar(Paff[counter][port.indexMap[i][k]], Paff[counter][port.indexMap[j][l]]);
											
											IntVar eExpr = solver.makeIsEqualVar(indexvar5, indexvar6).var();
											IntVar fExpr = solver.makeDifference(1, eExpr).var();
											//IntVar fExpr = solver.makeSum(eExpr, 0).var();
											//IntExpr fExpr = solver.makeIsEqualVar(port.indexMap[i][k], port.indexMap[j][l]);
											//if(port.indexMap[i][k] == port.indexMap[j][l]) {

												//fExpr = solver.makeSum(eExpr, 1);
												
											//}
											
											IntVar zExpr = solver.makeIsEqualVar(Waff[counter][i][k], Waff[counter][j][l]).var();
											//fExpr = solver.makeSum(eExpr, zExpr).var();
											
											IntVar gExpr = solver.makeSum(bExpr, dExpr).var();
											IntVar hExpr = solver.makeSum(gExpr, fExpr).var();
											IntVar iExpr = solver.makeSum(hExpr, zExpr).var();
											solver.addConstraint(solver.makeLess(iExpr, 3));
											
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
	private void Constraint7(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// End-to-End latancy
		//Equation Number 12 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			String lastSwitch = stream.getLastSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			int lastIndex = FindPortIndex(lastSwitch, stream.Id);
			if((firstIndex != -1) && (lastIndex != -1)) {
				for (int i = 0; i < stream.N_instances; i++) {
					
					var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastSwitch, stream.Id)][i]).var();
					var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]).var();
					
					//solver.addConstraint(solver.makeLessOrEqual(solver.makeDifference(Tclose[lastIndex][getPortObject(lastSwitch, stream.Id).indexMap[getStreamIndex(lastSwitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]), (stream.Deadline -  Current.SW.get(0).clockAsync)));

					solver.addConstraint(solver.makeLessOrEqual(solver.makeDifference(indexvar, indexvar2).var(), (stream.Deadline -  Current.SW.get(0).clockAsync)));
				}
			}
			
		}
		

	}
	private void Constraint8(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// Offset
		//The opeening should be after offset
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				
				var indexvar= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][0]).var();
				
				//solver.addConstraint(solver.makeGreaterOrEqual(Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][0]], stream.offset));

				solver.addConstraint(solver.makeGreaterOrEqual(indexvar, stream.offset));
			}
			
		}
		

	}
	private void Constraint9(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// AboluteDeadline
		//The opeening should be after offset
		for (Stream stream : Current.streams) {
			String lastSwitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastSwitch, stream.Id);
			if(lastIndex != -1) {
				
				var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastSwitch, stream.Id)][0]).var();
				
				//solver.addConstraint(solver.makeLessOrEqual(Tclose[lastIndex][getPortObject(lastSwitch, stream.Id).indexMap[getStreamIndex(lastSwitch, stream.Id)][0]], stream.Deadline));

				solver.addConstraint(solver.makeLessOrEqual(indexvar, stream.Deadline));
			}
			
		}
	
	}
	private void Constraint10(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		// AboluteDeadline
		//The opeening should be after offset
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < (port.AssignedStreams.get(i).N_instances -1); j++) {
							solver.addConstraint(solver.makeLessOrEqual(Waff[counter][i][j], Waff[counter][i][j+1]));
						}
					}
					
					counter++;
				}
			}
		}

	}
	private void BoundedStartJitterConstraint(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		for (Stream stream : Current.streams) {
			IntVar eExpr = null;
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						
						var indexvar= solver.makeElement(Tclose[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][j]).var();
						//IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][j]]));

						IntVar aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), indexvar)).var();
						
						var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]).var();
						//IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]));

						IntVar bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), indexvar2)).var();
						IntVar cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr)).var();
						IntVar dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr)).var();
						if(eExpr == null) {
							eExpr = dExpr.var();
						}else {
							eExpr = solver.makeSum(eExpr, dExpr.var()).var();
						}

					}
				}
				solver.addConstraint(solver.makeLessOrEqual(eExpr, (long) (stream.Period * 0.01)));

			}
			
		}

	}


	private OptimizeVar CostMinimizer(IntVar[] Costs) {
		IntVar tempIntVar = null;
		tempIntVar = solver.makeProd(Costs[0], 0).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[1], 0).var()).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[2], 1).var()).var();
		//tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[3], 0).var()).var();
		Costs[3] = tempIntVar;
		return solver.makeMinimize(Costs[3],1);
		

	}
	private OptimizeVar Cost0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff,IntVar[] SenderJitter) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						
						var indexvar= solver.makeElement(Tclose[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][j]).var();
						//IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][j]]));

						IntVar aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), indexvar)).var();
						
						var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]).var();
						//IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]));

						IntVar bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), indexvar2)).var();
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
	private OptimizeVar Cost1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] ReciverJitter) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {

			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][j]).var();
						//IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][j]]));

						IntVar aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), indexvar)).var();
						
						var indexvar2= solver.makeElement(Topen[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][i]).var();
						//IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]]));

						IntVar bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), indexvar2)).var();
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
		ReciverJitter[1] = eExpr;
		return solver.makeMinimize(ReciverJitter[1], 1);
	}
	private OptimizeVar Cost2(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] ReciverJitter) {
		IntVar bExpr= null;
		for (Stream stream : Current.streams) {
			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][i]).var();
					var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]).var();
					//IntExpr aExpr = solver.makeDifference(Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]);

					IntVar aExpr = solver.makeDifference(indexvar, indexvar2).var();
					if(bExpr == null) {
						bExpr = aExpr.var();
					}else {
						bExpr = solver.makeSum(bExpr, aExpr.var()).var();
					}

				}
			}	
		}
		ReciverJitter[2] = bExpr;
		return solver.makeMinimize(ReciverJitter[2], 1);

	}
	private OptimizeVar Cost3(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] ReciverJitter) {
		int counter = 0;
		IntVar aExpr= null;
		IntVar bExpr= null;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						IntVar cExpr = solver.makeMax(Waff[counter][i]).var();
						if(aExpr == null) {
							aExpr = cExpr;
						}else {
							aExpr = solver.makeSum(aExpr, cExpr).var();
						}
						
					}					
					counter++;
					if(bExpr == null) {
						bExpr = aExpr;
					}else {
						bExpr = solver.makeSum(bExpr, aExpr).var();
					}
					aExpr = null;
					
				}
			}
		}
		
		ReciverJitter[3] = bExpr;
		return solver.makeMinimize(ReciverJitter[3], 1);

	}
	private void CostLimiter(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] ReciverJitter) {
			solver.addConstraint(solver.makeLessOrEqual(ReciverJitter[3], 44063));

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
	private void SetDefaultSolution(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff) {
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int GCLrecord = 0;
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							Waff[counter][i][j].setValue(GCLrecord);
							GCLrecord++;
						}
						
					}
					
					counter++;
				}
			}
		}
	}
}
