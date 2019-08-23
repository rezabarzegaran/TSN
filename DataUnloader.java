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


public class DataUnloader {
	

	boolean CreateLuxiOutput;
	String defaltDirPath;
	List<Integer> costValues;
	int hyperperiod = 0;
    DataVisualizer visualizer = new DataVisualizer();
    public DataUnloader(){
    	CreateLuxiOutput = true;
    	costValues = new ArrayList<Integer>();
    }
    public void UnloadAll(List<Solution> solutions, String name) {
    	int counter = 1;
    	for (Solution solution : solutions) {
    		String pathStringStreams = "streams/"+ name;
    		String pathStringSwitches = "switchs/"+ name;
    		String fileNameStream = "S_" + counter + ".xml";
			UnloadStreams(solution, pathStringStreams, fileNameStream);
			UnloadPorts(solution, pathStringSwitches, fileNameStream);
			counter++;
		}
    	if(CreateLuxiOutput) {
    		String pathStringStreams = "luxiTool/"+ name;
    		UnloadLuxi(solutions.get(solutions.size() - 1), pathStringStreams);
    	}
    	
    }
    public void UnloadOnce(Solution solution, String name, int counter) {
		String pathStringStreams = "streams/"+ name;
		String pathStringJitters = "jitters/"+ name;
		String pathStringSwitches = "switchs/"+ name;
		String fileNameStream = "S_" + counter + ".xml";
		hyperperiod = solution.Hyperperiod;
		UnloadStreams(solution, pathStringStreams, fileNameStream);
		UnloadJitterStreams(solution, pathStringJitters, fileNameStream);
		UnloadPorts(solution, pathStringSwitches, fileNameStream);
	    visualizer.CreateTotalSVG(solution, "visual/"+ name + "/Solution " + counter, solution.Hyperperiod, false);
    	costValues.add(solution.getCost());
    	if(CreateLuxiOutput) {
    		String pathStringStreamsLuxi = "luxiTool/"+ name;
    		UnloadLuxi(solution, pathStringStreamsLuxi);
    	}
    	
    }
    public void UnloadLuxi(Solution solution, String DirPath){
    	try {
    		Files.createDirectories(Paths.get(DirPath));
    		PrintWriter writer = new PrintWriter(DirPath + "/historySCHED1.txt", "UTF-8");
    		String hyperperiod = String.valueOf(solution.Hyperperiod);
    		for (Switches sw : solution.SW) {
				for (Port port : sw.ports) {
					if(port.outPort) {
						String routeLink = sw.Name + "," + port.connectedTo;
						writer.println();
						writer.println(routeLink);
						for (int i = 0; i < port.Tclose.length; i++) {
							String frame = String.valueOf(port.Topen[i]) + "\t" + String.valueOf(port.Tclose[i]) + "\t" + hyperperiod + "\t" + String.valueOf(port.affiliatedQue[i]);
							writer.println(frame);
						}
					}
				}
			}
    		
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    	
    	
    }
    
    public void UnloadPorts(Solution solution, String DirPath, String fName) {
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
    public void CreateReport(int dur) {
    	try {
    		PrintWriter writer = new PrintWriter("report.txt", "UTF-8");
    		String lineString = "Optimization Finished";
    		writer.println(lineString);
    		lineString = "The hyperPeriod is: " + hyperperiod;
    		writer.println(lineString);
    		lineString = "There are " + costValues.size() + " solutions, whithin " + dur + " seconds.";
    		writer.println(lineString);
    		
    		lineString = "The cost values are: ";
    		writer.println(lineString);
    		
    		for (int i = 0; i < costValues.size(); i++) {
        		lineString = "Solution " + i + ":\t" + costValues.get(i);
        		writer.println(lineString);
			}
    		

    		
    		writer.close();
    	} catch (Exception e){
            e.printStackTrace();
        }
    }
    public void UnloadStreams(Solution solution, String DirPath, String fName){
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
    public void UnloadJitterStreams(Solution solution, String DirPath, String fName){
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