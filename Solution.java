package TSN;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class Solution {
	public Solution(DataLoader dataLoader) {
        streams = new ArrayList<Stream>();
        ES = new ArrayList<EndSystems>();
        SW = new ArrayList<Switches>();
        costValues = new ArrayList<Long>();
        Apps = new ArrayList<App>();
        Create(dataLoader.getMessages(), dataLoader.getRoutes(), dataLoader.getApps(), dataLoader.getSwitches());
        Initialize();
	}
	public Solution(List<Stream> _streams, List<EndSystems> _es, List<Switches> _sw, List<App> _apps, List<Long> _costs, int _hyperperiod) {
        streams = new ArrayList<Stream>();
        ES = new ArrayList<EndSystems>();
        SW = new ArrayList<Switches>();
        Apps = new ArrayList<App>();
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
        for (App _app : _apps) {
			Apps.add(_app.Clone());
		}
        Hyperperiod = _hyperperiod;
	}
    
	public List<Stream> streams;
    public List<EndSystems> ES;
    public List<Switches> SW;
    public List<App> Apps;
    public List<Long> costValues;  
    public int Hyperperiod = 1;
    
    private void Create(List<Messages> _messages, List<Routes> routes, List<ControlApp> CAs, List<NetSwitch> SWs){

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
        
        for (ControlApp controlApp : CAs) {
        	App tc = new App(controlApp.id);
        	for (int id : controlApp.inIDs) {
				tc.AddInMessage(id);
			}
        	for (int id : controlApp.outIDs) {
				tc.AddOutMessage(id);
			}
        	Apps.add(tc);
		}
        for (NetSwitch netSwitch : SWs) {
    		Optional<Switches> tempSwitch = SW.stream().filter(x -> x.Name.equals(netSwitch.Name)).findFirst();
    		if (tempSwitch.isPresent()) {
    			Switches crrSwitches = tempSwitch.get();
    			crrSwitches.addHashTable(netSwitch.delayTable);
			}
    		
		}
        
    }
    private void setSourceES(Routes item) {
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
    private void setSinkES(Routes item) {
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
    private void Initialize(){
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
    private int LCM(int a, int b) {
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
    	return new Solution(streams, ES, SW, Apps, costValues, (Hyperperiod));
    }
    public List<Integer> getCosts() {
    	List<Integer> CostTerms = new ArrayList<Integer>(); 
    	for (Long temp : costValues) {
    		CostTerms.add(temp.intValue());
		}
    	return CostTerms;
    }


}
