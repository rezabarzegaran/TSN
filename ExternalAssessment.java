package TSN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.SearchMonitor;
import com.google.ortools.constraintsolver.Solver;

public class ExternalAssessment extends SearchMonitor{
	
	
	IntVar[] Costs;
	IntVar[][] Wperiod;
	IntVar[][] Wlength;
	IntVar[][] Woffset;
	Solution Current;
	int External1Index;
	Solver solver;
	public static int Pre_Cost1;
	public static int Pre_Cost2;
	public static int Total_Cost;
	boolean enable_NoOverlap = false;
	DataUnloader dataUnloader = new DataUnloader();
	Runtime runtime = Runtime.getRuntime();
	String toolname = "TSNNetCal.exe";
	String inputPath = "NetCal";
	//String inputPath = "usecases\\IEEE Access\\_TC1 - random open windows (change overlapped situations)\\1-1";
	String runcommand = toolname + " " + inputPath;

	public ExternalAssessment(Solver s,  IntVar[] _Costs, IntVar[][] _wperiod, IntVar[][] _wlength, IntVar[][] _woffset , Solution _current) {
		super(s);
		solver = s;
		Current = _current;
		Pre_Cost1 = Integer.MAX_VALUE;
		Pre_Cost2 = Integer.MAX_VALUE;
		Total_Cost = Integer.MAX_VALUE;
		Wperiod = new IntVar[_woffset.length][];
		Wlength = new IntVar[_woffset.length][];
		Woffset = new IntVar[_woffset.length][];
		Costs = new IntVar[_Costs.length];
		
		
		for (int i = 0; i < _Costs.length; i++) {
			Costs[i] = _Costs[i];
		}
		
		
		for (int i = 0; i < _woffset.length; i++) {
			int insideLen = _woffset[i].length;
			Wperiod[i] = new IntVar[insideLen];
			Wlength[i] = new IntVar[insideLen];
			Woffset[i] = new IntVar[insideLen];
			for (int j = 0; j < _woffset[i].length; j++) {
				Wperiod[i][j] = _wperiod[i][j];
				Wlength[i][j] = _wlength[i][j];
				Woffset[i][j] = _woffset[i][j];
			}
		}
		
	}
	
	public boolean acceptSolution() {
	
		boolean flag = true;
		if (enable_NoOverlap) {
			flag = NoOverlappingWindows();
		}						
		if(flag) {
			int COST1 = (int) Costs[0].value();
			int COST2 = GetWCLatency();
			flag = false;
			int COST3 = COST1 + COST2;
			if(COST3 < Total_Cost) {
				Pre_Cost1 = COST1;
				Pre_Cost2 = COST2;
				Total_Cost = COST3;

				flag = true;
			}
		}
		return flag && super.acceptSolution();
	}
	
	public int getExternalCost1() {
		return getWindowPercentage();
	}
	public int getExternalCost2() {
		return getLatencyCost();
	}
	public int getExternalCost3() {
		return getTotalCost();
	}
	private int getLatencyCost() {
		return Pre_Cost2;
	}
	private int getWindowPercentage() {
		return Pre_Cost1;
	}
	private int getTotalCost() {
		return Total_Cost;
	}
	private boolean NoOverlappingWindows() {		
		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int NUsedQ = port.getUsedQ();
					for (int i = 0; i < NUsedQ; i++) {
						int iperiod = (int) Wperiod[portcounter][i].value();
						int ilength = (int) Wlength[portcounter][i].value();
						int ioffset = (int) Woffset[portcounter][i].value();
						for (int j = 0; j < NUsedQ; j++) {
							int jperiod = (int) Wperiod[portcounter][j].value();
							int jlength = (int) Wlength[portcounter][j].value();
							int joffset = (int) Woffset[portcounter][j].value();
							int iInstances = LCM(iperiod, jperiod)/ iperiod;
							int jInstances = LCM(iperiod, jperiod)/ jperiod;	
							if (i != j) {		
								for (int k = 0; k < iInstances; k++) {
									for (int l = 0; l < jInstances; l++) {
										int iopen = k * iperiod + ioffset;
										int iclose = iopen + ilength;
										
										int jopen = l * jperiod + joffset;
										int jclose = jopen + jlength;
										
										
										boolean FC = (iclose >= jopen) ? true : false;
										boolean SC = (jclose >= iopen) ? true : false;
										
										boolean decisionconsition = FC ^ SC;
								
										if (!decisionconsition) {
											return false;
										}
										
									}
								}


							}
						}
					}

		
					portcounter++;
				}
			}
		}
		
		return true;
	}
	private int GetWCLatency(){
		makeSolution();
		
		dataUnloader.NETCALCall(Current);	
        try
        {
        	Process process = runtime.exec(runcommand); 
            String line;
            BufferedReader bri = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = bri.readLine()) != null) {
              System.out.println(line);
            }
            bri.close();
            //process.waitFor();
            //System.out.println("Done.");
            process.destroy();       	
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }	
		return 100;
	}
	
	private void makeSolution() {

		int portcounter = 0;
		for (Switches sw : Current.SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					int gclcounter = 0;
					int Hyperperiod = GetPortHyperperiod(Wperiod[portcounter]);

					port.SetGCLs(GetPortGCLSize(Wperiod[portcounter], Hyperperiod));
					int NUsedQ = 0;
					for (Que q : port.ques) {
						if(q.isUsed()) {
							int WW_period = (int) Wperiod[portcounter][NUsedQ].value() * sw.microtick;
							int N_instances = Hyperperiod / WW_period;
							for (int j = 0; j < N_instances; j++) {
								port.Topen[gclcounter] = WW_period * j + (int) Woffset[portcounter][NUsedQ].value() * sw.microtick;
								port.Tclose[gclcounter] = port.Topen[gclcounter] + (int) Wlength[portcounter][NUsedQ].value() * sw.microtick;
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
	private int GetPortGCLSize(IntVar[] portPeriod, int hyper) {
		int GCLSize = 0;
		for (int i = 0; i < portPeriod.length; i++) {
			int crr_P = (int) portPeriod[i].value();
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
