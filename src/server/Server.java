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
	
	public Server() {
		Random rnd = new Random();
		processId = rnd.nextInt();
		logicalClock = 0;
		lamportAlgorithm = new LamportAlgorithm();
	}
	
	public int getValue(int dataId) {
		return database.get(dataId);
	}
	
	public void write(int dataId, int integerValue) {
		logicalClock++;
		Message message = new Message(processId, logicalClock, dataId, integerValue);
		lamportAlgorithm.write(message);
	}
	
	
	
}
