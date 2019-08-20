package TSN;

import java.util.Optional;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
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
		Jitters = new IntVar[2];
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
		Constraint7(Topen, Tclose, Paff);
	}
	public void addCosts() {
		Cost0(Topen, Tclose, Paff, Jitters);
		Cost1(Topen, Tclose, Paff, Jitters);
	}
	public void addDecision() {
		IntVar[] x = new IntVar[TotalVars];
		IntVar[] y = new IntVar[TotalVars];
		IntVar[] z = new IntVar[TotalVars];
		FlatArray(Topen, x, NOutports);
		FlatArray(Tclose, y, NOutports);
		FlatArray(Paff, z, NOutports);
	    DecisionBuilder db1 = solver.makePhase(x, solver.CHOOSE_FIRST_UNBOUND, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db2 = solver.makePhase(y, solver.CHOOSE_FIRST_UNBOUND, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db3 = solver.makePhase(z, solver.CHOOSE_FIRST_UNBOUND, solver.INT_VALUE_DEFAULT);
	    DecisionBuilder db4 = solver.compose(db1, db2);
	    db = solver.compose(db4, db3);
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
	private void Constraint1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		//Assign sequence of windows for each stream
		//Equation Number 2 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {
							solver.addConstraint(solver.makeGreaterOrEqual(Topen[counter][port.indexMap[i][j]], j * port.AssignedStreams.get(i).Period));
							solver.addConstraint(solver.makeLess(Tclose[counter][port.indexMap[i][j]], (j+1) * port.AssignedStreams.get(i).Period));
						}
						
					}
					counter++;
				}
			}
		}
	}
	private void Constraint2(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		// Assign constraint for periodic GCLs
		//Equation Number 3 part 1 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < (port.AssignedStreams.get(i).N_instances - 1); j++) {
							solver.addConstraint(solver.makeEquality(solver.makeDifference(Topen[counter][port.indexMap[i][j+1]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Period));
						}
						
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint3(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		// Assign constraint for periodic GCLs, window for fixed duration
		//Equation Number 3, part 2 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		int counter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					for (int i = 0; i < port.AssignedStreams.size(); i++) {
						for (int j = 0; j < port.AssignedStreams.get(i).N_instances; j++) {							
							solver.addConstraint(solver.makeEquality(solver.makeDifference(Tclose[counter][port.indexMap[i][j]], Topen[counter][port.indexMap[i][j]]), port.AssignedStreams.get(i).Transmit_Time));
						}
						
					}
					counter++;
				}
			}
			
		}
	}
	private void Constraint4_main(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
	private void Constraint4(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		// No overlaping windows
		//Equation Number 4 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
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
	private void Constraint5(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
														solver.addConstraint(solver.makeLessOrEqual(solver.makeSum(Tclose[counter][port.indexMap[j][k]],sw.clockAsync), Topen[secondcounter][nextPort.indexMap[l][k]]));

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
	private void Constraint6(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
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
											
											IntExpr aExpr = solver.makeSum(Tclose[counter][port.indexMap[i][k]] ,sw.clockAsync);
											IntExpr bExpr = solver.makeIsLessOrEqualVar(aExpr, Topen[counter2][prevport.indexMap[Previndex][l]]);
											
											IntExpr cExpr = solver.makeSum(Tclose[counter2][prevport.indexMap[Previndex][l]] ,sw.clockAsync);
											IntExpr dExpr = solver.makeIsLessOrEqualVar(cExpr, Topen[counter][port.indexMap[i][k]]);
											
											
											IntExpr eExpr = solver.makeIsEqualVar(Paff[counter][port.indexMap[i][k]], Paff[counter][port.indexMap[j][l]]);
											IntExpr fExpr = solver.makeSum(eExpr, 0);
											//IntExpr fExpr = solver.makeIsEqualVar(port.indexMap[i][k], port.indexMap[j][l]);
											if(port.indexMap[i][k] == port.indexMap[j][l]) {

												fExpr = solver.makeSum(eExpr, 1);
												
											}
											IntExpr gExpr = solver.makeSum(bExpr, dExpr);
											IntExpr hExpr = solver.makeSum(gExpr, fExpr);
											solver.addConstraint(solver.makeLessOrEqual(hExpr, 2));
											
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
	private void Constraint7(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff) {
		// End-to-End latancy
		//Equation Number 12 of the paper IEEE802.1Qbv Gate Control List Synthesis using Array Theory Encoding
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			String lastSwitch = stream.getLastSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			int lastIndex = FindPortIndex(lastSwitch, stream.Id);
			if((firstIndex != -1) && (lastIndex != -1)) {
				for (int i = 0; i < stream.N_instances; i++) {
					solver.addConstraint(solver.makeLessOrEqual(solver.makeDifference(Tclose[lastIndex][getPortObject(lastSwitch, stream.Id).indexMap[getStreamIndex(lastSwitch, stream.Id)][i]], Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]), (stream.Deadline -  Current.SW.get(0).clockAsync)));
				}
			}
			
		}
		

	}
	private void Cost0(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[] SenderJitter) {
		for (Stream stream : Current.streams) {
			String firstSwitch = stream.getFirstSwitch();
			int firstIndex = FindPortIndex(firstSwitch, stream.Id);
			if(firstIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][j]]));
						IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[firstIndex][getPortObject(firstSwitch, stream.Id).indexMap[getStreamIndex(firstSwitch, stream.Id)][i]]));
						IntExpr cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr));
						IntExpr dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr));
						SenderJitter[0] = dExpr.var();
						solver.makeMinimize(SenderJitter[0], 2);
					}
				}
			}
			
		}
	}
	private void Cost1(IntVar[][] Topen, IntVar[][] Tclose, IntVar[][] Paff, IntVar[] ReciverJitter) {
		for (Stream stream : Current.streams) {
			String lastswitch = stream.getLastSwitch();
			int lastIndex = FindPortIndex(lastswitch, stream.Id);
			if(lastIndex != -1) {
				for (int i = 0; i < stream.N_instances; i++) {
					for (int j = 0; j < stream.N_instances; j++) {
						IntExpr aExpr = solver.makeAbs(solver.makeDifference((j * stream.Period), Tclose[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][j]]));
						IntExpr bExpr = solver.makeAbs(solver.makeDifference((i * stream.Period), Topen[lastIndex][getPortObject(lastswitch, stream.Id).indexMap[getStreamIndex(lastswitch, stream.Id)][i]]));
						IntExpr cExpr = solver.makeAbs(solver.makeDifference(aExpr, bExpr));
						IntExpr dExpr = solver.makeAbs(solver.makeDifference(stream.Transmit_Time, cExpr));
						ReciverJitter[1] = dExpr.var();
						solver.makeMinimize(ReciverJitter[1], 1);
					}
				}
			}
			
		}
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
