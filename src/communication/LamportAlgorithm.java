package communication;

import java.util.Comparator;
import java.util.Iterator;
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
	private Object lock = new Object();

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
		reliableChannel.sendMessage(message);
		Thread t = new Thread(new CheckSameClock(message));
		t.start();
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
		Thread t = new Thread(new CheckSameClock(m));
		t.start();
	}

	private void ackHandler(LamportAck a) {
		Integer count = ackCount.get(a.getIdRelatedMessage());
		if (count == null) {
			ackCount.put(a.getIdRelatedMessage(), 1);
		} else if (count < groupSize) {
			int value = count.intValue();
			value++;
			ackCount.put(a.getIdRelatedMessage(), value);
		}
		synchronized (lock) {
			lock.notify();
		}
	}

	private synchronized void lamportClockUpdate(Event e) {
		logicalClock = Math.max(logicalClock, e.getLogicalClock()) + 1;
	}

	private class CheckSameClock implements Runnable {

		private Message messageToCheck;
		private boolean differentClock;

		public CheckSameClock(Message message) {
			messageToCheck = message;
			differentClock = true;
		}

		private synchronized void sendAck() {
			logicalClock++;
			LamportAck ack = new LamportAck(server.getProcessId(), logicalClock, messageToCheck.getLogicalClock(),
					messageToCheck.getProcessId());
			ackHandler(ack);
			reliableChannel.sendMessage(ack);
			return;
		}

		@Override
		public synchronized void run() {
			Iterator<Message> i = writeQueue.iterator();
			Message messageTemp = null;
			if (i.hasNext()) {
				messageTemp = i.next();
				i = writeQueue.iterator();
			}
			if (messageTemp == null)
				differentClock = false;
			while (i.hasNext()) {
				messageTemp = i.next();
				if (messageTemp.getLogicalClock() == messageToCheck.getLogicalClock()
						&& server.getProcessId() > messageToCheck.getProcessId()) {
					sendAck();
					return;
				}
				if (messageTemp.getLogicalClock() == messageToCheck.getLogicalClock() && messageTemp.getEventId() != messageToCheck.getEventId())
					differentClock = false;
			}
			if (differentClock == true)
				sendAck();
			differentClock = true;
		}
	}

	private class CheckQueue implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					while (checkQueueHead()) {
						Message m = writeQueue.poll();
						server.updateDatabase(m);
						ackCount.remove(m.getEventId());
					}
					synchronized (lock) {
						lock.wait();
					}
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
}
