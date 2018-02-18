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
	 * The key of the element represents the logicalClock, the value is the
	 * corresponding timer
	 * 
	 */
	private ConcurrentMap<Integer, Timer> timers;
	/**
	 * The key of the element represents the process id, the value is the last
	 * logicalclock received from that process
	 */
	private ConcurrentMap<Integer, Integer> currentClock;

	/**
	 * The key of the element represents the logicalClock, the value is the
	 * corresponding message
	 * 
	 */
	private ConcurrentMap<Integer, Event> historyBuffer;

	/**
	 * The key of the element represents the logicalClock, the value is the
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
		this.currentClock = new ConcurrentHashMap<Integer, Integer>(groupLength);
		this.receiverThread = new Thread(new Receiver());
		for (int i = 1; i <= groupLength; i++) {
			currentClock.put(i, 0);
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
	 * Set logicalClock to the object, then sends it to the multicast group, it adds
	 * the message to the historyBuffer then returns. We have added a limit of 128
	 * bytes for a datagram packet.
	 * 
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 */
	public synchronized void sendMessage(Event msg, boolean ackOrNack) {
		Integer clock = currentClock.get(processId);
		Integer msgClock = msg.getLogicalClock();
		Timer timer = timers.get(clock);
		if (clock < msgClock)
			currentClock.replace(processId, clock);
		if (!ackOrNack) {
			historyBuffer.put(clock, msg);
			acksReceived.put(clock, 0);
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
			timer.schedule(new Retransmit(clock), 1000);
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
		Integer targetClock = ack.getTargetClock();
		if (ack.getProcessId() == processId || ack.getTargetProcId() != processId)
			return;
		else {
			Integer numberOfAcks = acksReceived.get(targetClock);
			numberOfAcks++;
			acksReceived.replace(targetClock, numberOfAcks);
			if (numberOfAcks == groupLength - 1) {
				timers.get(targetClock).cancel();
				timers.remove(targetClock);
				System.out.println("Message with clock: " + targetClock + " received");
				historyBuffer.remove(targetClock);
				acksReceived.remove(targetClock);
			}
		}
	}

	private void manageNackReceived(Nack nack) {
		Integer targetClock = nack.getTargetClock();
		if (nack.getProcessId() == processId) {
			timers.get(targetClock).cancel();
			sendMessage(historyBuffer.get(targetClock), false);
		} else
			return;
	}

	private void manageMessageReceived(Message message) {
		Integer msgClock = message.getLogicalClock();
		Integer msgPid = message.getProcessId();
		if (message.getProcessId() == processId)
			return;
		else {
			checkAndManageClock(message);
			Ack ack = new Ack(processId, 0);
			ack.setTargetClock(msgClock);
			ack.setTargetProcId(msgPid);
			sendMessage(ack, true);
			if(msgClock > currentClock.get(msgPid))
				lamportAlgorithm.receiveEvent(message);
		}
	}
	
	//Overload
	private void manageMessageReceived(LamportAck lamportAck) {
		Integer msgClock = lamportAck.getLogicalClock();
		Integer msgPid = lamportAck.getProcessId();
		if (lamportAck.getProcessId() == processId)
			return;
		else {
			checkAndManageClock(lamportAck);
			Ack ack = new Ack(processId, 0);
			ack.setTargetClock(msgClock);
			ack.setTargetProcId(msgPid);
			sendMessage(ack, true);
			if(msgClock > currentClock.get(msgPid))
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
	private void checkAndManageClock(Event e) {
		Integer epid = e.getProcessId();
		Integer eClock = e.getLogicalClock();
		Integer currEclock = currentClock.get(epid);
		if(currEclock < eClock) {
		for (int i = currEclock; i < eClock - 1; i++) {
			Nack nack = new Nack(processId, 0);
			nack.setTargetClock(i);
			nack.setTargetProcId(epid);
			sendMessage(nack, true);
		}
		currentClock.replace(epid, eClock);
		}
		else
			return;
	}

	private class Retransmit extends TimerTask {
		private Integer clock;

		public Retransmit(Integer clock) {
			this.clock = clock;
		}

		@Override
		public void run() {
			sendMessage(historyBuffer.get(clock), false);
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
