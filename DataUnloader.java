package TSN;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;

import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.w3c.dom.Attr;

import org.w3c.dom.Node;
import org.w3c.dom.Element;


public class DataUnloader {

    public DataUnloader(){

    }
    public void UnloadAll(List<Solution> solutions, String name) {
    	int counter = 1;
    	for (Solution solution : solutions) {
    		String pathString = "streams/"+ name;
    		String fileNameStream = "S_" + counter + ".xml";
			UnloadStreams(solution, pathString, fileNameStream);
			counter++;
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
							Element t_open = doc.createElement("T_open");
							port.appendChild(t_open);
							Attr time = doc.createAttribute("T");
							time.setValue(String.valueOf(portObj.Topen[portObj.indexMap[stramIndex][i]]));
							t_open.setAttributeNode(time);
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