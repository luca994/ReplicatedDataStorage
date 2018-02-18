package communication;

public class Ack extends Event {

	private Integer targetSequenceNumber;
	private Integer targetProcId;
	
	public int getTargetProcId() {
		return (int)targetProcId;
	}

	public int getTargetSequenceNumber() {
		return (int)targetSequenceNumber;
	}

	public void setTargetProcId(Integer targetProcId) {
		this.targetProcId = targetProcId;
	}
	
	public void setTargetSequenceNumber(Integer targetSequenceNumber) {
		this.targetSequenceNumber = targetSequenceNumber;
	}
	
	public Ack(int PID, int logClock) {
		super(PID, logClock);
	}

}
