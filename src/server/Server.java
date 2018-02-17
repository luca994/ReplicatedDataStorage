package server;

import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import communication.LamportAlgorithm;
import communication.Message;

public class Server {
	
	private ConcurrentMap<Integer, Integer> database;
	private int processId;
	private int logicalClock;
	private LamportAlgorithm lamportAlgorithm;
	
	public Server(int groupLength) {
		Random rnd = new Random();
		processId = rnd.nextInt();
		logicalClock = 0;
		lamportAlgorithm = new LamportAlgorithm(processId, groupLength, this);
	}
	
	public int getValue(int dataId) {
		return database.get(dataId);
	}
	
	public void write(int dataId, int integerValue) {
		logicalClock++;
		Message message = new Message(processId, logicalClock, dataId, integerValue);
		lamportAlgorithm.write(message);
	}
	
	public void updateDatabase(Message message) {
		database.put(message.getDataId(), message.getIntegerValue());
	}
	
	
	
}
