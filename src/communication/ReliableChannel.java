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
	 * Set sequenceNumber to the object, then sends it to the multicast group, it adds
	 * the message to the historyBuffer then returns. We have added a limit of 128
	 * bytes for a datagram packet.
	 * 
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 */
	public synchronized void sendMessage(Event msg, boolean ackOrNack,boolean retransmission) {
		Integer sequenceNumber = currentSequenceNumber.get(processId);
		if(!retransmission && !ackOrNack) {
			sequenceNumber++;
			msg.setSequenceNumber(sequenceNumber);
			currentSequenceNumber.replace(processId, sequenceNumber);
		}
		Integer msgSN = msg.getSequenceNumber();	
		Timer timer = timers.get(sequenceNumber);
		if (sequenceNumber < msgSN)
			currentSequenceNumber.replace(processId, sequenceNumber);
		if (!ackOrNack) {
			historyBuffer.put(sequenceNumber, msg);
			acksReceived.put(sequenceNumber, 0);
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
			ObjectOutput out = null;
			out = new ObjectOutputStream(bos);
			out.writeObject(msg);
			out.flush();
			byte[] bytes = bos.toByteArray();
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, multicastSocket.getInetAddress(),
					multicastSocket.getLocalPort());
			multicastSocket.send(packet);
			timer = new Timer();
			// TODO scegliere un tempo adatto
			timer.schedule(new Retransmit(sequenceNumber), 1000);
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dispatcherReceivedEvent(Event e) {

		if (e instanceof Ack) {
			manageAckReceived((Ack) e);
			return;
		}
		if (e instanceof Nack) {
			manageNackReceived((Nack) e);
			return;
		}
		if (e instanceof Message) {
			manageMessageReceived((Message) e);
			return;
		}
		if (e instanceof LamportAck) {
			manageMessageReceived((LamportAck) e);
			return;
		}
	}

	private synchronized void manageAckReceived(Ack ack) {
		Integer targetSN = ack.getTargetSequenceNumber();
		if (ack.getProcessId() == processId || ack.getTargetProcId() != processId)
			return;
		else {
			Integer numberOfAcks = acksReceived.get(targetSN);
			numberOfAcks++;
			acksReceived.replace(targetSN, numberOfAcks);
			if (numberOfAcks == groupLength - 1) {
				timers.get(targetSN).cancel();
				timers.remove(targetSN);
				System.out.println("Message with SN: " + targetSN + " received");
				historyBuffer.remove(targetSN);
				acksReceived.remove(targetSN);
			}
		}
	}

	private void manageNackReceived(Nack nack) {
		Integer targetSN = nack.getTargetSequenceNumber();
		if (nack.getProcessId() == processId) {
			timers.get(targetSN).cancel();
			sendMessage(historyBuffer.get(targetSN), false,true);
		} else
			return;
	}

	private void manageMessageReceived(Message message) {
		Integer msgSN = message.getSequenceNumber();
		Integer msgPid = message.getProcessId();
		if (message.getProcessId() == processId)
			return;
		else {
			checkAndManageSequenceNumber(message);
			Ack ack = new Ack(processId, 0);
			ack.setTargetSequenceNumber(msgSN);
			ack.setTargetProcId(msgPid);
			sendMessage(ack, true,false);
			if(msgSN > currentSequenceNumber.get(msgPid))
				lamportAlgorithm.receiveEvent(message);
		}
	}
	
	//Overload
	private void manageMessageReceived(LamportAck lamportAck) {
		Integer msgSN = lamportAck.getSequenceNumber();
		Integer msgPid = lamportAck.getProcessId();
		if (lamportAck.getProcessId() == processId)
			return;
		else {
			checkAndManageSequenceNumber(lamportAck);
			Ack ack = new Ack(processId, 0);
			ack.setTargetSequenceNumber(msgSN);
			ack.setTargetProcId(msgPid);
			sendMessage(ack, true,false);
			if(msgSN > currentSequenceNumber.get(msgPid))
				lamportAlgorithm.receiveEvent(lamportAck);
		}
	}

	/**
	 * This method checks if there are some lost message between the received one
	 * and the last message received from that process and sends the Nacks Then it
	 * updates the sequence number corresponding to the process.
	 * 
	 * @param e
	 */
	private void checkAndManageSequenceNumber(Event e) {
		Integer epid = e.getProcessId();
		Integer eSN = e.getSequenceNumber();
		Integer currESN = currentSequenceNumber.get(epid);
		if(currESN < eSN) {
		for (int i = currESN; i < eSN - 1; i++) {
			Nack nack = new Nack(processId, 0);
			nack.setTargetSequenceNumber(i);
			nack.setTargetProcId(epid);
			sendMessage(nack, true,false);
		}
		currentSequenceNumber.replace(epid, eSN);
		}
		else
			return;
	}

	private class Retransmit extends TimerTask {
		private Integer sequenceNumber;

		public Retransmit(Integer sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
		}

		@Override
		public void run() {
			sendMessage(historyBuffer.get(sequenceNumber), false,true);
		}
	}

	private class Receiver implements Runnable {
		@Override
		public void run() {
			byte[] bytesBuffer = new byte[128];
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
