package TSN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

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
	public static int Best_so_Far;
	public static int Best_Cost;
	DataUnloader dataUnloader = new DataUnloader();
	DataLoader dataLoader = new DataLoader();
	Runtime runtime = Runtime.getRuntime();
	String toolname = "TSNNetCal.exe";
	String inputPath = "usecases/NetCal";
	//String inputPath = "usecases\\IEEE Access\\_TC1 - random open windows (change overlapped situations)\\1-1";
	String runcommand = toolname + " " + inputPath;

	public ExternalAssessment(Solver s,  IntVar[] _Costs, IntVar[][] _wperiod, IntVar[][] _wlength, IntVar[][] _woffset , Solution _current) {
		super(s);
		solver = s;
		Current = _current;
		Pre_Cost1 = Integer.MAX_VALUE;
		Pre_Cost2 = Integer.MAX_VALUE;
		Best_so_Far = Integer.MAX_VALUE;
		Best_Cost = Integer.MAX_VALUE;
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
		boolean flag = false;
		makeSolution();
		NETCALCall(Current);
		NETCALLRun(Current);
		if((Pre_Cost2 < Best_so_Far)) {
			flag = true;
			Best_so_Far = Pre_Cost2;
		}else if (Pre_Cost2 == 0) {
			int cost = (int) Costs[0].value();
			if (Best_Cost > cost) {
				flag = true;
				Best_Cost = cost;
			}
		}
		return flag && super.acceptSolution();
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

    public void NETCALCall(Solution s) {
		String luxiToolPath = "usecases/NetCal/in/";
		UnloadLuxi(s, luxiToolPath);
    }
    
    public void NETCALLRun(Solution s) {
		int averagee2e = 0;
		int fails = 0;
        try
        {
        	Runtime runtime = Runtime.getRuntime();
        	String toolname = "TSNNetCal.exe";
        	String inputPath = "usecases/NetCal";
        	String runcommand = toolname + " " + inputPath;
        	Process process = runtime.exec(runcommand);
            process.waitFor();
            process.destroy(); 
            DataLoader dataLoader = new DataLoader();;
			HashMap<Integer, Integer> delays = dataLoader.LoadLuxiReport(inputPath);

			boolean isGood = true;
			
			for(Integer entry : delays.keySet()) {
				int current_delay = delays.get(entry);
				if(current_delay == -1) {
					isGood = false;
				}else {
					averagee2e += current_delay;
					for (Stream stream : s.streams) {
						if(stream.Id == entry) {
							if(stream.Deadline < current_delay) {
								fails++;
							}
						}
						
					}
					
				}
			}
			if(delays.size() > 0) averagee2e /= delays.size();
			if(!isGood) averagee2e = Integer.MAX_VALUE;
			

                  	
        }
        catch (IOException | InterruptedException e)
        {
            //e.printStackTrace();
        }
        
        Pre_Cost1 = averagee2e;
        Pre_Cost2 = fails;
    }
    
    private void UnloadLuxi(Solution solution, String DirPath){
    	try {
    		int linecounter = 0;
    		Files.createDirectories(Paths.get(DirPath));
    		PrintWriter writer = new PrintWriter(DirPath + "/historySCHED1.txt", "UTF-8");
    		for (Switches sw : solution.SW) {
				for (Port port : sw.ports) {
					if(port.outPort) {
						String routeLink = sw.Name + "," + port.connectedTo;
						if(linecounter != 0) {
							writer.println();
						}
						writer.println(routeLink);
						linecounter++;
						for (int i = 0; i < port.Tclose.length; i++) {
							String frame = String.valueOf(port.Topen[i]) + "\t" + String.valueOf(port.Tclose[i]) + "\t" + String.valueOf(port.Period) + "\t" + String.valueOf(port.affiliatedQue[i]);
							writer.println(frame);
						}
					}
				}
			}
    		writer.print("#");
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
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
