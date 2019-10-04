package TSN;

import java.util.ArrayList;
import java.util.List;

public class App {
	public App(int _id) {
		ID = _id;
		inputMessages = new ArrayList<Integer>();
		outputMessages = new ArrayList<Integer>();
	}
	public App(int _id, List<Integer> _in, List<Integer> _out ) {
		ID = _id;
		inputMessages = new ArrayList<Integer>();
		outputMessages = new ArrayList<Integer>();
		for (int _inID : _in) {
			inputMessages.add(_inID);
		}
		for (int _outID : _out) {
			outputMessages.add(_outID);
		}
	}
	
	public App Clone() {
		return new App(ID, inputMessages, outputMessages);
	}
	
	
	public int ID;
	
	List<Integer> inputMessages;
	List<Integer> outputMessages;
	
	public void AddInMessage(int id) {
		inputMessages.add(id);
	}
	public void AddOutMessage(int id) {
		outputMessages.add(id);
	}
	public boolean isIncluded(int _id) {
		for (Integer id : inputMessages) {
			if(id == _id) {
				return true;
			}
		}
		for (Integer id : outputMessages) {
			if(id == _id) {
				return true;
			}
		}
		return false;
	}

}
