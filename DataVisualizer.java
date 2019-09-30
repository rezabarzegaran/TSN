package TSN;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




public class DataVisualizer {
	int Duration = 0;
	int offset = 220;
	int Toffset = 30;
	int rowH = 30;
	int maxH = 0;
	int mlt = 10;
	int co = 1;
	public DataVisualizer() {
		
	}
	public void CreateStreamWiseSVG(Solution solution, String DirPath, int duration) {
		try {
			Duration = duration * mlt;
			for (Stream stream : solution.streams) {
	            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	            Document doc = dBuilder.newDocument();
	            
	            Element svg = initIndivitual(doc, solution, stream);
	            doc.appendChild(svg);
	            
	            int counter = 0;
				for (String node : stream.routingList) {
					Port crr_Port = getPortObject(solution, node, stream.Id);
					int s_index = getStreamIndex(solution, node, stream.Id);
					for (int i = 0; i < crr_Port.indexMap[s_index].length; i++) {
						int x_1 = crr_Port.Topen[crr_Port.indexMap[s_index][i]] * mlt;
						int x_2 = crr_Port.Tclose[crr_Port.indexMap[s_index][i]] * mlt;
						int width = x_2 - x_1;
						if( (x_1 <= Duration) && (x_2 <= Duration)) {
							addFrame(doc, svg, x_1 + offset , rowH * counter + Toffset, width, rowH, String.valueOf(stream.Id), stream.Priority);
						}

					}
					counter++;
				}
	            
           	            
	            TransformerFactory transformerFactory = TransformerFactory.newInstance();
	            Transformer transformer = transformerFactory.newTransformer();
	            DOMSource domSource = new DOMSource(doc);
	            Files.createDirectories(Paths.get(DirPath));
	            String path = DirPath + "/" + "Stream_" + stream.Id + ".svg";
	            StreamResult streamResult = new StreamResult(new File(path));
	            transformer.transform(domSource, streamResult);
				
			}
		
		} catch (Exception e){
            e.printStackTrace();
        }
	}
	public void CreateTotalSVG(Solution solution, String DirPath, int duration) {
		try {
			Duration = duration * mlt;
			maxH = solution.getNOutPorts();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            Element svg = init(doc, solution);
            doc.appendChild(svg);
            
            for (Stream stream : solution.streams) {
				for (String node : stream.routingList) {
					Port crr_Port = getPortObject(solution, node, stream.Id);
					int s_index = getStreamIndex(solution, node, stream.Id);
					int port_index = getPortIndex(solution, node, stream.Id);
					for (int i = 0; i < crr_Port.indexMap[s_index].length; i++) {
						int x_1 = crr_Port.Topen[crr_Port.indexMap[s_index][i]] * mlt;
						int x_2 = crr_Port.Tclose[crr_Port.indexMap[s_index][i]] * mlt;
						int width = x_2 - x_1;
						if( (x_1 <= Duration) && (x_2 <= Duration)) {
							addFrame(doc, svg, x_1 + offset , rowH * port_index + Toffset, width, rowH, String.valueOf(stream.Id), stream.Priority);
						}

					}
				}
			}
               
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            Files.createDirectories(Paths.get(DirPath));
            String path = DirPath + "/" + "scheduletable.svg";
            StreamResult streamResult = new StreamResult(new File(path));
            transformer.transform(domSource, streamResult);
            
			
		} catch (Exception e){
            e.printStackTrace();
        }
	}
	private int getPortIndex(Solution Current, String swName, int mID) {
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
		return counter;
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
	private Element initIndivitual(Document doc, Solution solution, Stream stream) {
		
		int maxh = stream.routingList.size();
		Element root = doc.createElement("svg");
    	Attr baseAttr = doc.createAttribute("baseProfile");
    	baseAttr.setValue("full");
		root.setAttributeNode(baseAttr);
		
    	Attr xmlnsAttr = doc.createAttribute("xmlns");
    	xmlnsAttr.setValue("http://www.w3.org/2000/svg");
		root.setAttributeNode(xmlnsAttr);
		
    	Attr xmlnsev = doc.createAttribute("xmlns:ev");
    	xmlnsev.setValue("http://www.w3.org/2001/xml-events");
		root.setAttributeNode(xmlnsev);
		
    	Attr xmlnsxlinkAttr = doc.createAttribute("xmlns:xlink");
    	xmlnsxlinkAttr.setValue("http://www.w3.org/1999/xlink");
		root.setAttributeNode(xmlnsxlinkAttr);
		
    	Attr heightAttr = doc.createAttribute("height");
    	String heightString  = String.valueOf(rowH *(maxh + 1) + Toffset);
    	heightAttr.setValue(heightString);
		root.setAttributeNode(heightAttr);
		
    	Attr widthAttr = doc.createAttribute("width");
    	String widthString  = String.valueOf(Duration + 2 * offset);
    	widthAttr.setValue(widthString);
		root.setAttributeNode(widthAttr);
		
		Element defsElement = doc.createElement("defs");
		root.appendChild(defsElement);
		
		
		Element linElement = drawline(doc, (offset + 0), ((rowH *maxh) + Toffset) , (offset + Duration) , ((rowH *maxh) + Toffset), "bottum");
		root.appendChild(linElement);
		
		for (int i = 0; i <= (Duration/100); i++) {
			Element lineElement = drawline(doc, (offset + (i * 100)), (0 + Toffset), (offset + (i * 100)) , ((rowH * maxh) + Toffset), "mainvertical");
			root.appendChild(lineElement);
		}
		
		for (int i = 0; i <= (Duration/10); i++) {
			if((i % 100) != 0) {
				Element lineElement = drawline(doc, (offset + (i * 10)), (0 + Toffset), (offset + (i * 10)) , ((rowH * maxh) + Toffset), "subvertical");
				root.appendChild(lineElement);
			}
		}
		
		for (int i = 0; i <= (Duration/100); i++) {
			Element txtElement = addText(doc, String.valueOf(i * 100 * co), (offset + (i* 100)), ((rowH * (maxh + 1)) + Toffset), "timeTag");
			root.appendChild(txtElement);
		}
		
		for (int i = 0; i < maxh; i++) {
			Element lineElement = drawline(doc, 0, i * rowH + Toffset, offset + Duration , i * rowH + Toffset, "divider");
			root.appendChild(lineElement);
		}
		List<String> portListaStrings = stream.getPorts();
		for (int i = 0; i < maxh; i++) {
			
			Element txtElement = addText(doc, portListaStrings.get(i), offset/2, (rowH * i) + (2 * rowH/3) + Toffset, "portTag");
			root.appendChild(txtElement);
		}
		
		
		return root;
	}
	private Element init(Document doc, Solution solution) {
		Element root = doc.createElement("svg");
    	Attr baseAttr = doc.createAttribute("baseProfile");
    	baseAttr.setValue("full");
		root.setAttributeNode(baseAttr);
		
    	Attr xmlnsAttr = doc.createAttribute("xmlns");
    	xmlnsAttr.setValue("http://www.w3.org/2000/svg");
		root.setAttributeNode(xmlnsAttr);
		
    	Attr xmlnsev = doc.createAttribute("xmlns:ev");
    	xmlnsev.setValue("http://www.w3.org/2001/xml-events");
		root.setAttributeNode(xmlnsev);
		
    	Attr xmlnsxlinkAttr = doc.createAttribute("xmlns:xlink");
    	xmlnsxlinkAttr.setValue("http://www.w3.org/1999/xlink");
		root.setAttributeNode(xmlnsxlinkAttr);
		
    	Attr heightAttr = doc.createAttribute("height");
    	String heightString  = String.valueOf(rowH *(maxH + 1) + Toffset);
    	heightAttr.setValue(heightString);
		root.setAttributeNode(heightAttr);
		
    	Attr widthAttr = doc.createAttribute("width");
    	String widthString  = String.valueOf(Duration + 2 * offset);
    	widthAttr.setValue(widthString);
		root.setAttributeNode(widthAttr);
		
		Element defsElement = doc.createElement("defs");
		root.appendChild(defsElement);
		
		
		Element linElement = drawline(doc, (offset + 0), ((rowH *maxH) + Toffset) , (offset + Duration) , ((rowH *maxH) + Toffset), "bottum");
		root.appendChild(linElement);
		
		for (int i = 0; i <= (Duration/100); i++) {
			Element lineElement = drawline(doc, (offset + (i * 100)), (0 + Toffset), (offset + (i * 100)) , ((rowH * maxH) + Toffset), "mainvertical");
			root.appendChild(lineElement);
		}
		
		for (int i = 0; i <= (Duration/10); i++) {
			if((i % 100) != 0) {
				Element lineElement = drawline(doc, (offset + (i * 10)), (0 + Toffset), (offset + (i * 10)) , ((rowH * maxH) + Toffset), "subvertical");
				root.appendChild(lineElement);
			}
		}
		
		for (int i = 0; i <= (Duration/100); i++) {
			Element txtElement = addText(doc, String.valueOf(i * 100 * co), (offset + (i* 100)), ((rowH * (maxH + 1)) + Toffset), "timeTag");
			root.appendChild(txtElement);
		}
		
		for (int i = 0; i < solution.getNOutPorts(); i++) {
			Element lineElement = drawline(doc, 0, i * rowH + Toffset, offset + Duration , i * rowH + Toffset, "divider");
			root.appendChild(lineElement);
		}
		List<String> portListaStrings = solution.getOutPorts();
		for (int i = 0; i < solution.getNOutPorts(); i++) {
			
			Element txtElement = addText(doc, portListaStrings.get(i), offset/2, (rowH * i) + (2 * rowH/3) + Toffset, "portTag");
			root.appendChild(txtElement);
		}
		
		
		return root;
	}
	private void addFrame(Document doc, Element root,  int x, int y, int width, int height, String txt, int pri) {
		String colorString = null;
		switch (pri) {
		case 0:
			colorString = "maroon";
			break;
		case 1:
			colorString = "purple";
			break;
		case 2:
			colorString = "red";
			break;
		case 3:
			colorString = "orange";
			break;
		case 4:
			colorString = "fuchsia";
			break;
		case 5:
			colorString = "pink";
			break;
		case 6:
			colorString = "lime";
			break;
		case 7:
			colorString = "yellow";
			break;

		default:
			break;
		}
		
		Element rect = addBox(doc, x, y, width, height, colorString);
		if(isthereNode(root, rect)) {
			Element textElement = addText(doc, txt, x + (width/2), y + (2*rowH/3), "frameTag");
			ReplaceNode(root, textElement);
		}else {
			root.appendChild(rect);
			Element textElement = addText(doc, txt, x + (width/2), y + (2*rowH/3), "frameTag");
			root.appendChild(textElement);
		}
		

	}
	private Element addBox(Document doc, int x, int y, int width, int height, String color) {
		Element box = doc.createElement("rect");
		
    	Attr xAttr = doc.createAttribute("x");
    	xAttr.setValue(String.valueOf(x));
    	box.setAttributeNode(xAttr);
    	
    	Attr yAttr = doc.createAttribute("y");
    	yAttr.setValue(String.valueOf(y));
    	box.setAttributeNode(yAttr);
    	
    	Attr wAttr = doc.createAttribute("width");
    	wAttr.setValue(String.valueOf(width));
    	box.setAttributeNode(wAttr);
    	
    	Attr hAttr = doc.createAttribute("height");
    	hAttr.setValue(String.valueOf(height));
    	box.setAttributeNode(hAttr);
    	
    	Attr rxAttr = doc.createAttribute("rx");
    	rxAttr.setValue(String.valueOf(3));
    	box.setAttributeNode(rxAttr);
    	
    	Attr ryAttr = doc.createAttribute("ry");
    	ryAttr.setValue(String.valueOf(3));
    	box.setAttributeNode(ryAttr);
    	
    	Attr styleAttr = doc.createAttribute("style");
    	String styleString = "fill:"+ color + ";stroke:black;stroke-width:1;opacity:0.75" ;
    	styleAttr.setValue(styleString);
    	box.setAttributeNode(styleAttr);
    	
		return box;
	}
	private Element addText(Document doc, String txt, int x, int y, String where) {
		Element text = doc.createElement("text");
		
    	Attr xAttr = doc.createAttribute("x");
    	xAttr.setValue(String.valueOf(x));
    	text.setAttributeNode(xAttr);
    	
    	Attr yAttr = doc.createAttribute("y");
    	yAttr.setValue(String.valueOf(y));
    	text.setAttributeNode(yAttr);
    	
    	
    	Attr anchorAttr = doc.createAttribute("text-anchor");
    	anchorAttr.setValue("middle");
    	text.setAttributeNode(anchorAttr);
    	
    	Attr fillAttr = doc.createAttribute("fill");
    	Attr styleAttr = doc.createAttribute("style");
    	switch (where) {
		case "timeTag":
			fillAttr.setValue("black");
			styleAttr.setValue("font-style:bold ; font-family: times; font-size: 20px");
			break;
		case "portTag":
			fillAttr.setValue("black");
			styleAttr.setValue("font-style:italic; font-family: times; font-size: 16px");
			break;
		case "frameTag":
			fillAttr.setValue("black");
			styleAttr.setValue("font-family: times; font-size: 14px");
			break;

		default:
			break;
		}
    	
    	text.setAttributeNode(fillAttr);
    	text.setAttributeNode(styleAttr);
    	
    	text.setTextContent(txt);
    	
		return text;
	}
	
	private Element drawline(Document doc, int x1, int y1, int x2, int y2, String status) {
		Element line = doc.createElement("line");
		
    	Attr x1Attr = doc.createAttribute("x1");
    	x1Attr.setValue(String.valueOf(x1));
    	line.setAttributeNode(x1Attr);
    	
    	Attr y1Attr = doc.createAttribute("y1");
    	y1Attr.setValue(String.valueOf(y1));
    	line.setAttributeNode(y1Attr);
    	
    	Attr x2Attr = doc.createAttribute("x2");
    	x2Attr.setValue(String.valueOf(x2));
    	line.setAttributeNode(x2Attr);
    	
    	Attr y2Attr = doc.createAttribute("y2");
    	y2Attr.setValue(String.valueOf(y2));
    	line.setAttributeNode(y2Attr);
    	
    	Attr styleAttr = doc.createAttribute("style");
    	switch (status) {
		case "bottum":
			styleAttr.setValue("stroke:red ;stroke-width:3");
			break;
		case "mainvertical":
			styleAttr.setValue("stroke:black ;stroke-width:1");
			break;
			
		case "subvertical":
			styleAttr.setValue("stroke:gray ;stroke-width:1");
			break;
		case "divider":
			styleAttr.setValue("stroke:silver ;stroke-width:1");
			break;

		default:
			break;
		}
    	
    	line.setAttributeNode(styleAttr);
    	
    	return line;
	}
	private boolean isthereNode(Element root, Element rect) {
		NodeList rectsList = root.getElementsByTagName(rect.getTagName());
		for (int i = 0; i < rectsList.getLength(); i++) {
			Node nNode = rectsList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            	Element eElement = (Element) nNode;
    			if(eElement.isEqualNode(rect)) {
    				return true;
    			}
            }

		}
		return false;
	}
	private void ReplaceNode(Element root, Element text) {
		NodeList textList = root.getElementsByTagName(text.getTagName());
		for (int i = 0; i < textList.getLength(); i++) {
			Node nNode = textList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
            	Element eElement = (Element) nNode;
            	if((eElement.getAttribute("x").equals(text.getAttribute("x"))) && (eElement.getAttribute("y").equals(text.getAttribute("y")))) {
            		String contentString = eElement.getTextContent()+ ";" + text.getTextContent();
            		eElement.setTextContent(contentString);
            	}
            	
            }

		}
	}
}
