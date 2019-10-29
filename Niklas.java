package TSN;
import java.util.*;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntExpr;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Niklas extends SolutionMethod {
	
	Solution Current;
	Solver solver;
	DecisionBuilder db;

	HashMap<Port, IntVar> P;
	IntVar[] Costs;

	OptimizeVar OptVar;
	int TotalRuns = 0;
	int NOutports;
	int TotalVars;
	
	
	
	public Niklas(Solver _solver) {
		solver = _solver;
        TotalRuns = 0;
	}

	public void Initialize(Solution current) {
		setInit(current);
		initVariables();
	}

	private class NiklasPort
	{
		private Port rezaPort;

		private SortedMap<Integer, NiklasQueue> niklasPortQueues;

		public NiklasPort(Port rezaPort)
		{
			this.rezaPort = rezaPort;
			niklasPortQueues = new TreeMap<>();
		}

		public Port getRezaPort()
		{
			int i = 0;
			for (Map.Entry<Integer, NiklasQueue> entry : niklasPortQueues.entrySet()) {
				NiklasQueue nq = entry.getValue();
				rezaPort.Topen[i] = nq.wO;
				rezaPort.Tclose[i] = nq.wO + nq.wL;
				//TODO: Set period

				i++;
			}
			return this.rezaPort;
		}

		public void createInitialGCL()
		{
			// Notation & Algorithm inspired by Algorithm 2 & 3 from "Flexible Scheduling for the
			//Time-Triggered Traffic in
			//Time-Sensitive Networking" (Niklas Reusch)


			// queue sending time + guard band
			for (Que q : rezaPort.ques)
			{
				if (q.assignedStreams.size() > 0)
				{
					NiklasQueue newNQ = new NiklasQueue();

					for (Stream s : q.assignedStreams)
					{
						int sT = s.Period; // period
						int st = s.Transmit_Time; // transmit time
						double spp = (double) st / sT; // Period percentage

						// window period percentage
						newNQ.pp += spp;

						// window Sending time
						newNQ.st += st;

						// window guard band
						if(st > newNQ.gb)
						{
							newNQ.gb = st;
						}
					}

					niklasPortQueues.put(q.id, newNQ);
				}
			}



			// maxPortPeriod = max period of all queues
			// offset = 0
			int maxPortPeriod = 0;
			int currentOffset = 0;

			// For each queue: window length = sending time of all attached streams + max sending time; window period = sending time of all attached streams / queue period percentage
			for (NiklasQueue nq : niklasPortQueues.values())
			{
				nq.wL = nq.st + nq.gb;
				nq.wT = Math.round(nq.st / nq.pp);

				if (nq.wT > maxPortPeriod)
				{
					maxPortPeriod = nq.wT;
				}
			}

			for (Map.Entry<Integer, NiklasQueue> entry : niklasPortQueues.entrySet())
			{
				NiklasQueue nq = entry.getValue();

				nq.wO = currentOffset;
				float scaleFactor = maxPortPeriod / nq.wT;
				nq.wL = (int) Math.ceil(nq.st * scaleFactor + nq.gb);
				nq.wL = Math.min(nq.wL, maxPortPeriod-currentOffset);

				nq.wT = maxPortPeriod;
				currentOffset += nq.wL;
			}
		}

		private class NiklasQueue
		{
			public int st;
			public int gb = 0;
			public float pp;
			public int wL; // window length
			public int wT; // window period
			public int wO; // window offset
		}
	}

	public void setInit(Solution init)
	{
		// Generate initial GCL
		for (Switches sw : init.SW) {
			for (Port port : sw.ports) {
				if (port.outPort) {
					NiklasPort np = new NiklasPort(port);
					np.createInitialGCL();
					port = np.getRezaPort();

				}
			}
		}

		Current = init;
	}


	public void initVariables() {
		NOutports = Current.getNOutPorts();
		P = new HashMap<Port, IntVar>();
		Costs = new IntVar[3];
		AssignVars(P);
	}

	public void addConstraints() {
	}

	public void addCosts() {

	}
	public void addDecision() {

	}
	public void addSolverLimits() {

	}
	public DecisionBuilder getDecision() {
		return db;
	}
	public Solution cloneSolution() {
		return Current;
	}
	private int AssignVars(HashMap<Port, IntVar> P) {
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if (port.outPort) {
					//TODO: use port period
					P.put(port, solver.makeIntVar(port.Tclose[port.Tclose.length-1], 120, "P_" + port.toString()));
				}
			}
		}
		return 0;
	}



}
