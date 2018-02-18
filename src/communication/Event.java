package communication;


public class Event {
	
	protected Integer processId;
	protected Integer logicalClock;
	protected Integer sequenceNumber;
	protected String eventId;

	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public Event(int PID, int logClock) {
		processId=PID;
		logicalClock=logClock;
		sequenceNumber=0;
		eventId=logicalClock.toString()+"."+processId.toString();
	}
	
	public String getEventId() {
		return eventId;
	}
	
	public int getProcessId() {
		return processId.intValue();
	}
	
	public int getSequenceNumber() {
		return sequenceNumber.intValue();
	}
	public int getLogicalClock() {
		return logicalClock.intValue();
	}
}
