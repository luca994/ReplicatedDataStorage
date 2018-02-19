package server;

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
	}
	
	public int getValue(int dataId) {
		return database.get(dataId);
	}
	
	public void write(int dataId, int integerValue) {
		lamportAlgorithm.write(dataId, integerValue, processId);
	}
	
	public synchronized void updateDatabase(Message message) {
		database.put(message.getDataId(), message.getIntegerValue());
	}

	public int getProcessId() {
		return processId;
	}
	
}
