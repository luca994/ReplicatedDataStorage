package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import communication.LamportAlgorithm;
import communication.Message;

public class Server {
	
	private ConcurrentMap<Integer, Integer> database;
	private int processId;
	private LamportAlgorithm lamportAlgorithm;
	
	public Server(int processId, int groupLength) {
		this.processId = processId;
		lamportAlgorithm = new LamportAlgorithm(processId, groupLength, this);
		database = new ConcurrentHashMap<>();
	}
	
	public String getValue(int dataId) {
		Integer value = database.get(dataId);
		if(value == null)
			return null;
		else 
			return value.toString();
	}
	
	public void write(int dataId, int integerValue) {
		lamportAlgorithm.write(dataId, integerValue, processId);
	}
	
	public synchronized void updateDatabase(Message message) {
		database.put(message.getDataId(), message.getIntegerValue());
	}
	
	public void print() {
		System.out.println("Database:\n");
		database.toString();
	}

	public int getProcessId() {
		return processId;
	}
	
}
