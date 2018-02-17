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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ReliableChannel {

	private Timer timer;
	private MulticastSocket multicastSocket;
	private ExecutorService threadPool;
	private Integer groupLength;
	private final Integer processId;

	/*
	 * The key of the element represents the process id, the value is the last SN
	 * received from that process
	 */
	private ConcurrentMap<Integer, Integer> currentSN;

	/*
	 * The key of the element represents the transmission sequence number, the value
	 * is the corresponding message
	 * 
	 */
	private ConcurrentMap<Integer, Event> historyBuffer;

	/*
	 * The key of the element represents the transmission sequence number, the value
	 * is the number of acknowledgements received
	 * 
	 */
	private ConcurrentMap<Integer, Integer> ackReceived;

	public ReliableChannel(int processId, int groupLength) {
		this.groupLength = groupLength;
		this.processId = processId;
		this.historyBuffer = new ConcurrentHashMap<Integer, Event>();
		this.currentSN = new ConcurrentHashMap<Integer, Integer>(groupLength);
		this.ackReceived = new ConcurrentHashMap<Integer, Integer>();
		for (int i = 1; i <= groupLength; i++) {
			currentSN.put(i, 0);
		}
	}

	public void startConnection(int multicastPort, String multicastAddress) throws IOException {
		multicastSocket = new MulticastSocket(multicastPort);
		multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
	}

	/**
	 * Sends a Message object to the multicast group, then returns
	 * We have added a limit of 128 bytes for a datagram packet
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 */
	private synchronized void sendMessage(Event msg) throws IOException {
		Integer sn = currentSN.get(processId) + 1;
		currentSN.replace(processId, sn);
		msg.setTransmissionSequence(sn);
		historyBuffer.put(sn, msg);
		ackReceived.put(sn, 0);

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
	}
	
	private void dispatcherReceivedEvent(Event e) {
		Integer pid = e.getProcessId();
		Integer sn = currentSN.get(pid);
		currentSN.replace(e.getProcessId, value)
		if(e instanceof Ack) {
			manageAckReceived((Ack)e);
			return;
		}
		if(e instanceof Nack) {
			manageNackReceived((Nack)e);
			return;
		}
		if(e instanceof Message) {
			manageMessageReceived((Message)e);
			return;
		}	
	}
	
	private void manageAckReceived(Ack ack) {
		
	}
	private void manageNackReceived(Nack nack) {}
	private void manageMessageReceived(Message message) {}
	
	private class Receiver implements Runnable {
		@Override
		public void run () {
			byte [] bytesBuffer = new byte[128];
			DatagramPacket packet = new DatagramPacket (bytesBuffer,bytesBuffer.length);
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

