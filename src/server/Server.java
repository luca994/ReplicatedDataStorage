package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import communication.LamportAlgorithm;
import communication.Message;

public class Server {

	private ConcurrentMap<Integer, Integer> database;
	private int processId;
	private LamportAlgorithm lamportAlgorithm;
	private BlockingQueue<Message> messageDelivered;

	public Server(int processId, int groupLength) {
		this.processId = processId;
		this.messageDelivered = new LinkedBlockingQueue<Message>();
		lamportAlgorithm = new LamportAlgorithm(processId, groupLength, messageDelivered);
		database = new ConcurrentHashMap<>();
	}

	public String getValue(int dataId) {
		Integer value = database.get(dataId);
		if (value == null)
			return null;
		else
			return value.toString();
	}

	public void write(int dataId, int integerValue) {
		lamportAlgorithm.write(dataId, integerValue, processId);
	}

	public synchronized void updateDatabase() {
		while (true) {
			try {
				Message message = messageDelivered.take();
				database.put(message.getDataId(), message.getIntegerValue());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void print() {
		System.out.println("Database:\n");
		for (Integer key : database.keySet()) {
			System.out.println("Key:" + key + " value:" + database.get(key) + "\n");
		}
	}

	public int getProcessId() {
		return processId;
	}

}
