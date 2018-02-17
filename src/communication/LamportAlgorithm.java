package communication;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import server.Server;

public class LamportAlgorithm {
	
	private ReliableChannel reliableChannel;
	private PriorityBlockingQueue<Message> writeQueue;
	private ConcurrentMap<String, Integer> ackCount;
	private int ackToDelivery;
	private Server server;
	
	public LamportAlgorithm(int processId, int groupLength, Server server) {
		this.server = server;
		Comparator<Message> c = new Order();
		ackToDelivery = groupLength;
		writeQueue = new PriorityBlockingQueue<>(11, c);
		reliableChannel = new ReliableChannel(processId, groupLength);
	}
	
	public void write(Message message) {
		try {
			writeQueue.put(message);
			ackCount.put(message.getEventId(), 1);
			reliableChannel.sendMessage(message);
			reliableChannel.sendMessage(new Ack(message.getProcessId(), message.getLogicalClock()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receiveEvent(Event e) {
		if(e instanceof Message)
			messageHandler((Message) e);
		else if(e instanceof Ack)
			ackHandler((Ack) e);
	}
	
	public void messageHandler(Message m) {
		writeQueue.put(m);
		//multicast acknowledgment
	}
	
	public void ackHandler(Ack a) {
		
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
