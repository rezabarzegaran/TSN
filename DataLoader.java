package TSN;
import java.io.File;
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
public class DataLoader {

    public List<Messages> messages = new ArrayList<Messages>();
    public List<Routes> routings = new ArrayList<Routes>();
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