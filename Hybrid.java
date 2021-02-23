package TSN;
import java.util.*;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;


public class Hybrid extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;
	OptimizeVar OptVar;
	int NOutports;
	int TotalVars;
	IntVar[] Costs;
	OptimizeVar costVar;
	int TotalRuns = 0;
	int[][] Wperiod;
	long LenScale = (long) 10000;
	Random rand = new Random();

	
	
	public Hybrid(Solver _solver) {
		solver = _solver;
        TotalRuns = 0;
	}
	
	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}
	public void addConstraints() {	
	}
	public void addCosts() {
	}
	public void addDecision() {
	}
	public void addSolverLimits() {
	}
	public Solution cloneSolution() {
		if(TotalRuns != 0) {
			getNeighbour(Wperiod);
		}
		Solution Opt = RUN();
		while (Opt == null) {
			getNeighbour(Wperiod);
			Opt = RUN();
		}
		return Opt;
	}

	public void setInit(Solution init)
	{
		Current = init;
	}
	public void initVariables() {
		NOutports = Current.getNOutPorts();
		Wperiod = new int[NOutports][];
		initialsolution(Wperiod);
	}
	public void initialsolution(int[][] wperiod) {

		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NusedQ = port.getUsedQ();
					wperiod[portcounter] = new int[NusedQ];
					int UsedQCounter = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							wperiod[portcounter][UsedQCounter] = (port.getHPeriod()) / sw.microtick;
							UsedQCounter++;
						}
					}

		
					portcounter++;
				}
			}
		}
	}
	public void getNeighbour(int[][] wperiod) {

		int ChosenPortCounter = rand.nextInt(NOutports);
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					if(portcounter == ChosenPortCounter) {
						int NusedQ = port.getUsedQ();
						int ChosenUsedQcounter = rand.nextInt(NusedQ);
						int ChosenPeriod = rand.nextInt(Current.Hyperperiod) + 1 ;
						while(((Current.Hyperperiod % ChosenPeriod) != 0) || (ChosenPeriod == 0)) {
							ChosenPeriod = rand.nextInt(Current.Hyperperiod) + 1;
						}

						wperiod[ChosenPortCounter][ChosenUsedQcounter] = ChosenPeriod / sw.microtick;

						return;
					}
					portcounter++;
				}
			}
		}
	}
	
	public Solution RUN() {
		
		Solver solverFraction = new Solver("Fraction");
		SolutionMethod method = new HybridFraction(solverFraction);
	    method.Initialize(AssignSolution(Wperiod));	
		method.addConstraints();	
		method.addCosts();
		method.addDecision();		
		method.addSolverLimits();
		
		
	    Solution bestSoFar = null;
	    long start=(System.currentTimeMillis());
	    while (solverFraction.nextSolution()) { 
	    	bestSoFar = method.cloneSolution();
	    	long now= ( System.currentTimeMillis() );
		    long Tnow = now - start;
	    	System.out.println("Found, " +  Tnow);
		    
	    	if(method.Monitor(start)) {
	    		break;
	    	}

	    }
	    solverFraction.endSearch();
		return bestSoFar;
	}
	public Que getQueofPort(Port p, int priority) {
		for (Que q : p.ques) {
			if(q.Priority == priority) {
				return q;
			}
		}
		return null;
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
	

	public int getSolutionNumber() {
		return TotalRuns;
	}
	

	public boolean Monitor(long started) {
		TotalRuns++;
		long duration = System.currentTimeMillis() - started;
    	System.out.println("Solution Found!!, in Time: " + duration);    	
		if(TotalRuns >= 5){
			return true;
			//return false;
		}else {
			return false;

		}

	}
	private Solution AssignSolution(int[][] wperiod)  {
		
		Solution _Current = Current.Clone();
		_Current.costValues.clear();
		_Current.Variables = TotalVars;
		int portcounter = 0;
		for (Switches sw : _Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int Hyperperiod = GetPortHyperperiod(wperiod[portcounter]);
					port.SetGCLs(GetPortGCLSize(wperiod[portcounter], Hyperperiod));
					port.setPeriod(Hyperperiod);
					int NUsedQ = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							q.setPeriod( wperiod[portcounter][NUsedQ]);
							NUsedQ++;
						}
					}

					portcounter++;
				}			
				
			}
		}
	return _Current;
	}
	private int GetPortHyperperiod(int[] portPeriod) {
		int hyperperiod = 1;
		for (int i = 0; i < portPeriod.length; i++) {
			int tempperiod = portPeriod[i];
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
	private int GetPortGCLSize(int[] portPeriod, int hyper) {
		int GCLSize = 0;
		for (int i = 0; i < portPeriod.length; i++) {
			int crr_P =  portPeriod[i];
			GCLSize += hyper / crr_P;
		}
		return GCLSize;
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
