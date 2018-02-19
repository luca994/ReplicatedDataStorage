package communication;

public class LamportAck extends Event {
	
	private Integer logicalClockMessage;

	public LamportAck(int PID, int logClock, int clockMessage) {
		super(PID, logClock);
		logicalClockMessage = clockMessage;
	}
	
	public String getIdRelatedMessage() {
		return logicalClockMessage.toString()+"."+processId.toString();
	}

}
