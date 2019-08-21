package TSN;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;


import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

class Messages{
    Messages(int _id, int _period,int _deadline, int _size, int _priority, int _offset){
        id = _id;
        period = _period;
        deadline = _deadline;
        size = _size;
        priority = _priority;
        offset = _offset;
        
    }
    int id;
    int period;
    int deadline;
    int size;
    int priority;
    int offset;
    public Messages DeepClone(){
        return new Messages(id, period, deadline, size, priority, offset);
    }
    
}
class Routes{
	Routes(int _id, List<String> _nodes, List<Integer> _IDs){
		id = _id;
		for (String item : _nodes) {
			nodes.add(item);
		}
		for (Integer item : _IDs) {
			messsageIDs.add(item);
			
		}
		
	}
	int id;
	List<String> nodes = new ArrayList<String>();
	List<Integer> messsageIDs = new ArrayList<Integer>();
	public Routes DeepClone() {
		return new Routes(id , nodes, messsageIDs);
	}
}
class ControlApp{
	ControlApp(int _id, List<Integer> _inIDs, List<Integer> _outIDs){
		id = _id;
		for (Integer item : _inIDs) {
			inIDs.add(item);
			
		}
		for (Integer item : _outIDs) {
			outIDs.add(item);
			
		}
	}
	int id;
	List<Integer> inIDs = new ArrayList<Integer>();
	List<Integer> outIDs = new ArrayList<Integer>();
}
public class DataLoader {
	public GraphExporter graphicExporter = new GraphExporter();
    public List<Messages> messages = new ArrayList<Messages>();
    public List<Routes> routings = new ArrayList<Routes>();
    public List<ControlApp> CAs = new ArrayList<ControlApp>();
    public DataLoader(){

    }
    public void Load(String path){
        try{
            File fXmlFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();
                    
            NodeList messageList = doc.getElementsByTagName("Message");
            NodeList routingList = doc.getElementsByTagName("Route");
            NodeList appList = doc.getElementsByTagName("APP");
            

        
            for (int temp = 0; temp < messageList.getLength(); temp++) {

                Node nNode = messageList.item(temp);
                       
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        
                    Element eElement = (Element) nNode;
                    int _id = Integer.parseInt(eElement.getAttribute("id"));
                    int _deadline = Integer.parseInt(eElement.getAttribute("deadline"));
                    int _period = Integer.parseInt(eElement.getAttribute("period"));
                    int _size = Integer.parseInt(eElement.getAttribute("size"));
                    int _priority = Integer.parseInt(eElement.getAttribute("priority"));
                    int _offset = Integer.parseInt(eElement.getAttribute("offset"));
                    Messages tc = new Messages(_id,_period,_deadline,_size, _priority, _offset);
                    messages.add(tc);
        
        
                }
            }
            for (int temp = 0; temp < routingList.getLength(); temp++) {

                Node nNode = routingList.item(temp);
                List<String> nodenameList = new ArrayList<String>();
                List<Integer> IDnameList = new ArrayList<Integer>();
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        
                    Element eElement = (Element) nNode;
                    int _id = Integer.parseInt(eElement.getAttribute("id"));
                    NodeList listednotes = eElement.getElementsByTagName("node");
                    for (int i = 0; i < listednotes.getLength(); i++) {
						nodenameList.add(listednotes.item(i).getTextContent());
					}
                    NodeList listedID = eElement.getElementsByTagName("messageID");
                    for (int i = 0; i < listedID.getLength(); i++) {
                    	IDnameList.add(Integer.parseInt( listedID.item(i).getTextContent()));
					}


                    Routes tc = new Routes(_id,nodenameList,IDnameList);
                    routings.add(tc);
        
        
                }
            }
            for (int i = 0; i < appList.getLength(); i++) {
            	Node nNode = routingList.item(i);
            	List<Integer> inIDList = new ArrayList<Integer>();
            	List<Integer> outIDList = new ArrayList<Integer>();
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                	Element eElement = (Element) nNode;
                	int _id = Integer.parseInt(eElement.getAttribute("id"));
                    NodeList listednotes = eElement.getElementsByTagName("in");
                    for (int j = 0; j < listednotes.getLength(); j++) {
                    	inIDList.add(Integer.valueOf(listednotes.item(i).getTextContent()));
					}
                    NodeList listednotes2 = eElement.getElementsByTagName("in");
                    for (int j = 0; j < listednotes2.getLength(); j++) {
                    	outIDList.add(Integer.valueOf(listednotes2.item(i).getTextContent()));
					}
                	ControlApp tc = new ControlApp(_id, inIDList, outIDList);
                	CAs.add(tc);
                }
			}

        } catch (Exception e){
            e.printStackTrace();
        }


    }
    public void exportGraph() {
    	graphicExporter.Export( messages,routings, "visual");
    }
    public void UnloadLuxi() {
    	try {
    		String DirPath = "luxiTool";
    		Files.createDirectories(Paths.get(DirPath));
    		PrintWriter writer = new PrintWriter(DirPath + "/vis.txt", "UTF-8");
    		for (Routes r : routings) {
        		String routeLink = String.valueOf(r.id) + " : ";
        		for (int i = 0; i < (r.nodes.size() - 1); i++) {
        			routeLink += r.nodes.get(i) + "," + r.nodes.get(i+1) + " ; ";
				}
        		writer.println(routeLink);
			}

    		writer.close();
    		
    		PrintWriter mwriter = new PrintWriter(DirPath + "/msg.txt", "UTF-8");
    		for (Messages m : messages) {
    			String routingID = null;
    			for (Routes r : routings) {
					for (int mId : r.messsageIDs) {
						if(mId == m.id) {
							routingID = String.valueOf(r.id);
						}
					}
				}
    			String mLink = String.valueOf(m.id) + ", " + String.valueOf(m.size) + ", " + String.valueOf(m.deadline) + ", ";
    			mLink += routingID + ", TT, " + String.valueOf(m.priority) + ", " + String.valueOf(m.period);
    			mwriter.println(mLink);
			}

    		mwriter.close();
    		
    	} catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<Messages> getCloneMessages(){

        List<Messages> out_messages = new ArrayList<Messages>();
        for (Messages item : messages) {
            out_messages.add(item.DeepClone());
        }
        return out_messages;
    }

    public List<Messages> getMessages(){

        return messages;
    }
    public List<Routes> getCloneRoutes(){
    	List<Routes> out_routesList = new ArrayList<Routes>();
    	for (Routes item : routings) {
			out_routesList.add(item.DeepClone());
		}
    	return out_routesList;
    }
    public List<Routes> getRoutes(){
    	return routings;
    }
}