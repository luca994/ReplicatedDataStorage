package communication;

public class Message extends Event {
	
	private int dataId;
	private int integerValue;
	
	public Message(int PID, int logClock, int dId, int intVal) {
		super(PID, logClock);
		dataId=dId;
		integerValue=intVal;
	}
}
