package TSN;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;




public class Solution {
	public Solution(){
        streams = new ArrayList<Stream>();
        ES = new ArrayList<EndSystems>();
        SW = new ArrayList<Switches>();
        costValues = new ArrayList<Long>();
        
    }
	public Solution(List<Stream> _streams, List<EndSystems> _es, List<Switches> _sw, List<Long> _costs, int _hyperperiod) {
        streams = new ArrayList<Stream>();
        ES = new ArrayList<EndSystems>();
        SW = new ArrayList<Switches>();
        costValues = new ArrayList<Long>();
        costValues.clear();
        
        for (Long val : _costs) {
			costValues.add(val);
		}
        for (Stream s : _streams) {
			streams.add(s.Clone());
		}
        for (EndSystems es : _es) {
			ES.add(es.Clone());
		}
        for (Switches sw : _sw) {
			SW.add(sw.Clone());
		}
        Hyperperiod = _hyperperiod;
	}
    public List<Stream> streams;
    public List<EndSystems> ES;
    public List<Switches> SW;
    public List<Long> costValues;
    public int Hyperperiod = 1;
    public void Create(List<Messages> _messages, List<Routes> routes, List<ControlApp> CAs){

        for (Messages item : _messages) {
            streams.add(new Stream(item.id, item.period, item.deadline, item.size, item.priority, item.offset));
        }
        for (Routes item : routes) {
        	
        	setSourceES(item);
        	setSinkES(item);
        	for (int i = 1; i < (item.nodes.size()-1); i++) {
        		String name = item.nodes.get(i);
        		Optional<Switches> tempSwitch = SW.stream().filter(x -> x.Name.equals(name)).findFirst();
        		Switches crrSwitches;
        		if (tempSwitch.isEmpty()) {
        			crrSwitches = new Switches(name);
					SW.add(crrSwitches);
				}else {
					crrSwitches = tempSwitch.get();
				}
        		
        		for ( int mID : item.messsageIDs) {
        			Optional<Stream> tempStream = streams.stream().filter(x -> (x.Id == mID)).findFirst();
					Stream mStream = tempStream.get();
					crrSwitches.addSteams(mStream);
				}
        		
			}
		}
        for (Switches sw : SW) {
        	for (Routes route : routes) {
				sw.setGraph(route.nodes, route.messsageIDs);
			}
			
		}
        for (Routes routes2 : routes) {
			for (int ID : routes2.messsageIDs) {
				for (Stream s : streams) {
					if(s.Id == ID) {
						s.SetRouting(routes2.nodes);
					}
				}
			}
		}
    }
    public void setSourceES(Routes item) {
    	Optional<EndSystems> tempSourcEndSystems = ES.stream().filter(x -> x.Name.equals(item.nodes.get(0))).findFirst();
    	if(tempSourcEndSystems.isEmpty()) {
    		EndSystems temp = new EndSystems(item.nodes.get(0));
    		for (int ID : item.messsageIDs) {
    			temp.addoutID(ID);
			}
    		ES.add(temp);
    	}else {
    		for (int ID : item.messsageIDs) {
    			tempSourcEndSystems.get().addoutID(ID);
			}
    		
    	}
    }
    public void setSinkES(Routes item) {
    	int NodeSize = item.nodes.size();
    	Optional<EndSystems> tempSinkEndSystems = ES.stream().filter(x -> x.Name.equals(item.nodes.get(NodeSize-1))).findFirst();
    	if(tempSinkEndSystems.isEmpty()) {
    		EndSystems temp = new EndSystems(item.nodes.get(NodeSize-1));
    		for (int ID : item.messsageIDs) {
    			temp.addinID(ID);
			}
    		ES.add(temp);
    	}else {
    		for (int ID : item.messsageIDs) {
    			tempSinkEndSystems.get().addinID(ID);
			}
    	}
    }
    public void Initialize(){
    	for (Stream s : streams) {
    		Hyperperiod = LCM(Hyperperiod, s.Period);
		}
    	for (Stream s : streams) {
    		s.initiate(Hyperperiod);
		}
    	for (Switches sw : SW) {
			
			sw.initiate();
		}


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
    public int getNOutPorts() {
    	int Outports = 0;
		for (Switches sw : SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					Outports++;
				}
			}
		}
		return Outports;
    }
    public List<String> getOutPorts(){
    	List<String> nameStrings = new ArrayList<String>();
		for (Switches sw : SW) {
			for (Port port : sw.ports) {
				if(port.outPort) {
					nameStrings.add("[" + sw.Name + " : " + port.connectedTo + "]");
				}
			}
		}
		return nameStrings;
    }
    public Solution Clone() {
    	return new Solution(streams, ES, SW, costValues, Hyperperiod);
    }
    public int getCost() {
    	int valCostLong =  0;
    	for (Long a : costValues) {
    		valCostLong += a;
    		System.out.println(a);
		}
    	return valCostLong;
    }

}
