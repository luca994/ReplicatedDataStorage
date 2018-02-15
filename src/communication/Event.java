package communication;

import java.util.Random;

public class Event {
	
	protected int processId;
	protected int logicalClock;
	protected int eventId;
	protected String senderIp;
	
	public Event(int PID, int logClock) {
		Random rnd= new Random();
		processId=PID;
		logicalClock=logClock;
		eventId=rnd.nextInt();
	}
}
