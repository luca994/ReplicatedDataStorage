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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReliableChannel {

	private static final int MULTICAST_PORT = 10000;
	private static final String MULTICAST_ADDRESS = "224.0.0.1";

	private MulticastSocket multicastSocket;
	private Thread receiverThread;
	private int groupLength;
	private int currentSequenceNumber;
	private final int processId;
	private LamportAlgorithm lamportAlgorithm;
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
	 * corresponding acks received
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
	public ReliableChannel(int processId, int groupLength, LamportAlgorithm lamportAlgorithm) {
		this.receiverThread = new Thread(new Receiver());
		try {
			startConnection(MULTICAST_PORT, MULTICAST_ADDRESS);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.lamportAlgorithm = lamportAlgorithm;
		this.groupLength = groupLength;
		this.processId = processId;
		this.timers = new ConcurrentHashMap<Integer, Timer>();
		this.acksReceived = new ConcurrentHashMap<Integer, Integer>();
		this.historyBuffer = new ConcurrentHashMap<Integer, Event>();
		this.currentSequenceNumber = 0;
		this.eventReceived = new ArrayList<String>();
		this.lock = new Object();
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
		receiverThread.start();
	}

	/**
	 * Set sequenceNumber to the object, then sends it to the multicast group, it
	 * adds the message to the historyBuffer then returns. We have added a limit of
	 * 128 bytes for a datagram packet.
	 * 
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 */
	public synchronized void sendMessage(Event msg) {
		boolean ack = (msg instanceof Ack);
		if (msg.getProcessId() != processId)
			throw new IllegalArgumentException("ERROR! You are attempting to send a message with wrong process id");
		if (!ack) {
			updateSequenceNumber(msg);
			historyBuffer.put(currentSequenceNumber, msg);
			acksReceived.put(currentSequenceNumber, 0);
			Timer timer = new Timer();
			timers.put(currentSequenceNumber, timer);
			// TODO scegliere un tempo adatto ora ho messo 2 secondi
			timer.schedule(new Retransmit(currentSequenceNumber), 2000);
		}
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
	 * this method updates the sequence number and assigns the new one to the event
	 * in input
	 * 
	 * @param msg
	 */
	private synchronized void updateSequenceNumber(Event msg) {
		currentSequenceNumber++;
		msg.setSequenceNumber(currentSequenceNumber);
	}

	/**
	 * this method is called by the receiver to dispatch the event to the right
	 * method
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
	 * this method manage the event of receiving an acknowledgement. It increments
	 * the counter of acks for that message and if it's the last ack it stops the
	 * related timer and remove the message from the history buffer
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
				if (numberOfAcks == null) {
					System.out.println("Big Delay?!");
					return;
				}
				numberOfAcks++;
				acksReceived.replace(targetSN, numberOfAcks);
				System.out.println("Ack from process " + ack.getProcessId() + " received!");
				if (numberOfAcks == groupLength - 1) {
					timers.get(targetSN).cancel();
					timers.remove(targetSN);
					System.out.println(
							"Message: " + historyBuffer.get(targetSN).getEventId() + " delivered to all the members");
					historyBuffer.remove(targetSN);
					acksReceived.remove(targetSN);
				}
			}
		}
	}

	/**
	 * this method sends a multicast ack to the members of the multicast and calls
	 * the function lamport algorith of the upper layer
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
				eventReceived.add(message.eventId);
				lamportAlgorithm.receiveEvent(message);
			}
		}
	}

	/**
	 * This method checks if the message received is new or it is been already
	 * received
	 * 
	 * @param message
	 * @return
	 */
	private boolean newEvent(Event message) {
		return !eventReceived.contains(message.eventId);
	}

	/**
	 * this is the task used to schedule the retransmission of a message
	 * 
	 * @author luca
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
				System.out.println("Time Expired, retransmission... \nEvent: " + retransmission.eventId);
				historyBuffer.remove(sequenceNumber);
				acksReceived.remove(sequenceNumber);
				sendMessage(retransmission);
			}
		}
	}

	/**
	 * this is the runnable class used to create the object that receives the
	 * multicast messages
	 * 
	 * @author luca
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
