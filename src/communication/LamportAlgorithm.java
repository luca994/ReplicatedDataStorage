package communication;

import java.util.Comparator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import server.Server;

public class LamportAlgorithm {
	
	private ReliableChannel reliableChannel;
	private PriorityBlockingQueue<Message> writeQueue;
	private ConcurrentMap<String, Integer> ackCount;
	private int groupSize;
	private int logicalClock;
	private Server server;
	
	public LamportAlgorithm(int processId, int groupLength, Server server) {
		logicalClock = 0;
		this.server = server;
		Comparator<Message> c = new Order();
		groupSize = groupLength;
		writeQueue = new PriorityBlockingQueue<>(11, c);
		reliableChannel = new ReliableChannel(processId, groupLength, this);
	}
	
	public synchronized void write(int dataId, int integerValue, int processId) {
		logicalClock++;
		Message message = new Message(processId, logicalClock, dataId, integerValue);
		logicalClock++;
		Ack ack = new Ack(processId, logicalClock);
		reliableChannel.sendMessage(message);
		reliableChannel.sendMessage(ack);
	}
	
	public void receiveEvent(Event e) {
		if(e instanceof Message)
			messageHandler((Message) e);
		else if(e instanceof LamportAck)
			ackHandler((LamportAck) e);
	}
	
	public void messageHandler(Message m) {
		writeQueue.put(m);
	}
	
	public void ackHandler(LamportAck a) {
		
	}
	
	
	/*
	 * prova coda prioritaria (da eliminare)
	 */
	/*public Message getQueueHead() {
		return database.element();
	}
	
	public static void main(String[] args) {
		Message m = new Message(0, 1, 0, 0);
		Message m1 = new Message(1, 1, 0, 0);
		LamportAlgorithm l = new LamportAlgorithm();
		l.write(m1);
		l.write(m);
		System.out.println(l.getQueueHead().getEventId());
		
	}*/
	
}
