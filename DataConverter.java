package TSN;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class DataConverter {
	
	//Covertes the test cases from Niklas to .xml File
	Map<String, String> map = new HashMap<String, String>();
	
	//Constructor
	public DataConverter() {
		
	}
	//Convert txt Files containing streams and pathes to xml File
	public void txt2xml(String StraminputPath, String RouteinputPath, String outputPath) {
		try {
            FileReader fileReader = new FileReader(StraminputPath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            Element root = doc.createElement("Network");
            doc.appendChild(root);
        	Element messages = doc.createElement("Messages");
        	root.appendChild(messages);
            
            String line = null;
            int counter = 1;
            while((line = bufferedReader.readLine()) != null) {
            	
            	if(!line.contains("#")) {
					Element m = doc.createElement("Message");
					messages.appendChild(m);
					
					Attr idAttr = doc.createAttribute("id");
					idAttr.setValue(String.valueOf(counter));
					m.setAttributeNode(idAttr);
					
					
					String[] parts = line.split(", ");
					
					Attr sizeAttr = doc.createAttribute("size");
					sizeAttr.setValue(parts[1]);
					m.setAttributeNode(sizeAttr);
					
					Attr deadlineAttr = doc.createAttribute("deadline");
					int deadline = (int) (Double.parseDouble(parts[2]) / 100);
					
					deadlineAttr.setValue(String.valueOf(deadline));
					m.setAttributeNode(deadlineAttr);
					
					Attr periodAttr = doc.createAttribute("period");
					periodAttr.setValue(parts[6]);
					m.setAttributeNode(periodAttr);
					
					

					
					Attr prioAttr = doc.createAttribute("priority");
					prioAttr.setValue(parts[5]);
					m.setAttributeNode(prioAttr);
					
					
					Attr offsetAttr = doc.createAttribute("offset");
					offsetAttr.setValue(String.valueOf(0));
					m.setAttributeNode(offsetAttr);
					
					map.put(parts[3], String.valueOf(counter));
					System.out.println(map.get(parts[3]));
		
            		counter++;
            	}              
            }
            bufferedReader.close();
            
            FileReader fileReader2 = new FileReader(RouteinputPath);
            BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
        	Element routes = doc.createElement("Routing");
        	root.appendChild(routes);           
            
            String l = null;
            int c = 1;
            while((l = bufferedReader2.readLine()) != null) {
            	
            	if(!l.contains("#")) {
					Element r = doc.createElement("Route");
					routes.appendChild(r);
						
					Attr idAttr = doc.createAttribute("id");
					idAttr.setValue(String.valueOf(c));
					r.setAttributeNode(idAttr);
					
					
					String[] firstparts = l.split(" : ");
					String[] secondparts = firstparts[1].split(" ; ");
					
					Element nodes = doc.createElement("Nodes");
					r.appendChild(nodes);
					
					for (int i = 0; i < secondparts.length; i++) {
						String[] thirdparts = secondparts[i].split(",");
			            Element node = doc.createElement("node");
			            node.appendChild(doc.createTextNode(thirdparts[0]));
			            nodes.appendChild(node);
			            if(i == (secondparts.length - 1)) {
			            	String[] fourthparts = thirdparts[1].split(" ");
				            Element node2 = doc.createElement("node");
				            node2.appendChild(doc.createTextNode(fourthparts[0]));
				            nodes.appendChild(node2);
			            }
					}
					
					Element ms = doc.createElement("messages");
					r.appendChild(ms);
					
		            Element mId = doc.createElement("messageID");
		            mId.appendChild(doc.createTextNode(map.get(firstparts[0])));
		            ms.appendChild(mId);
					
					c++;
					
            	}      	
            
            }
             
            bufferedReader2.close();
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File(outputPath));
            transformer.transform(domSource, streamResult);
            
			
		} catch(Exception ex) {

                ex.printStackTrace();
		}
            
	}
}
