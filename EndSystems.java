package TSN;

import java.util.ArrayList;
import java.util.List;


public class EndSystems {
	String Name;
	List<Integer> inStreamsIDs = new ArrayList<Integer>();
	List<Integer> outStreamsIDs = new ArrayList<Integer>();
	public EndSystems(String _name) {
		Name = _name;
	}
	public EndSystems(String _name, List<Integer> _in, List<Integer> _out) {
		Name = _name;
		for (int inval : _in) {
			inStreamsIDs.add(inval);
		}
		for (int outval : _out) {
			outStreamsIDs.add(outval);
		}
		
	}
	
	public void addinID(int ID) {
		inStreamsIDs.add(ID);
	}
	public void addoutID(int ID) {
		outStreamsIDs.add(ID);
	}
	public EndSystems Clone() {
		return new EndSystems(Name, inStreamsIDs, outStreamsIDs);
	}
	
	
	

}
