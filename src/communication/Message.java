package communication;

public class Message extends Event {
	
	private int dataId;
	private int integerValue;
	private String senderIp;
	
	public Message(int PID, int logClock, int dId, int intVal, String ip) {
		super(PID, logClock);
		dataId=dId;
		integerValue=intVal;
		senderIp=ip;
	}
}
