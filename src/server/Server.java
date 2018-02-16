package server;

import java.util.Random;
import java.util.concurrent.ConcurrentMap;

public class Server {
	
	private ConcurrentMap<Integer, Integer> database;
	private int processId;
	private int logicalClock;
	
	public Server() {
		Random rnd = new Random();
		processId = rnd.nextInt();
		logicalClock = 0;
	}
	
	public int getValue(int dataId) {
		return database.get(dataId);
	}
	
	
}
