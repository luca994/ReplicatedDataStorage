package communication;

public class LamportAck extends Event {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8666991708666659089L;
	
	private Integer logicalClockMessage;
	private Integer processIdMessage;

	public LamportAck(int PID, int logClock, int clockMessage, int processIdMessage) {
		super(PID, logClock);
		logicalClockMessage = clockMessage;
		this.processIdMessage = processIdMessage;
	}
	
	public String getIdRelatedMessage() {
		return logicalClockMessage.toString()+"."+processIdMessage.toString();
	}

}
