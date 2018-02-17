package communication;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class LamportAlgorithm {
	
	//private ReliableChannel reliableChannel
	//private ConcurrentLinkedQueue<Message> writeQueue;
	private PriorityBlockingQueue<Message> database; 
	
	public LamportAlgorithm() {
		Comparator<Message> c = new Order();
		database = new PriorityBlockingQueue<>(11, c);
	}
	
	public void write(Message message) {
		database.put(message);
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
