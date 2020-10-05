package TSN;

import java.util.ArrayList;
import java.util.List;

public class Stream {
	public Stream(int _id, int _period, int _deadline, int _size, int priority, int _offset){
        Id = _id;
        Period = _period;
        Deadline = _deadline;
        Size = _size;
        Priority = priority;
        offset = _offset;
        // 1 Byte takes 0.008us at 1000Mbits, 42bit header for ethernet packet. Round down.
        Transmit_Time = (int) Math.ceil((Size + 42)*0.081);
        //Speed 1000
        //Transmit_Time = (int) Math.ceil((Size)*0.008);
        
        //Speed 100
        //Transmit_Time = (int) Math.ceil((Size)*0.08);
        // Use this for Jorge
        //Transmit_Time = (int) Math.ceil((Size)*0.005);
        routingList = new ArrayList<String>();
        CroutingList = new ArrayList<String>();

    }
	public Stream(int _id, int _period, int _deadline, int _size, int priority, int _offset, int _hyper, int _instances, List<String> _routing, List<String> _crouting){
        Id = _id;
        Period = _period;
        Deadline = _deadline;
        Size = _size;
        Priority = priority;
        offset = _offset;
        // 1 Byte takes 0.008us at 1000Mbits, 42bit header for ethernet packet. Round down.
        Transmit_Time = (int) Math.ceil((Size + 42)*0.081);
        //Transmit_Time = (int) Math.ceil((Size)*0.008);
        
        //Speed 100
        //Transmit_Time = (int) Math.ceil((Size)*0.08);
        // Use this for Jorge
        //Transmit_Time = (int) Math.ceil((Size)*0.005);
        routingList = new ArrayList<String>();
        CroutingList = new ArrayList<String>();
        Hyperperiod = _hyper;
        N_instances = _instances;
        for (String s : _routing) {
			routingList.add(s);
		}
        for (String s : _crouting) {
			CroutingList.add(s);
		}

    }

	
	
    public int Id;
    public int Period;
    public int Deadline;
    public int Size;
    public int Priority;
    public int Transmit_Time;
    public int Hyperperiod;
    public int N_instances;
    public int offset;
    List<String> routingList;
    List<String> CroutingList;
    
    public void initiate(int hyperperiod) {
    	Hyperperiod = hyperperiod;
    	N_instances = (Hyperperiod - offset)/Period;
    	
    }
    public void SetRouting(List<String> routing) {
    	for (String s : routing) {
    		CroutingList.add(s);
			if(!s.contains("ES")) {
				routingList.add(s);
			}
		}
    	
    }
    public String getFirstSwitch() {
    	if(!routingList.isEmpty()) {
    		return routingList.get(0);
    	}
    	return null;
    }
    public String getLastSwitch() {
    	if(!routingList.isEmpty()) {
    		return routingList.get(routingList.size() - 1 );
    	}
    	return null;
    }
    public String getpreviousSwitch(String currentSW) {
    	for (int i = 0; i < routingList.size(); i++) {
			if(routingList.get(i).equals(currentSW)) {
				return routingList.get(i - 1);
			}
		}
    	return null;
    }
    public boolean isThisFirstSwtich(String swName) {
    	for (int i = 0; i < routingList.size(); i++) {
			if(routingList.get(i).equals(swName)) {
				if(i == 0){
					return true;
				}
			}
		}
    	return false;
    }
    public List<String> getPorts(){
    	List<String> nameStrings = new ArrayList<String>();
    	for (int i = 1; i < (CroutingList.size()-1); i++) {
    		nameStrings.add("[" + CroutingList.get(i) + " : " + CroutingList.get(i+1) + "]");
		}
		return nameStrings;
    }
    public Stream Clone() {
    	return new Stream(Id, Period, Deadline, Size, Priority, offset, Hyperperiod, N_instances, routingList, CroutingList );
    }

}
