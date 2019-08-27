package TSN;

import java.util.Optional;
import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Romon {
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	public Romon(Solver _solver) {
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
		Waff = new IntVar[NOutports][][];
		Jitters = new IntVar[5];
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
	}
	public void addCosts() {
		Opt1 = Cost0(Topen, Tclose, Paff, Waff, Jitters);
		Opt2 = Cost1(Topen, Tclose, Paff, Waff, Jitters);
		Opt3 = Cost2(Topen, Tclose, Paff, Waff, Jitters);
		Opt4 = Cost3(Topen, Tclose, Paff, Waff, Jitters);
		Opt5 = CostMinimizer(Jitters);
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		IntVar[] w = new IntVar[get3DarraySize(Waff)];
		FlatArray(Topen, x, NOutports);
		FlatArray(Tclose, y, NOutports);
		FlatArray(Paff, z, NOutports);
		FlatArray3D(Waff, w, NOutports);
	    DecisionBuilder db1 = solver.makePhase(x, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db2 = solver.makePhase(y, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db3 = solver.makePhase(z, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db4 = solver.makePhase(w, solver.INT_VALUE_DEFAULT, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db5 = solver.compose(db1, db2);
	    DecisionBuilder db6 = solver.compose(db5, db3);
	    db = solver.compose(db6, db4);
	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public Solution cloneSolution() {
		return AssignSolution(Topen, Tclose, Paff, Waff, Jitters);
	}
	int NOutports;
	int TotalVars;
	
	IntVar[][] Topen;
	IntVar[][] Tclose;
	IntVar[][] Paff;
	IntVar[][][] Waff;
	IntVar[] Jitters;
	OptimizeVar Opt1;
	OptimizeVar Opt2;
	OptimizeVar Opt3;
	OptimizeVar Opt4;
	OptimizeVar Opt5;
	int TotalRuns = 0;
	
	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);
    	
    	if((TotalRuns >= 20) || (duration >= 10000000)){
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
							
							
							var indexvar= solver.makeElement(Paff[counter], Waff[counter][i][j]);
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
							var indexvar= solver.makeElement(Topen[counter], Waff[counter][i][j]);
							//solver.addConstraint(solver.makeGreaterOrEqual(Topen[counter][port.indexMap[i][j]], j * port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeGreaterOrEqual(indexvar, j * port.AssignedStreams.get(i).Period));
							//solver.addConstraint(solver.makeLess(Tclose[counter][port.indexMap[i][j]], (j+1) * port.AssignedStreams.get(i).Period));
							var indexvar2= solver.makeElement(Tclose[counter], Waff[counter][i][j]);
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
							var indexvar= solver.makeElement(Topen[counter], Waff[counter][i][j+1]);
							var indexvar2= solver.makeElement(Topen[counter], Waff[counter][i][j]);
							//solver.addConstraint(solver.makeEquality(solver.makeDifference(Topen[counter][port.indexMap[i][j+1]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Period));
							//solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(Topen[counter][port.indexMap[i][j+1]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(indexvar, indexvar2), port.AssignedStreams.get(i).Period));

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
							var indexvar= solver.makeElement(Tclose[counter], Waff[counter][i][j]);
							var indexvar2= solver.makeElement(Topen[counter], Waff[counter][i][j]);
							//solver.addConstraint(solver.makeEquality(solver.makeDifference(Tclose[counter][port.indexMap[i][j]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Transmit_Time));
							//solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(Tclose[counter][port.indexMap[i][j]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Transmit_Time));
							solver.addConstraint(solver.makeGreaterOrEqual(solver.makeDifference(indexvar, indexvar2), port.AssignedStreams.get(i).Transmit_Time));

							
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
										
					for (int i = 0; i < port.GCLSize; i++) {
						for (int j = 0; j < port.GCLSize; j++) {
							if(j != i) {
								
								
							
								solver.addConstraint(solver.makeEquality(solver.makeSum(solver.makeIsLessOrEqualVar(Tclose[counter][i], Topen[counter][j]),solver.makeIsGreaterOrEqualVar(Topen[counter][i], Tclose[counter][j])), 1));
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
														
														var indexvar= solver.makeElement(Tclose[counter], Waff[counter][j][k]);
														
														var indexvar2= solver.makeElement(Topen[secondcounter], Waff[counter][l][k]);
														
														//solver.addConstraint(solver.makeLessOrEqual(solver.makeSum(Tclose[counter][port.indexMap[j][k]],sw.clockAsync), Topen[secondcounter][nextPort.indexMap[l][k]]));

														
														solver.addConstraint(solver.makeLessOrEqual(solver.makeSum(indexvar,sw.clockAsync), indexvar2));

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
								if(!port.AssignedStreams.get(j).isThisFirstSwtich(sw.Name) && (i != j)) {
									int counter2 = FindPortIndex(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									Port prevport = getPortObject(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									int Previndex = getStreamIndex(port.AssignedStreams.get(j).getpreviousSwitch(sw.Name), port.AssignedStreams.get(j).Id);
									
									for (int k = 0; k < port.AssignedStreams.get(i).N_instances; k++) {
										
										for (int l = 0; l < port.AssignedStreams.get(j).N_instances; l++) {
											
											var indexvar= solver.makeElement(Tclose[counter], Waff[counter][i][k]);
											
											//IntExpr aExpr = solver.makeSum(Tclose[counter][port.indexMap[i][k]] ,sw.clockAsync);
											IntExpr aExpr = solver.makeSum(indexvar ,sw.clockAsync);
											
											var indexvar2= solver.makeElement(Topen[counter2], Waff[counter2][Previndex][l]);
											//IntExpr bExpr = solver.makeIsLessOrEqualVar(aExpr, Topen[counter2][prevport.indexMap[Previndex][l]]);
											IntExpr bExpr = solver.makeIsLessOrEqualVar(aExpr, indexvar2);
											
											
											var indexvar3= solver.makeElement(Tclose[counter2], Waff[counter2][Previndex][l]);
											//IntExpr cExpr = solver.makeSum(Tclose[counter2][prevport.indexMap[Previndex][l]] ,sw.clockAsync);
											IntExpr cExpr = solver.makeSum(indexvar3 ,sw.clockAsync);
											var indexvar4= solver.makeElement(Topen[counter], Waff[counter][i][k]);
											//IntExpr dExpr = solver.makeIsLessOrEqualVar(cExpr, Topen[counter][port.indexMap[i][k]]);
											IntExpr dExpr = solver.makeIsLessOrEqualVar(cExpr, indexvar4);
											
											var indexvar5= solver.makeElement(Paff[counter], Waff[counter][i][k]);
											var indexvar6= solver.makeElement(Paff[counter], Waff[counter][j][l]);
											//IntExpr eExpr = solver.makeIsEqualVar(Paff[counter][port.indexMap[i][k]], Paff[counter][port.indexMap[j][l]]);
											
											IntExpr eExpr = solver.makeIsEqualVar(indexvar5, indexvar6);
											IntExpr fExpr = solver.makeSum(eExpr, 0);
											//IntExpr fExpr = solver.makeIsEqualVar(port.indexMap[i][k], port.indexMap[j][l]);
											//if(port.indexMap[i][k] == port.indexMap[j][l]) {

												//fExpr = solver.makeSum(eExpr, 1);
												
											//}
											
											IntExpr zExpr = solver.makeIsEqualVar(Waff[counter][i][k], Waff[counter][j][l]);
											fExpr = solver.makeSum(eExpr, zExpr);
											
											IntExpr gExpr = solver.makeSum(bExpr, dExpr);
											IntExpr hExpr = solver.makeSum(gExpr, fExpr);
											solver.addConstraint(solver.makeLess(hExpr, 3));
											
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
					
					var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastSwitch, stream.Id)][i]);
					var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]);
					
					//solver.addConstraint(solver.makeLessOrEqual(solver.makeDifference(Tclose[lastIndex][getPortObject(lastSwitch, stream.Id).indexMap[getStreamIndex(lastSwitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]), (stream.Deadline -  Current.SW.get(0).clockAsync)));

					solver.addConstraint(solver.makeLessOrEqual(solver.makeDifference(indexvar, indexvar2), (stream.Deadline -  Current.SW.get(0).clockAsync)));
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
				
				var indexvar= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][0]);
				
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
				
				var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastSwitch, stream.Id)][0]);
				
				//solver.addConstraint(solver.makeLessOrEqual(Tclose[lastIndex][getPortObject(lastSwitch, stream.Id).indexMap[getStreamIndex(lastSwitch, stream.Id)][0]], stream.Deadline));

				solver.addConstraint(solver.makeLessOrEqual(indexvar, stream.Deadline));
			}
			
		}
		

	}
	private OptimizeVar CostMinimizer(IntVar[] Costs) {
		IntVar tempIntVar = null;
		tempIntVar = solver.makeProd(Costs[0], 1).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[1], 1).var()).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[2], 2).var()).var();
		tempIntVar = solver.makeSum(tempIntVar, solver.makeProd(Costs[3], 3).var()).var();
		Costs[4] = tempIntVar;
		return solver.makeMinimize(Costs[4],1);
		

	}
	private OptimizeVar Cost0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff,IntVar[] SenderJitter) {
		IntVar eExpr = null;
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						
						var indexvar= solver.makeElement(Tclose[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][j]);
						//IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][j]]));

						IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), indexvar));
						
						var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]);
						//IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]));

						IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), indexvar2));
						IntExpr cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr));
						IntExpr dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr));
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
						var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][j]);
						//IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][j]]));

						IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), indexvar));
						
						var indexvar2= solver.makeElement(Topen[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][i]);
						//IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]]));

						IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), indexvar2));
						IntExpr cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr));
						IntExpr dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr));
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
					var indexvar= solver.makeElement(Tclose[lastIndex], Waff[lastIndex][getStreamIndex(lastswitch, stream.Id)][i]);
					var indexvar2= solver.makeElement(Topen[firstIndex], Waff[firstIndex][getStreamIndex(firstSwitch, stream.Id)][i]);
					//IntExpr aExpr = solver.makeDifference(Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]);

					IntExpr aExpr = solver.makeDifference(indexvar, indexvar2);
					if(bExpr == null) {
						bExpr = aExpr.var();
					}else {
						bExpr = solver.makeSum(bExpr, aExpr.var()).var();
					}

				}
			}	
		}
		ReciverJitter[2] = bExpr;
		return solver.makeMinimize(bExpr, 1);

	}
	private OptimizeVar Cost3(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[][][] Waff, IntVar[] ReciverJitter) {
		int counter = 0;
		IntVar aExpr= null;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						
						if(aExpr == null) {
							aExpr = solver.makeMax(Waff[counter][i]).var();
						}else {
							
						}
						aExpr = solver.makeSum(aExpr,solver.makeMax(Waff[counter][i]).var()).var();
					}
					
					
					counter++;
					
				}
			}
		}
		
		ReciverJitter[3] = aExpr;
		return solver.makeMinimize(aExpr, 1);

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
}
