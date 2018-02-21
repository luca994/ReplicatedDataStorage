package communication;

public class Message extends Event {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2736291415009713801L;
	
	private int dataId;
	private int integerValue;
	
	public Message(int PID, int logClock, int dId, int intVal) {
		super(PID, logClock);
		dataId=dId;
		integerValue=intVal;
	}

	@Override
	public String toString() {
		return "Message [eventId=" + eventId + "]";
	}

	public int getDataId() {
		return dataId;
	}

	public int getIntegerValue() {
		return integerValue;
	}
}
