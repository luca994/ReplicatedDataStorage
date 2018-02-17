package communication;


public class Event {
	
	protected Integer processId;
	protected Integer logicalClock;
	protected String eventId;
	private Integer transmissionSN; /* Sequence number used for enumerate the transmission */
	
	public int getTransmissionSequence() {
		return (int)transmissionSN;
	}

	public void setTransmissionSequence(Integer transmissionSequence) {
		this.transmissionSN = transmissionSequence;
	}

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
