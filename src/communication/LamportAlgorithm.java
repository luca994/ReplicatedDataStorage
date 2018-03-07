package communication;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class LamportAlgorithm implements Runnable {

	private int processId;
	private int groupSize;
	private ExecutorService exec;

	/**
	 * Is the current logical clock
	 */
	private Integer logicalClock;

	/**
	 * Is the reliable channel from which the servers exchange messages
	 */
	private ReliableChannel reliableChannel;

	/**
	 * Is the queue that reorders the messages based on logical clock
	 */
	private PriorityBlockingQueue<Message> writeQueue;

	/**
	 * The key is the event id the value is the number of Lamportacks received
	 */
	private ConcurrentMap<String, Integer> ackCount;

	/**
	 * This variable is used as a mutex
	 */
	private Object lock;

	/**
	 * This queue is the queue in which we place a message when is to deliver to the
	 * application (we have received all the Lamport acks and is at the head of the
	 * queue. (PRODUCER)
	 */
	private BlockingQueue<Message> messageDelivered;

	/**
	 * This queue is the queue from which we take the events received from the
	 * channel (CONSUMER)
	 */
	private BlockingQueue<Event> eventReceived;

	/**
	 * Constructor makes sure that the Lamport Algorithm works fine
	 * 
	 * @param processId
	 * @param groupLength
	 * @param messageQueue
	 */
	public LamportAlgorithm(int processId, int groupLength, BlockingQueue<Message> messageQueue) {
		this.logicalClock = 0;
		this.processId = processId;
		this.groupSize = groupLength;
		this.messageDelivered = messageQueue;
		lock = new Object();
		writeQueue = new PriorityBlockingQueue<>(11, new Order());
		ackCount = new ConcurrentHashMap<>();
		eventReceived = new LinkedBlockingQueue<Event>();
		reliableChannel = new ReliableChannel(processId, groupLength, eventReceived);
		this.exec = Executors.newCachedThreadPool();
		exec.submit(new CheckQueue());
		exec.submit(this);
	}

	/**
	 * Send a write message with a new element to the other processes
	 * 
	 * @param dataId
	 * @param integerValue
	 */
	public synchronized void write(int dataId, int integerValue) {
		logicalClock++;
		Message message = new Message(processId, logicalClock, dataId, integerValue);
		reliableChannel.enqueueEvent(message);
		System.out.println("[SENDING] Message: " + message.getEventId() + " with element: <" + message.getDataId() + ","
				+ message.getIntegerValue() + ">");
		messageHandler(message);
	}

	/**
	 * looks for an update message with the specified dataId and with the most
	 * recent lamport clock, if there isn't one it returns null
	 * 
	 * @param dataId
	 * @return
	 */
	public String read(int dataId) {
		Message m1 = new Message(processId, 0, 0, 0);
		for (Message m : writeQueue) {
			if (m.getDataId() == dataId && m.getProcessId() == processId && m.getLogicalClock() > m1.getLogicalClock())
				m1 = m;
		}
		if (m1.getLogicalClock() != 0)
			return Integer.valueOf(m1.getIntegerValue()).toString();
		else
			return null;
	}

	/**
	 * Receive and manage an event sent from other processes
	 * 
	 * @throws InterruptedException
	 */
	private void receiveEvent() throws InterruptedException {
		while (true) {
			Event e = eventReceived.take();
			lamportClockUpdate(e);
			if (e instanceof Message) {
				System.out.println("[RECEIVED] Message: " + e.getEventId() + " with element: <" + ((Message) e).getDataId() + ","
						+ ((Message) e).getIntegerValue() + ">");
				messageHandler((Message) e);
			} else if (e instanceof LamportAck) {
				System.out.println("[RECEIVED] Ack: " + e.getEventId() + " related to message: "
						+ ((LamportAck) e).getIdRelatedMessage());
				ackCountHandler((LamportAck) e);
			}
		}
	}

	/**
	 * Updates the lamport clock according to the rules
	 * 
	 * @param e
	 */
	private synchronized void lamportClockUpdate(Event e) {
		logicalClock = Math.max(logicalClock, e.getLogicalClock()) + 1;
	}

	/**
	 * Handles a message received, so it writes the message in the queue and send
	 * the ack
	 * 
	 * @param m
	 */
	private synchronized void messageHandler(Message m) {
		writeQueue.put(m);
		// printQueue();
		ackCountHandler(new LamportAck(processId, logicalClock, m.getLogicalClock(), m.getProcessId()));
		exec.submit(new MonitorAck(m));
	}

	/**
	 * Adds the received ack to the count
	 * 
	 * @param a
	 */
	private synchronized void ackCountHandler(LamportAck a) {
		Integer count = ackCount.get(a.getIdRelatedMessage());
		if (count == null) {
			ackCount.put(a.getIdRelatedMessage(), 1);
		} else {
			count++;
			ackCount.replace(a.getIdRelatedMessage(), count);
			if (count == groupSize) {
				synchronized (lock) {
					lock.notify();
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			receiveEvent();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The thread of that class polls until there are the conditions to send the
	 * ack, and then sends the ack
	 *
	 */
	private class MonitorAck implements Runnable {

		private Message messageToCheck;
		private boolean condition1;
		private boolean condition2;

		public MonitorAck(Message message) {
			messageToCheck = message;
			condition1 = false;
			condition2 = false;
		}

		/**
		 * Sends the ack related to the message
		 * 
		 * @param messageToAck
		 */
		private synchronized void sendAck(Message messageToAck) {
			logicalClock++;
			LamportAck ack = new LamportAck(processId, logicalClock, messageToAck.getLogicalClock(),
					messageToAck.getProcessId());
			reliableChannel.enqueueEvent(ack);
			System.out.println("[SENDING] Ack  " + ack.getEventId() + " for message " + messageToAck.getEventId()
					+ " with element: <" + messageToAck.getDataId() + "," + messageToAck.getIntegerValue() + ">");
		}

		@Override
		public synchronized void run() {
			if (messageToCheck.getProcessId() == processId) {
				sendAck(messageToCheck);
				return;
			}
			while (true) {
				for (Message m : writeQueue) {
					if (m.getLogicalClock() == messageToCheck.getLogicalClock() && m.getProcessId() == processId) {
						condition1 = true;
						break;
					}
				}
				if (condition1 == false) {
					sendAck(messageToCheck);
					return;
				}
				if (condition1 == true) {
					for (Message m : writeQueue) {
						if (m.getLogicalClock() == messageToCheck.getLogicalClock() && m.getProcessId() > processId) {
							condition2 = true;
							break;
						}
					}
				}
				if (condition2 == false) {
					sendAck(messageToCheck);
					return;
				}
				condition1 = false;
				condition2 = false;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Constantly checks if the element at the head of the queue has received all
	 * the acks, if so, it deliver to the server the message
	 */
	private class CheckQueue implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					while (checkQueueHead()) {
						Message m = writeQueue.poll();
						messageDelivered.put(m);
						System.out.println("[DB WRITE] Message: " + m.getEventId() + " with element: <" + m.getDataId() + ","
								+ m.getIntegerValue() + ">");
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

		/**
		 * Returns true if the head of the queue has received all the acks
		 * 
		 * @return
		 */
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
	 * private void printQueue() { System.out.println("Queue:\n"); for (Message m :
	 * writeQueue) { System.out.println("Message: " + m.getEventId()); if (m ==
	 * writeQueue.element()) System.out.println(" (queue head)\n"); else
	 * System.out.println("\n"); } }
	 */

	/**
	 * Defines the correct criterion to order the queue
	 */
	private class Order implements Comparator<Message> {

		@Override
		public synchronized int compare(Message o1, Message o2) {
			if (o1.getLogicalClock() < o2.getLogicalClock())
				return -1;
			else if (o1.getLogicalClock() > o2.getLogicalClock())
				return 1;
			else if (o1.getProcessId() > o2.getProcessId())
				return 1;
			else
				return -1;
		}

	}

}