package communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class ReliableChannel {

	private static final int MULTICAST_PORT = 10000;
	private static final String MULTICAST_ADDRESS = "224.0.0.1";
	private static final long RETRANSMISSION_TIME = 1000;
	private static final int PACKET_LOSS_PERCENTAGE = 0;

	private int processId;
	private int groupSize;

	private BlockingQueue<Event> eventReceivedQueue;
	private BlockingQueue<Event> eventToSend;

	private MulticastSocket multicastSocket;
	private int currentSequenceNumber;
	private Semaphore sem;

	/**
	 * This variable is used as a mutex
	 */
	private Object lock;

	/**
	 * This is the list of eventId of all the events received
	 */
	private List<String> eventReceived;

	/**
	 * The key of the element represents the sequenceNumber, the value is the
	 * corresponding timer
	 * 
	 */
	private ConcurrentMap<Integer, Timer> timers;

	/**
	 * The key of the element represents the sequenceNumber, the value is the
	 * corresponding message
	 * 
	 */
	private ConcurrentMap<Integer, Event> historyBuffer;

	/**
	 * The key of the element represents the sequenceNumber, the value is the
	 * corresponding number of acks received
	 * 
	 */
	private ConcurrentMap<Integer, Integer> acksReceived;

	/**
	 * Constructor that initializes all the parameters except the multicast
	 * connection
	 * 
	 * @param processId
	 * @param groupLength
	 * @param lamportAlgorithm
	 */
	public ReliableChannel(int processId, int groupLength, BlockingQueue<Event> eventReceivedQueue) {
		this.eventReceivedQueue = eventReceivedQueue;
		this.eventToSend = new LinkedBlockingQueue<Event>();
		this.groupSize = groupLength;
		this.processId = processId;
		this.timers = new ConcurrentHashMap<Integer, Timer>();
		this.acksReceived = new ConcurrentHashMap<Integer, Integer>();
		this.historyBuffer = new ConcurrentHashMap<Integer, Event>();
		this.currentSequenceNumber = 0;
		this.eventReceived = new ArrayList<String>();
		this.sem = new Semaphore(0);
		this.lock = new Object();
		try {
			startConnection(MULTICAST_PORT, MULTICAST_ADDRESS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the multicast connection with other hosts
	 * 
	 * @param multicastPort
	 * @param multicastAddress
	 * @throws IOException
	 */
	public void startConnection(int multicastPort, String multicastAddress) throws IOException {
		multicastSocket = new MulticastSocket(multicastPort);
		multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
		new Thread(new Receiver()).start();
		new Thread(new Sender()).start();
	}

	/**
	 * Enqueues an event to be sent
	 * 
	 * @param e
	 */
	public void enqueueEvent(Event e) {
		try {
			eventToSend.put(e);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Set sequenceNumber to the object, then sends it to the multicast group, it
	 * adds the message to the historyBuffer then returns. The limit for a datagram
	 * packet is set to 128 bytes.
	 * 
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 */
	private synchronized void sendMessage(Event msg) {
		boolean ack = (msg instanceof Ack);
		if (msg.getProcessId() != processId)
			throw new IllegalArgumentException("ERROR! You are attempting to send a message with wrong process id");
		if (!ack) {
			updateSequenceNumber(msg);
			historyBuffer.put(currentSequenceNumber, msg);
			acksReceived.put(currentSequenceNumber, 0);
			Timer timer = new Timer();
			timers.put(currentSequenceNumber, timer);
			timer.schedule(new Retransmit(currentSequenceNumber), RETRANSMISSION_TIME);
		}

		if (new Random().nextInt(100) >= 100 - PACKET_LOSS_PERCENTAGE)
			return;

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
			ObjectOutput out = null;
			out = new ObjectOutputStream(bos);
			out.writeObject(msg);
			out.flush();
			byte[] bytes = bos.toByteArray();
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(MULTICAST_ADDRESS),
					MULTICAST_PORT);
			multicastSocket.send(packet);
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates the sequence number and assigns the new one to the event in input
	 * 
	 * @param msg
	 */
	private synchronized void updateSequenceNumber(Event msg) {
		currentSequenceNumber++;
		msg.setSequenceNumber(currentSequenceNumber);
	}

	/**
	 * Dispatches the events to the right handler method
	 * 
	 * @param e
	 */
	private void dispatcherReceivedEvent(Event e) {
		if (e instanceof Ack) {
			manageAckReceived((Ack) e);
			return;
		} else {
			manageMessageReceived(e);
			return;
		}
	}

	/**
	 * Manages the event of receiving an acknowledgement. It increments the counter
	 * of acks for that message and if it's the last ack it stops the related timer
	 * and remove the message from the history buffer
	 * 
	 * @param ack
	 */
	private void manageAckReceived(Ack ack) {
		if (ack.getProcessId() == processId || ack.getTargetProcId() != processId)
			return;
		else {
			synchronized (lock) {
				int targetSN = ack.getTargetSequenceNumber();
				Integer numberOfAcks = acksReceived.get(targetSN);
				if (numberOfAcks == null)
					return;
				numberOfAcks++;
				acksReceived.replace(targetSN, numberOfAcks);
				if (numberOfAcks == groupSize - 1) {
					timers.get(targetSN).cancel();
					timers.remove(targetSN);
					historyBuffer.remove(targetSN);
					acksReceived.remove(targetSN);
					sem.release();
				}
			}
		}
	}

	/**
	 * Sends a multicast ack to the members of the multicast and puts the message,
	 * if it is new, in the event received queue
	 * 
	 * @param message
	 */
	private void manageMessageReceived(Event message) {
		int msgSN = message.getSequenceNumber();
		int msgPid = message.getProcessId();
		if (message.getProcessId() == processId)
			return;
		else {
			Ack ack = new Ack(processId);
			ack.setTargetSequenceNumber(msgSN);
			ack.setTargetProcId(msgPid);
			sendMessage(ack);
			if (newEvent(message)) {
				eventReceived.add(message.getEventId());
				wipeEventReceived(message);
				try {
					eventReceivedQueue.put(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Checks if the message received is new or it is been already received
	 * 
	 * @param message
	 * @return
	 */
	private boolean newEvent(Event message) {
		return !eventReceived.contains(message.eventId);
	}

	/**
	 * It must be called when a new event is received. Removes the last event
	 * received from the same process with a lesser logical clock
	 * 
	 * @param lastEventReceived
	 */
	private void wipeEventReceived(Event lastEventReceived) {
		String toRemove = null;
		for (String s : eventReceived) {
			String[] logClockAndPid = s.split("\\.");
			Integer logClock = Integer.parseInt(logClockAndPid[0]);
			Integer pid = Integer.parseInt(logClockAndPid[1]);
			if (logClock < lastEventReceived.getLogicalClock() && pid == lastEventReceived.getProcessId()) {
				toRemove = s;
				break;
			}
		}
		if (toRemove != null)
			eventReceived.remove(toRemove);
	}

	/**
	 * Is the task used to schedule the retransmission of a message
	 *
	 */
	private class Retransmit extends TimerTask {
		private int sequenceNumber;

		public Retransmit(int sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
		}

		@Override
		public void run() {
			synchronized (lock) {
				if (acksReceived.get(sequenceNumber) == null)
					return;
				Event retransmission = historyBuffer.get(sequenceNumber);
				historyBuffer.remove(sequenceNumber);
				acksReceived.remove(sequenceNumber);
				sendMessage(retransmission);
			}
		}
	}

	/**
	 * Is the Class runnable that sends the message that has been enqueued by
	 * following a FIFO logic
	 *
	 */
	private class Sender implements Runnable {

		@Override
		public void run() {
			while (true) {
				Event e;
				try {
					e = eventToSend.take();
					sendMessage(e);
					sem.acquire();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Is the Class runnable that receives the events
	 *
	 */
	private class Receiver implements Runnable {
		@Override
		public void run() {
			byte[] bytesBuffer = new byte[512];
			DatagramPacket packet = new DatagramPacket(bytesBuffer, bytesBuffer.length);
			try {
				while (true) {
					multicastSocket.receive(packet);
					ByteArrayInputStream bis = new ByteArrayInputStream(bytesBuffer);
					ObjectInput in = null;
					in = new ObjectInputStream(bis);
					Event event = (Event) in.readObject();
					dispatcherReceivedEvent(event);
					in.close();
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}
