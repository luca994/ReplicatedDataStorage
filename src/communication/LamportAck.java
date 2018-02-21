package communication;

public class LamportAck extends Event {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8666991708666659089L;
	
	private Integer logicalClockMessage;

	public LamportAck(int PID, int logClock, int clockMessage) {
		super(PID, logClock);
		logicalClockMessage = clockMessage;
	}
	
	public String getIdRelatedMessage() {
		return logicalClockMessage.toString()+"."+processId.toString();
	}

}
