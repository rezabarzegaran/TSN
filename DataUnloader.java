package TSN;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;

import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.NameList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




class DataUnloader {
	
	boolean LuxiInterface;
	boolean JitterTimeInterface;
	boolean StreamWiseInterface;
	boolean GeneralInterface;
	boolean GenerateOMNETPP;
	String defaltDirPath = "Results";
	List<List<Integer>> costValues;
	List<Long> SolutionTimes;
	int hyperperiod = 0;
	int variables = 0;
    DataVisualizer visualizer = new DataVisualizer();
    String defaultPath = "Results";
    public DataUnloader(){
    	LuxiInterface = true;
    	JitterTimeInterface = true;
    	StreamWiseInterface = false;
    	GeneralInterface = true;
    	GenerateOMNETPP = true;
    	costValues = new ArrayList<List<Integer>>();
    	SolutionTimes = new ArrayList<Long>();
    }
    public void setDirPath(String _path) {
    	defaltDirPath = _path;
    }
    public void CaptureSolution(Solution solution, long Tnow) {
    	getCostValues(solution);
    	SolutionTimes.add(Tnow);
    	variables = solution.Variables;
    }
    public void WriteData(Solution solution, String name, int counter) {
    	//getCostValues(solution);
    	hyperperiod = solution.Hyperperiod;
		if (!defaultPath.contains(name)) {
    		defaultPath = defaultPath + "/" + name; 
    	}
		String streamPath = defaultPath + "/Streams";
		String switchPath = defaultPath + "/Switches";
		String GCLsPath = defaultPath + "/GCL";
		String schedulePath = defaultPath + "/Schedule/" + "S_" + counter;
		String LuxiToolPath = defaultPath + "/LuxiInterface/S_" + counter;
		String LuxiAssesPath = "usecases" + "/NetCal/in/historySCHED1";
		String solutionFile = "S_" + counter + ".xml";
		String jitterPath = defaultPath + "/Jitters";
		
		if(GenerateOMNETPP) {
			UnloadGCLs(solution, GCLsPath, solutionFile);
		}
		
		if(name.contains("Niklas")) {
    		//visualizer.CreateTotalWindowSVG(solution, schedulePath, solution.Hyperperiod);
    		UnloadPorts(solution, switchPath, solutionFile);
    		UnloadLuxi(solution, LuxiToolPath);
    		NETCALCall(solution);
    		NETCALLRun(solution);
    		
		}else {
			if (GeneralInterface) {
	    		visualizer.CreateTotalSVG(solution, schedulePath, solution.Hyperperiod);
	    		UnloadStreams(solution, streamPath, solutionFile);
	    		UnloadPorts(solution, switchPath, solutionFile);
			}
			if (JitterTimeInterface) {
	    		CreateJitterTimeInterface(solution, jitterPath, "S_"+counter);
	    		UnloadJitterStreams(solution, jitterPath, solutionFile);
			}
			if (LuxiInterface) {
	    		//String luxiToolPath = "Results/LuxiInterface/"+ name;
	    		UnloadLuxi(solution, LuxiToolPath);
			}
			if (StreamWiseInterface) {
	    		visualizer.CreateStreamWiseSVG(solution, schedulePath, solution.Hyperperiod);
			}
		}
		

		
    		
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
        costValues.get(costValues.size() - 1).add(averagee2e);
        costValues.get(costValues.size() - 1).add(fails);
    }
    private void getCostValues(Solution solution) {
    	List<Integer> costs = new ArrayList<Integer>();
    	for (int val : solution.getCosts()) {
			costs.add(val);
		}
    	costValues.add(costs);
    	System.out.println("Current Cost is: " + costs.get(0) + " , " + costs.get(1));
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
    private void UnloadOMNET(Solution solution, String DirPath) {
    	Unloadned(solution, DirPath);
    	Unloadini(solution, DirPath);
    	UnloadFlows(solution, DirPath);
    	UnloadRouting(solution, DirPath);
    }
    private void Unloadini(Solution solution, String DirPath) {
    	try {
    		int linecounter = 0;
    		Files.createDirectories(Paths.get(DirPath));
    		PrintWriter writer = new PrintWriter(DirPath + "/Solution.ini", "UTF-8");
			writer.println("[General]");
			writer.println("network = TC");
			writer.println("record-eventlog = true");
			writer.println("debug-on-errors = true");
			writer.println("result-dir = results-tc");
			writer.println("sim-time-limit = " + solution.Hyperperiod + "us");
			writer.println("**.displayAddresses = true");
			writer.println("**.verbose = true");
			int escounter = 1;
			for (EndSystems es : solution.ES) {
				if(escounter < 10) {
					writer.println("**." + es.Name + ".eth.address = \"00-00-00-00-00-0" + escounter + "\"");

				}else if(escounter <100) {
					writer.println("**." + es.Name + ".eth.address = \"00-00-00-00-00-" + escounter + "\"");

				}else if(escounter < 110) {
					writer.println("**." + es.Name + ".eth.address = \"00-00-00-00-" + (int) (escounter/100) + "-" + (escounter % 100) + "\"");

				}
				escounter++;

			}
			writer.println("**.SW*.processingDelay.delay = 0us");
			writer.println("**.filteringDatabase.database = xmldoc(\"xml/Routing.xml\", \"/filteringDatabases/\")");
			
			for (Switches sw : solution.SW) {
				int portcounter = 0;
				for (Port port : sw.ports) {
					if(port.outPort) {
						writer.println("**." + sw.Name+ ".eth[" + portcounter + "].queue.gateController.initialSchedule = xmldoc(\"xml/S.xml\", \"/schedules/switch[@name='" + sw.Name + "']/port[@id='" + portcounter + "']/schedule\") ");
						portcounter++;
					}

				}
			}
			
			writer.println("**.SW*.eth[*].queue.numberOfQueues = 8");
			writer.println("**.SW*.eth[*].queue.tsAlgorithms[*].typename = \"StrictPriority\"");
			writer.println("**.SW*.eth[*].mac.enablePreemptingFrames = false");
			writer.println("**.ES*.trafGenSchedApp.initialSchedule = xmldoc(\"xml/Flows.xml\")");


    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    }
	private void Unloadned(Solution solution, String DirPath) {
    	try {
    		int linecounter = 0;
    		Files.createDirectories(Paths.get(DirPath));
    		PrintWriter writer = new PrintWriter(DirPath + "/Solution.ned", "UTF-8");
			writer.println("package nesting.simulations.examples;");
			writer.println("import ned.DatarateChannel;");
			writer.println("import nesting.node.ethernet.VlanEtherHostQ;");
			writer.println("import nesting.node.ethernet.VlanEtherHostSched;");
			writer.println("import nesting.node.ethernet.VlanEtherSwitchPreemptable;");
			writer.println("network TC");
			writer.println("{");
			writer.println("types:");
			writer.println("channel C extends DatarateChannel");
			writer.println("{");
			writer.println("delay = 0us;");
			writer.println("datarate = 100Mbps;");
			writer.println("}");
			writer.println("submodules:");
			for (Switches sw : solution.SW) {
				writer.println(sw.Name+": VlanEtherSwitchPreemptable {");
				writer.println("gates:");
				writer.println("ethg["+sw.ports.size()+"];");
				writer.println("}");
			}
			for (EndSystems es : solution.ES) {
				writer.println(es.Name+": VlanEtherHostSched {");
				writer.println("}");
			}
			writer.println("connections:");
			for (Switches sw : solution.SW) {
				int portcounter=0;
				for (Port port : sw.ports) {
					if(port.outPort) {
						if(port.connectedToES) {
							writer.println(sw.Name + ".ethg[" + portcounter + "] <--> C <--> " + port.connectedTo + ".ethg;");
						}else {	
				    		Optional<Switches> tempsw = solution.SW.stream().filter(x -> x.Name.equals(port.connectedTo)).findFirst();
				    		if (tempsw.isPresent()) {
				    				Switches Csw = tempsw.get();
				    				if(solution.SW.indexOf(Csw) > solution.SW.indexOf(sw)) {
							    		Optional<Port> tempportout = Csw.ports.stream().filter(x -> (x.connectedTo.equals(sw.Name) && x.outPort)).findFirst();
							    		Optional<Port> tempportin = Csw.ports.stream().filter(x -> (x.connectedTo.equals(sw.Name) && !x.outPort)).findFirst();
							    		
							    		if(tempportout.isPresent()) {
							    			Port portout = tempportout.get();
											writer.println(sw.Name + ".ethg[" + portcounter + "] <--> C <--> " + port.connectedTo + ".ethg[" + Csw.getPortIndex(portout) + "];");
							    		}else {
							    			if(tempportin.isPresent()) {
							    				Port portin = tempportin.get();
												writer.println(sw.Name + ".ethg[" + portcounter + "] <--> C <--> " + port.connectedTo + ".ethg[" + Csw.getPortIndex(portin) + "];");

							    			}
							    		}
				    				}

				    				
				    			}

				    	}
					portcounter++;
					}

				}
				for (Port port : sw.ports) {
					if(!port.outPort) {
						
						if(port.connectedToES) {
				    		Optional<Port> tempport = sw.ports.stream().filter(x -> (x.connectedTo.equals(port.connectedTo) && x.outPort)).findFirst();
				    		if(!tempport.isPresent()) {
								writer.println(sw.Name + ".ethg[" + portcounter + "] <--> C <--> " + port.connectedTo + ".ethg ;");
								portcounter++;	
				    		}
							
						}else {
				    		Optional<Port> tempport = sw.ports.stream().filter(x -> (x.connectedTo.equals(port.connectedTo) && x.outPort)).findFirst();
				    		if(!tempport.isPresent()) {
					    		Optional<Switches> tempsw = solution.SW.stream().filter(x -> (x.Name.equals(port.connectedTo))).findFirst();
					    		if(tempsw.isPresent()) {
					    			Switches Csw = tempsw.get();
					    			int ccounter = 0;
					    			for (Port cport : Csw.ports) {
										if(cport.outPort) {
											writer.println(sw.Name + ".ethg[" + portcounter + "] <--> C <--> " + port.connectedTo + ".ethg[" + ccounter + "];");
											ccounter++;
											portcounter++;	
										}
									}
					    		}
				    		}
									
						}
						
						
						
	



					}
				}
			}
				
			writer.println("}");
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private void UnloadFlows(Solution solution, String DirPath) {
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            String period = Integer.toString(solution.Hyperperiod) + "us";
            
            Element root = doc.createElement("schedules");
            doc.appendChild(root);
            
            Element defaultcycle = doc.createElement("defaultcycle");
            defaultcycle.setTextContent(period);
            root.appendChild(defaultcycle);
            for (EndSystems es : solution.ES) {
            	Element host = doc.createElement("host");
            	root.appendChild(host);
            	Attr hostname = doc.createAttribute("name");
            	hostname.setValue(es.Name);
            	host.setAttributeNode(hostname);
            	Element cycle = doc.createElement("cycle");
            	cycle.setTextContent(period);
            	host.appendChild(cycle);
            	
            	int[][] times;
            	
            	int arraysize = 0;
            	for (int flowid : es.outStreamsIDs) {
					for (Stream s : solution.streams) {
						if(s.Id == flowid) {
							arraysize += s.N_instances;
						}
					}
				}
            	times = new int[arraysize][2];
            	
            	int arraycounter = 0;
            	for (int flowid : es.outStreamsIDs) {
					for (Stream s : solution.streams) {
						if(s.Id == flowid) {
							for (int i = 0; i < s.N_instances; i++) {
								times[arraycounter][0] = i * s.Period;
								times[arraycounter][1] = s.Id;
								arraycounter++;
							}
						}
					}
				}
            	
            	Arrays.sort(times, (a,b) -> Integer.compare(a[0],b[0]));

            	
            	
     
            	for (int i = 0; i < arraysize; i++) {
					for (Stream s : solution.streams) {
						if(s.Id == times[i][1]) {
							Element entry = doc.createElement("entry");
							host.appendChild(entry);
				            Element entryflowid = doc.createElement("flowId");
				            entryflowid.setTextContent(Integer.toString(s.Id));
				            entry.appendChild(entryflowid);
				            
				            Element entrystart = doc.createElement("start");
				            String starttime = Integer.toString(times[i][0]) + "us";
				            entrystart.setTextContent(starttime);
				            entry.appendChild(entrystart);
				            
				            Element entryqueue = doc.createElement("queue");
				            entryqueue.setTextContent(Integer.toString(s.Priority));
				            entry.appendChild(entryqueue);
				            
				            Element entrydest = doc.createElement("dest");
				            String listener = s.CroutingList.get(s.CroutingList.size() - 1);
				            int counter = 1;
				            for (EndSystems tempes : solution.ES) {
								if(tempes.Name.equals(listener)) {
									break;
								}
								counter++;
							}
				            
				            String listenerAdd = null;
							if(counter < 10) {
								listenerAdd = "00:00:00:00:00:0" + Integer.toString(counter);

							}else if(counter <100) {
								listenerAdd = "00:00:00:00:00-" + Integer.toString(counter);

							}else if(counter < 110) {
								listenerAdd = "00:00:00:00:" + Integer.toString((int)(counter/100)) + ":" + Integer.toString((counter%100));

							}
							
				            entrydest.setTextContent(listenerAdd);
				            entry.appendChild(entrydest);
				            
				            Element entrysize = doc.createElement("size");
				            entrysize.setTextContent(Integer.toString(s.Size)+"B");
				            entry.appendChild(entrysize);
						}
					}
				}
		    		
          
			}
            	
            
               
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/Flows.xml";
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private void UnloadRouting(Solution solution, String DirPath) {
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            String period = Integer.toString(solution.Hyperperiod) + "us";
            
            Element root = doc.createElement("filteringDatabases");
            doc.appendChild(root);
            
            
            for (Switches sw : solution.SW) {
                Element database = doc.createElement("filteringDatabase");
                root.appendChild(database);
            	Attr id = doc.createAttribute("id");
            	id.setValue(sw.Name);
            	database.setAttributeNode(id);
                Element staticlabel = doc.createElement("static");
                database.appendChild(staticlabel);
                Element forward = doc.createElement("forward");
                staticlabel.appendChild(forward);
                
                int portcounter = 0;
                for (Port port : sw.ports) {
					if(port.outPort) {
		            	for (Stream s : port.AssignedStreams) {
			                Element indivitualframe = doc.createElement("individualAddress");
			                forward.appendChild(indivitualframe);
			                
			            	Attr macad = doc.createAttribute("macAddress");
				            String listener = s.CroutingList.get(s.CroutingList.size() - 1);
				            int counter = 1;
				            for (EndSystems tempes : solution.ES) {
								if(tempes.Name.equals(listener)) {
									break;
								}
								counter++;
							}
				            String listenerAdd = null;
							if(counter < 10) {
								listenerAdd = "00-00-00-00-00-0" + Integer.toString(counter);

							}else if(counter <100) {
								listenerAdd = "00-00-00-00-00-" + Integer.toString(counter);

							}else if(counter < 110) {
								listenerAdd = "00-00-00-00-" + Integer.toString((int)(counter/100)) + "-" + Integer.toString((counter%100));

							}
				            
			            	macad.setValue(listenerAdd);
			            	indivitualframe.setAttributeNode(macad);
			            	
			            	Attr portlabel = doc.createAttribute("port");
			            	portlabel.setValue(Integer.toString(portcounter));
			            	indivitualframe.setAttributeNode(portlabel);

						}
   
						portcounter++;
					}
				}
                
                
            	
			}
            

               
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/Routing.xml";
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private void UnloadGCLs(Solution solution, String DirPath, String filename) {
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            String period = Integer.toString(solution.Hyperperiod) + "us";
            
            Element root = doc.createElement("schedules");
            doc.appendChild(root);
            
            Element defaultcycle = doc.createElement("defaultcycle");
            defaultcycle.setTextContent(period);
            root.appendChild(defaultcycle);
            
            for (Switches sw : solution.SW) {
            	Element host = doc.createElement("switch");
            	root.appendChild(host);
            	Attr hostname = doc.createAttribute("name");
            	hostname.setValue(sw.Name);
            	host.setAttributeNode(hostname);
            	
            	int portcounter = 0;
            	for (Port port : sw.ports) {
					if(port.outPort) {
		            	Element portlist = doc.createElement("port");
		            	host.appendChild(portlist);
		            	Attr portid = doc.createAttribute("id");
		            	portid.setValue(Integer.toString(portcounter));
		            	portlist.setAttributeNode(portid);
		            	
		            	
		            	Element schedule = doc.createElement("schedule");
		            	portlist.appendChild(schedule);
		            	Attr schedulecycle = doc.createAttribute("cycleTime");
		            	schedulecycle.setValue(period);
		            	schedule.setAttributeNode(schedulecycle);
		            	
		            	int[][] G = new int[port.Topen.length][3];
		            	for (int i = 0; i < port.Topen.length; i++) {
							G[i][0] = port.Topen[i];
							G[i][1] = port.Tclose[i];
							G[i][2] = port.affiliatedQue[i];
						}
		            	Arrays.sort(G, (a,b) -> Integer.compare(a[0],b[0]));
		            	
		            	
		            	
		            	
		            	int prevClose = 0;
						for (int j = 0; j < (solution.Hyperperiod / port.getPeriod()); j++) {
							for (int i = 0; i < port.Topen.length; i++) {			
								int dur =  (j * port.getPeriod()) + G[i][0] - prevClose;
								if(dur<0) {
									prevClose = 0;
									dur =  (j * port.getPeriod()) + G[i][0] - prevClose;
								}
								if(dur != 0) {
					            	Element entry = doc.createElement("entry");
					            	schedule.appendChild(entry);
					            	Element entrylength = doc.createElement("length");
					            	entrylength.setTextContent(Integer.toString(dur)+"us");
					            	entry.appendChild(entrylength);
					            	
					            	Element entrybit = doc.createElement("bitvector");
					            	entrybit.setTextContent("00000000");
					            	entry.appendChild(entrybit);
								}
								dur = G[i][1] - G[i][0];
				            	Element entry = doc.createElement("entry");
				            	schedule.appendChild(entry);
				            	Element entrylength = doc.createElement("length");
				            	entrylength.setTextContent(Integer.toString(dur)+"us");
				            	entry.appendChild(entrylength);
				            	Element entrybit = doc.createElement("bitvector");
				            	
				            	
								switch (G[i][2]) {
								case 0:
									entrybit.setTextContent("00000010");
									break;
								case 1:
									entrybit.setTextContent("00000001");
									break;
								case 2:
									entrybit.setTextContent("00000100");
									break;
								case 3:
									entrybit.setTextContent("00001000");
									break;
								case 4:
									entrybit.setTextContent("00010000");
									break;
								case 5:
									entrybit.setTextContent("00100000");
									break;
								case 6:
									entrybit.setTextContent("01000000");
									break;
								case 7:
									entrybit.setTextContent("10000000");
									break;

								default:
									throw new IllegalArgumentException("Unexpected value: " + G[i][2]);
								}
								
								entry.appendChild(entrybit);

								prevClose = (j * port.getPeriod()) + G[i][1];

							}
							

						}
						int dur = solution.Hyperperiod - prevClose;
						if(dur > 0) {
			            	Element entry = doc.createElement("entry");
			            	schedule.appendChild(entry);
			            	Element entrylength = doc.createElement("length");
			            	entrylength.setTextContent(Integer.toString(dur)+"us");
			            	entry.appendChild(entrylength);
			            	
			            	Element entrybit = doc.createElement("bitvector");
			            	entrybit.setTextContent("00000000");
			            	entry.appendChild(entrybit);
						}
		            	
		            	
		            	

		            	

						portcounter++;
					}
				}

			}
           
               
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/" + filename;
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    
    
    private void UnloadPorts(Solution solution, String DirPath, String fName) {
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            Element root = doc.createElement("Switches");
            doc.appendChild(root);
            
            for (Switches s : solution.SW) {
            	Element sw = doc.createElement("Switch");
            	root.appendChild(sw);
            	Attr sWNaAttr = doc.createAttribute("Name");
            	sWNaAttr.setValue(s.Name);
				sw.setAttributeNode(sWNaAttr);
				for (Port port : s.ports) {
					if(port.outPort) {
						Element portElement = doc.createElement("Port");
						sw.appendChild(portElement);
						Attr portNameAttr = doc.createAttribute("ConnectedTo");
						portNameAttr.setValue(port.connectedTo);
						portElement.setAttributeNode(portNameAttr);
						for (int j = 0; j < (solution.Hyperperiod/port.getPeriod()); j++) {
							for (int i = 0; i < port.Topen.length; i++) {
								Element frame = doc.createElement("Frame");
								portElement.appendChild(frame);
								Attr openAttr = doc.createAttribute("Open");
								openAttr.setValue(String.valueOf((j * port.getPeriod()) + port.Topen[i]));
								frame.setAttributeNode(openAttr);
								Attr closeAttr = doc.createAttribute("Close");
								closeAttr.setValue(String.valueOf((j * port.getPeriod()) + port.Tclose[i]));
								frame.setAttributeNode(closeAttr);
								Attr queAttr = doc.createAttribute("Que");
								queAttr.setValue(String.valueOf(port.affiliatedQue[i]));
								frame.setAttributeNode(queAttr);
							}
						}


						
					}
				}
			}
            
       
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/" + fName;
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void Report(Solution solution, long dur) {
    	CreateReport(dur);
    	if(GenerateOMNETPP) {
    		String omnetpath = defaltDirPath + "/OMNET";
    		UnloadOMNET(solution, omnetpath);
    	}
    }
    public void CreateReport(long dur) {

    	try {
    		Files.createDirectories(Paths.get(defaltDirPath));
    		String Reportpath = defaltDirPath + "/report.txt";
    		PrintWriter writer = new PrintWriter(Reportpath, "UTF-8");
    		String lineString = "Optimization Finished";
    		writer.println(lineString);
    		lineString = "The hyperPeriod is: " + hyperperiod;
    		writer.println(lineString);
    		lineString = "The number of frames are: " + variables;
    		writer.println(lineString);
    		lineString = "There are " + costValues.size() + " solutions, whithin " + dur + " milli seconds.";
    		writer.println(lineString);
    		
    		lineString = "The cost values are: ";
    		writer.println(lineString);
    		
    		for (int i = 0; i < costValues.size(); i++) {
    			lineString = "Solution " + i + ":\t";
    			for (int val : costValues.get(i)) {
    				if(val == Integer.MAX_VALUE) {
    					lineString += "Invalid" + ",\t";
    				}else {
    					lineString += val + ",\t";
    				}
            		
            		
				}
    			lineString += "Generated in : "  + SolutionTimes.get(i);
    			writer.println(lineString);

			}
    		
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    }
    private void UnloadStreams(Solution solution, String DirPath, String fName){
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            Element root = doc.createElement("Streams");
            doc.appendChild(root);
            for (Stream stream : solution.streams) {
				Element s = doc.createElement("Stream");
				root.appendChild(s);
				Attr IDatrib = doc.createAttribute("ID");
				IDatrib.setValue(String.valueOf(stream.Id));
				s.setAttributeNode(IDatrib);
				for (String portName : stream.routingList) {
					Element port = doc.createElement("Port");
					port.appendChild(doc.createTextNode(portName));
					s.appendChild(port);
					Port portObj = getPortObject(solution, portName, stream.Id);
					int stramIndex = getStreamIndex(solution, portName, stream.Id);
					
					if(portObj != null) {
						for (int i = 0; i < portObj.indexMap[stramIndex].length; i++) {

							
							Element frame = doc.createElement("Frame");
							port.appendChild(frame);
							Attr openAttr = doc.createAttribute("Open");
							openAttr.setValue(String.valueOf(portObj.Topen[portObj.indexMap[stramIndex][i]]));
							frame.setAttributeNode(openAttr);
							Attr closeAttr = doc.createAttribute("Close");
							closeAttr.setValue(String.valueOf(portObj.Tclose[portObj.indexMap[stramIndex][i]]));
							frame.setAttributeNode(closeAttr);
							Attr queAttr = doc.createAttribute("Que");
							queAttr.setValue(String.valueOf(portObj.affiliatedQue[portObj.indexMap[stramIndex][i]]));
							frame.setAttributeNode(queAttr);
							
						}
					}				

				}
				
				
			}
                 
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/" + fName;
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }


    }
    private void CreateJitterTimeInterface(Solution solution, String DirPath, String solutionName) {
    	try {
            Files.createDirectories(Paths.get(DirPath));
    		String filename = DirPath + "/" + solutionName + ".txt" ;
    		PrintWriter writer = new PrintWriter(filename, "UTF-8");
    		String lineString = "M recived at PNode, M recived at ACT";
    		writer.println(lineString);
    		
    		for (App CA : solution.Apps) {
        		Optional<Stream> inStream = solution.streams.stream().filter(x -> x.Id == CA.inputMessages.get(0)).findFirst();
        		Stream inputStream = null ;
        		if (!inStream.isEmpty()) {
        			inputStream = inStream.get();
        		}
        		
        		Optional<Stream> outStream = solution.streams.stream().filter(x -> x.Id == CA.outputMessages.get(0)).findFirst();
        		Stream outputStream = null;
        		if (!outStream.isEmpty()) {
        			outputStream = outStream.get();
        		}
        		
        		if((inputStream != null) && (outputStream != null)) {
        			String inSwitchName = inputStream.getLastSwitch();
					Port InportObj = getPortObject(solution, inSwitchName, inputStream.Id);
					int InstramIndex = getStreamIndex(solution, inSwitchName, inputStream.Id);
					
        			String outSwitchName = outputStream.getLastSwitch();
					Port OutportObj = getPortObject(solution, outSwitchName, outputStream.Id);
					int OutstramIndex = getStreamIndex(solution, outSwitchName, outputStream.Id);
					
					for (int i = 0; i < inputStream.N_instances; i++) {
						String treciveAtP = String.valueOf(InportObj.Tclose[InportObj.indexMap[InstramIndex][i]]);
						String treciveAtACT = String.valueOf(OutportObj.Tclose[OutportObj.indexMap[OutstramIndex][i]]);
						lineString = treciveAtP +"," + treciveAtACT;
						writer.println(lineString);
					}
					
					
					
        		}
        		
        		
			}
    		  		
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    }
    private void UnloadJitterStreams(Solution solution, String DirPath, String fName){
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            Element root = doc.createElement("Streams");
            doc.appendChild(root);
            for (Stream stream : solution.streams) {
				Element s = doc.createElement("Stream");
				root.appendChild(s);
				Attr IDatrib = doc.createAttribute("ID");
				IDatrib.setValue(String.valueOf(stream.Id));
				s.setAttributeNode(IDatrib);
				for (String portName : stream.routingList) {
					Element port = doc.createElement("Port");
					port.appendChild(doc.createTextNode(portName));
					s.appendChild(port);
					Port portObj = getPortObject(solution, portName, stream.Id);
					int stramIndex = getStreamIndex(solution, portName, stream.Id);
					
					if(portObj != null) {
						for (int i = 0; i < portObj.indexMap[stramIndex].length; i++) {

							
							Element frame = doc.createElement("Frame");
							port.appendChild(frame);
							Attr openAttr = doc.createAttribute("Open");
							openAttr.setValue(String.valueOf(portObj.Topen[portObj.indexMap[stramIndex][i]] - i * stream.Period));
							frame.setAttributeNode(openAttr);
							Attr closeAttr = doc.createAttribute("Close");
							closeAttr.setValue(String.valueOf(portObj.Tclose[portObj.indexMap[stramIndex][i]] - i * stream.Period ));
							frame.setAttributeNode(closeAttr);
							Attr queAttr = doc.createAttribute("Dur");
							queAttr.setValue(String.valueOf(portObj.Tclose[portObj.indexMap[stramIndex][i]] - portObj.Topen[portObj.indexMap[stramIndex][i]]));
							frame.setAttributeNode(queAttr);
							
						}
					}				

				}
				
				
			}
            
            
            
            
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/" + fName;
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            

        } catch (Exception e){
            e.printStackTrace();
        }


    }
	private Port getPortObject(Solution Current, String swName, int mID) {
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
	private int getStreamIndex(Solution Current, String swName, int mID) {
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