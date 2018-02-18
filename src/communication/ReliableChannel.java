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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReliableChannel {

	private MulticastSocket multicastSocket;
	private Thread receiverThread;
	private Integer groupLength;
	private final Integer processId;
	private LamportAlgorithm lamportAlgorithm;

	/**
	 * The key of the element represents the sequenceNumber, the value is the
	 * corresponding timer
	 * 
	 */
	private ConcurrentMap<Integer, Timer> timers;
	/**
	 * The key of the element represents the process id, the value is the last
	 * sequenceNumber received from that process
	 */
	private ConcurrentMap<Integer, Integer> currentSequenceNumber;

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
		this.lamportAlgorithm = lamportAlgorithm;
		this.groupLength = groupLength;
		this.processId = processId;
		this.timers = new ConcurrentHashMap<Integer, Timer>();
		this.acksReceived = new ConcurrentHashMap<Integer, Integer>();
		this.historyBuffer = new ConcurrentHashMap<Integer, Event>();
		this.currentSequenceNumber = new ConcurrentHashMap<Integer, Integer>(groupLength);
		this.receiverThread = new Thread(new Receiver());
		for (int i = 1; i <= groupLength; i++) {
			currentSequenceNumber.put(i, 0);
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
		Integer sequenceNumber = currentSequenceNumber.get(processId);
		if (msg.getSequenceNumber() == 0 && !ack) {
			sequenceNumber++;
			msg.setSequenceNumber(sequenceNumber);
			currentSequenceNumber.replace(processId, sequenceNumber);
		}
		Integer msgSN = msg.getSequenceNumber();
		if (sequenceNumber < msgSN)
			currentSequenceNumber.replace(processId, sequenceNumber);
		if (!ack) {
			historyBuffer.put(sequenceNumber, msg);
			acksReceived.put(sequenceNumber, 0);
			Timer timer = new Timer();
			timers.put(sequenceNumber, timer);
			// TODO scegliere un tempo adatto ora ho messo 2 secondi
			timer.schedule(new Retransmit(sequenceNumber),2000);
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
			ObjectOutput out = null;
			out = new ObjectOutputStream(bos);
			out.writeObject(msg);
			out.flush();
			byte[] bytes = bos.toByteArray();
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, multicastSocket.getInetAddress(),
					multicastSocket.getLocalPort());
			multicastSocket.send(packet);
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method is called by the receiver to dispatch the event to the right method
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
	 * this method manage the event of receiving an acknowledgement.
	 * It increments the counter of acks for that message and if it's the last ack it stops the related
	 * timer and remove the message from the history buffer
	 * @param ack
	 */
	private synchronized void manageAckReceived(Ack ack) {
		if (ack.getProcessId() == processId || ack.getTargetProcId() != processId)
			return;
		else {
			Integer targetSN = ack.getTargetSequenceNumber();
			Integer numberOfAcks = acksReceived.get(targetSN);
			numberOfAcks++;
			acksReceived.replace(targetSN, numberOfAcks);
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
	/**
	 * this method sends a multicast ack to the members of the multicast
	 * and calls the function lamport algorith of the upper layer
	 * @param message
	 */
	private void manageMessageReceived(Event message) {
		Integer msgSN = message.getSequenceNumber();
		Integer msgPid = message.getProcessId();
		if (message.getProcessId() == processId)
			return;
		else {
			checkAndManageSequenceNumber(message);
			Ack ack = new Ack(processId, 0);
			ack.setTargetSequenceNumber(msgSN);
			ack.setTargetProcId(msgPid);
			sendMessage(ack);
			lamportAlgorithm.receiveEvent(message); /*
													 * Chiamo receiveEvent ogni volta che ricevo un messaggio anche se è
													 * una ritrasmissione. Quindi ogni volta che lo ricevo io, non è
													 * detto che sia stato ricevuto da tutti
													 */
		}
	}

	/**
	 * This method updates the sequence number corresponding to the process of the event received.
	 * 
	 * @param e
	 */
	private void checkAndManageSequenceNumber(Event e) {
		Integer epid = e.getProcessId();
		Integer eSN = e.getSequenceNumber();
		Integer currESN = currentSequenceNumber.get(epid);
		if (currESN < eSN)
			currentSequenceNumber.replace(epid, eSN);
	}

	/**
	 * this is the task used to schedule the retransmission of a message
	 * @author luca
	 *
	 */
	private class Retransmit extends TimerTask {
		private Integer sequenceNumber;

		public Retransmit(Integer sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
		}

		@Override
		public void run() {
			sendMessage(historyBuffer.get(sequenceNumber));
		}
	}
	/**
	 * this is the runnable class used to create the object that receives the multicast messages
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
