package communication;


public class Event {
	
	protected Integer processId;
	protected Integer logicalClock;
	protected String eventId;
	
	public Event(int PID, int logClock) {
		processId=PID;
		logicalClock=logClock;
		eventId=logicalClock.toString()+"."+processId.toString();
	}
	
	public String getEventId() {
		return eventId;
	}
	
	public int getProcessId() {
		return processId.intValue();
	}
	
	public int getLogicalClock() {
		return logicalClock.intValue();
	}
}
