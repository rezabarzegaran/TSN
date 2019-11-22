package TSN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


public class Switches {
	String Name;
	int Hyperperiod = 1;
	List<Stream> streams =  new ArrayList<Stream>();
	List<Port> ports = new ArrayList<Port>();
	int clockAsync = 0;
	int microtick = 1;
	Map<Integer, Integer> delayTable = new HashMap<Integer, Integer>();

	public Switches(String _name) {
		Name = _name;
		Random rndRandom = new Random();
		//clockAsync =rndRandom.nextInt(10);
		
	}
	public Switches(String _name, int _hyperperiod, int _clock, List<Stream> _s, List<Port> _ports, Map<Integer, Integer> _delayTable) {
		Name = _name;
		Hyperperiod = _hyperperiod;
		clockAsync = _clock;
		for (Stream stream : _s) {
			streams.add(stream.Clone());
		}
		for (Port port : _ports) {
			ports.add(port.Clone());
		}
		addHashTable(_delayTable);
	}
	public void addHashTable(Map<Integer, Integer> inTable) {
		delayTable.clear();
		for (Map.Entry<Integer, Integer> entry : inTable.entrySet()) {
			delayTable.put(entry.getKey(), entry.getValue());
		}
	}
	public void addSteams(Stream s) {
		Hyperperiod = LCM(Hyperperiod, s.Period);
		streams.add(s);
	}
	public int LCM(int a, int b) {
		int lcm = (a > b) ? a : b;
        while(true)
        {
            if( lcm % a == 0 && lcm % b == 0 )
            {
                break;
            }
            ++lcm;
        }
		return lcm;
	}
	public void setGraph(List<String> nodes, List<Integer> Ids) {
		int index = isIncluded(nodes);
		if(index != -1) {
			if (!isExistInPort(nodes.get(index -1))) {
				//System.out.println(nodes.get(index));
				ports.add(new Port(nodes.get(index-1), false, microtick));
			}
			if(!isExistOutPort(nodes.get(index+1))) {
				Port temPort = new Port(nodes.get(index+1), true, microtick);
				for (int _id : Ids) {
					Optional<Stream> s = streams.stream().filter(x -> x.Id == _id).findFirst();
					if(s.isPresent()) {
						temPort.AssignStream(s.get());
					}
				}			
				ports.add(temPort);
			}else {
				Optional<Port> temPort = ports.stream().filter(x -> (x.connectedTo.equals(nodes.get(index+1)) && x.outPort)).findFirst();
				if(temPort.isPresent()) {
					for (int _id : Ids) {
						Optional<Stream> s = streams.stream().filter(x -> x.Id == _id).findFirst();
						if(s.isPresent()) {
							temPort.get().AssignStream(s.get());
						}
					}
				}
			}
			
		}
	}
	public int isIncluded(List<String> nodes) {
		int index = -1;
		for (String node : nodes) {
			if(Name.equals(node)) {
				index = nodes.indexOf(node);
			}
		}
		return index;
	}
	public boolean isExistOutPort(String sideName) {
		Optional<Port> tempPort = ports.stream().filter(x -> (x.connectedTo.equals(sideName) && x.outPort)).findFirst();
		if(tempPort.isEmpty()) {
			return false;
		}
		return true;
	}
	public boolean isExistInPort(String sideName) {
		Optional<Port> tempPort = ports.stream().filter(x -> (x.connectedTo.equals(sideName)&& !x.outPort)).findFirst();
		if(tempPort.isEmpty()) {
			return false;
		}
		return true;
	}
	public void initiate() {
		for (Port port : ports) {
			port.initiate();
		}
	}
	public int getDelay(Stream s) {
		int totalDelay = clockAsync;
		int size = s.Size;
		totalDelay += ItterateValue(size);
		
		//System.out.println("For size: " + size +", delay equals to: " + totalDelay );
		
		return totalDelay;
	}
	private int ItterateValue(int size) {
		
		int LowerKey = Integer.MIN_VALUE;
		int UpperKey = Integer.MAX_VALUE;
		int LowerValue = -1;
		int UpperValue = -1;
		for (Map.Entry<Integer, Integer> entry : delayTable.entrySet()) {
			if(entry.getKey() <= size) {
				if(entry.getKey() >= LowerKey) {
					LowerKey = entry.getKey();
					LowerValue = entry.getValue();
				}
				if(entry.getKey() >= size) {
					if(entry.getKey() <= UpperKey) {
						UpperKey = entry.getKey();
						UpperValue = entry.getValue();
					}
				}
			}
					
		}
		
		if((LowerValue != -1 ) && (UpperValue != -1)) {
			double step = UpperKey - LowerKey;
			double Slope = Math.ceil((UpperValue - LowerValue)/step);
			int finalvalue = LowerValue + (int) Slope * (size - LowerKey);
			return finalvalue;
		}
		return 0 ;
	}
	public Switches Clone() {
		return new Switches(Name, Hyperperiod, clockAsync, streams, ports, delayTable);
	}

}
class Port {
	int[] affiliatedQue;
	int[] Topen;
	int[] Tclose;
	int[][] indexMap;
	int GCLSize = 0;
	boolean outPort;
	int Period;
	String connectedTo;
	boolean connectedToES;
	int _microtick;
	int propagationDelay = 0;
	Que[] ques = new Que[8];
	List<Stream> AssignedStreams =  new ArrayList<Stream>();
	
	
	Port(String sideName, boolean isOut, int microtick){
		connectedTo = sideName;
		if(sideName.contains("ES")) {
			connectedToES = true;
		}else {
			connectedToES = false;
		}
		outPort = isOut;
		for (int i = 0; i < ques.length; i++) {
			ques[i] = new Que(i);
		}
	_microtick = microtick;
	Period = 0;
		
	}
	Port(String sideName, boolean isOut, List<Stream> _assignedstreams, boolean _c_to_es, int gcl, int[] _affq, int[] _topen, int[] _tclose, int[][] _index, int microtick, int _Period){
		connectedTo = sideName;
		outPort = isOut;
		for (int i = 0; i < ques.length; i++) {
			ques[i] = new Que(i);
		}
		for (Stream stream : _assignedstreams) {
			AssignedStreams.add(stream.Clone());
		}
		connectedToES = _c_to_es;
		GCLSize = gcl;
		affiliatedQue = new int[_affq.length];
		for (int i = 0; i < _affq.length; i++) {
			affiliatedQue[i] = _affq[i];
		}
		Topen = new int[_topen.length];
		for (int i = 0; i < _topen.length; i++) {
			Topen[i] = _topen[i];
		}
		Tclose = new int[_tclose.length];
		for (int i = 0; i < _tclose.length; i++) {
			Tclose[i] = _tclose[i];
		}
		indexMap = new int[_index.length][];
		for (int i = 0; i < _index.length; i++) {
			indexMap[i] = new int[_index[i].length];
			for (int j = 0; j < _index[i].length; j++) {
				indexMap[i][j] = _index[i][j];
			}
		}
		_microtick = microtick;
		Period = _Period;
		
		
	}
	int getPeriod() {
		return Period;
	}
	void setPeriod(int P) {
		Period = P;
	}
	int GetNQue(){
		return ques.length;
	}
	int getUsedQ() {
		int used = 0;
		for (Que Q : ques) {
			if(Q.assignedStreams.size() > 0) {
				used++;
			}
		}
		return used;
	}
	void AssignStream(Stream s) {
		AssignedStreams.add(s);
	}
	void initiate() {
		int GCLsize = 0;
		for (Stream stream : AssignedStreams) {
			ques[stream.Priority].AssignStream(stream);
			GCLsize += stream.N_instances;
			
		}
		Topen = new int[GCLsize];
		Tclose = new int[GCLsize];
		affiliatedQue = new int[GCLsize];
		GCLSize = GCLsize;
		indexMap = new int [AssignedStreams.size()][];
		for (int i = 0; i < AssignedStreams.size(); i++) {
			indexMap[i] = new int[AssignedStreams.get(i).N_instances];
		}
		int counter = 0;
		if(outPort) {
			
			for (int i = 0; i < AssignedStreams.size(); i++) {
				int N_istances = AssignedStreams.get(i).Hyperperiod / AssignedStreams.get(i).Period;
				for (int j = 0; j < N_istances; j++) {
					indexMap[i][j] = counter;
					//indexMap[i][j] = (int) Math.round((7*j)/8.0);
					counter++ ;
				}
			}
			
			
		}

				
		
		//int counter = 0;
		//for (int i = 0; i < AssignedStreams.size(); i++) {
			//indexMap[i] = new int[AssignedStreams.get(i).N_instances];
			//for (int j = 0; j < AssignedStreams.get(i).N_instances; j++) {
				//indexMap[i][j] = counter;
				//counter++;
			//}
			
		//}
		
		
	}
	public Port Clone() {
		return new Port(connectedTo, outPort, AssignedStreams, connectedToES, GCLSize, affiliatedQue, Topen, Tclose, indexMap, _microtick, Period);
	}

}
class Que{
	int id;
	int Priority;
	List<Stream> assignedStreams = new ArrayList<Stream>();
	public Que(int per){
		setID(per);
		setPriority(per);
	}
	private void setID(int _id) {
		id = _id;
	}
	private void setPriority(int per) {
		Priority = per;
	}
	public void AssignStream(Stream s) {
		assignedStreams.add(s);
	}
}