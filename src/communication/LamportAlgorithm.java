package communication;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

import server.Server;

public class LamportAlgorithm {

	private ReliableChannel reliableChannel;
	private PriorityBlockingQueue<Message> writeQueue;
	private ConcurrentMap<String, Integer> ackCount;
	private int groupSize;
	private Integer logicalClock;
	private Server server;
	private Thread updateThread;

	public LamportAlgorithm(int processId, int groupLength, Server server) {
		updateThread = new Thread(new CheckQueue());
		logicalClock = 0;
		this.server = server;
		Comparator<Message> c = new Order();
		groupSize = groupLength;
		writeQueue = new PriorityBlockingQueue<>(11, c);
		ackCount = new ConcurrentHashMap<>();
		reliableChannel = new ReliableChannel(processId, groupLength, this);
		updateThread.start();
	}

	public synchronized void write(int dataId, int integerValue, int processId) {
		logicalClock++;
		Message message = new Message(processId, logicalClock, dataId, integerValue);
		writeQueue.put(message);
		logicalClock++;
		LamportAck ack = new LamportAck(processId, logicalClock, message.getLogicalClock());
		reliableChannel.sendMessage(message);
		reliableChannel.sendMessage(ack);
		ackHandler(ack);
	}

	public void receiveEvent(Event e) {
		lamportClockUpdate(e);
		if (e instanceof Message)
			messageHandler((Message) e);
		else if (e instanceof LamportAck)
			ackHandler((LamportAck) e);
	}

	private synchronized void messageHandler(Message m) {
		writeQueue.put(m);
		logicalClock++;
		LamportAck ack = new LamportAck(server.getProcessId(), logicalClock, m.getLogicalClock());
		reliableChannel.sendMessage(ack);
	}

	private synchronized void ackHandler(LamportAck a) {
		Integer count = ackCount.get(a.getIdRelatedMessage());
		if (count == null) {
			ackCount.put(a.getIdRelatedMessage(), 1);
		} else if (count < groupSize) {
			int value = count.intValue();
			value++;
			ackCount.put(a.getIdRelatedMessage(), value);
		}
		notifyAll();
	}

	private synchronized void lamportClockUpdate(Event e) {
		logicalClock = Math.max(logicalClock, e.getLogicalClock()) + 1;
	}

	private class CheckQueue implements Runnable {
		@Override
		public synchronized void run() {
			while (true) {
				try {
					while (checkQueueHead()) {
						Message m = writeQueue.poll();
						server.updateDatabase(m);
					}
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		public boolean checkQueueHead() {
			if (writeQueue.isEmpty())
				return false;
			Message m = writeQueue.element();
			int count = ackCount.get(m.eventId);
			if (count == groupSize)
				return true;
			else
				return false;
		}

	}

	/*
	 * public static void main(String[] args) { LamportAlgorithm l = new
	 * LamportAlgorithm(0, 0, null); l.logicalClock = 0; Message m = new Message(0,
	 * l.logicalClock, 0, 0); l.logicalClock++; Message m1 = new Message(0,
	 * l.logicalClock, 0, 0); l.logicalClock++;
	 * System.out.println("logClock of m: "+m.getLogicalClock()+"\n");
	 * System.out.println("logClock of m1: "+m1.getLogicalClock()+"\n");
	 * 
	 * }
	 */

}
