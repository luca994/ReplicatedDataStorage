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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class ReliableChannel {

	private Timer timer;
	private MulticastSocket multicastSocket;
	private ExecutorService threadPool;
	private Integer groupLength;
	private final Integer processId;
	private LamportAlgorithm lamportAlgorithm;

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

	public ReliableChannel(int processId, int groupLength, LamportAlgorithm lamportAlgorithm) {
		this.lamportAlgorithm = lamportAlgorithm;
		this.groupLength = groupLength;
		this.processId = processId;
		this.historyBuffer = new ConcurrentHashMap<Integer, Event>();
		this.currentClock = new ConcurrentHashMap<Integer, Integer>(groupLength);
		for (int i = 1; i <= groupLength; i++) {
			currentClock.put(i, 0);
		}
	}

	public void startConnection(int multicastPort, String multicastAddress) throws IOException {
		multicastSocket = new MulticastSocket(multicastPort);
		multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
	}

	private void incrementSNandManageHistory() {

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
	public synchronized void sendMessage(Event msg, boolean retransmission) {
		if (!retransmission) {
			Integer clock = currentClock.get(processId) + 1;
			currentClock.replace(processId, clock);
			msg.setLogicalClock(clock);
			historyBuffer.put(clock, msg);
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
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		for (int i = currEclock; i < eClock - 1; i++) {
			Nack nack = new Nack(processId, 0);
			nack.setTargetClock(i);
			nack.setTargetProcId(epid);
			sendMessage(nack, true);
		}
		currentClock.replace(epid, eClock);
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
	}

	private void manageNackReceived(Nack nack) {
		if (nack.getProcessId() == processId)
			sendMessage(historyBuffer.get(nack.getTargetClock()), true);
		else
			return;
	}

	private void manageMessageReceived(Message message) {
		Integer msgClock = message.getLogicalClock();
		Integer msgPid = message.getProcessId();
		if (msgClock <= currentClock.get(msgPid)
				|| message.getProcessId() == processId)
			return;
		else {
			checkAndManageClock(message);
			Ack ack = new Ack(processId, 0);
			ack.setTargetClock(msgClock);
			ack.setTargetProcId(msgPid);
			sendMessage(ack,true);
		}
	}

	private void manageAckReceived(Ack ack) {
		if(ack.getProcessId() == processId || ack.getTargetProcId()!= processId)
			return;
		else {
			
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
