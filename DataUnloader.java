package TSN;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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


class DataUnloader {
	
	boolean LuxiInterface;
	boolean JitterTimeInterface;
	boolean StreamWiseInterface;
	boolean GeneralInterface;
	String defaltDirPath = "Results";
	List<List<Integer>> costValues;
	List<Long> SolutionTimes;
	int hyperperiod = 0;
    DataVisualizer visualizer = new DataVisualizer();
    String defaultPath = "Results";
    public DataUnloader(){
    	LuxiInterface = true;
    	JitterTimeInterface = true;
    	StreamWiseInterface = false;
    	GeneralInterface = true;
    	costValues = new ArrayList<List<Integer>>();
    	SolutionTimes = new ArrayList<Long>();
    }
    public void setDirPath(String _path) {
    	defaltDirPath = _path;
    }
    public void CaptureSolution(Solution solution, long Tnow) {
    	getCostValues(solution);
    	SolutionTimes.add(Tnow);
    }
    public void WriteData(Solution solution, String name, int counter) {
    	//getCostValues(solution);
    	hyperperiod = solution.Hyperperiod;
		if (!defaultPath.contains(name)) {
    		defaultPath = defaultPath + "/" + name; 
    	}
		String streamPath = defaultPath + "/Streams";
		String switchPath = defaultPath + "/Switches";
		String schedulePath = defaultPath + "/Schedule/" + "S_" + counter;
		String LuxiToolPath = defaultPath + "/LuxiInterface/S_" + counter;
		String solutionFile = "S_" + counter + ".xml";
		String jitterPath = defaultPath + "/Jitters";
		
		if(name.contains("Niklas")) {
    		visualizer.CreateTotalWindowSVG(solution, schedulePath, solution.Hyperperiod);
    		UnloadPorts(solution, switchPath, solutionFile);
    		UnloadLuxi(solution, LuxiToolPath);

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
    private void getCostValues(Solution solution) {
    	List<Integer> costs = new ArrayList<Integer>();
    	for (int val : solution.getCosts()) {
			costs.add(val);
		}
    	costValues.add(costs);
    	//System.out.println("Current Cost is: " + costs.get(costs.size() - 1));
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
						for (int i = 0; i < port.Topen.length; i++) {
							Element frame = doc.createElement("Frame");
							portElement.appendChild(frame);
							Attr openAttr = doc.createAttribute("Open");
							openAttr.setValue(String.valueOf(port.Topen[i]));
							frame.setAttributeNode(openAttr);
							Attr closeAttr = doc.createAttribute("Close");
							closeAttr.setValue(String.valueOf(port.Tclose[i]));
							frame.setAttributeNode(closeAttr);
							Attr queAttr = doc.createAttribute("Que");
							queAttr.setValue(String.valueOf(port.affiliatedQue[i]));
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
    public void CreateReport(long dur) {
    	try {
    		Files.createDirectories(Paths.get(defaltDirPath));
    		String Reportpath = defaltDirPath + "/report.txt";
    		PrintWriter writer = new PrintWriter(Reportpath, "UTF-8");
    		String lineString = "Optimization Finished";
    		writer.println(lineString);
    		lineString = "The hyperPeriod is: " + hyperperiod;
    		writer.println(lineString);
    		lineString = "There are " + costValues.size() + " solutions, whithin " + dur + " milli seconds.";
    		writer.println(lineString);
    		
    		lineString = "The cost values are: ";
    		writer.println(lineString);
    		
    		for (int i = 0; i < costValues.size(); i++) {
    			lineString = "Solution " + i + ":\t";
    			for (int val : costValues.get(i)) {
            		lineString += val + ",\t";
            		
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