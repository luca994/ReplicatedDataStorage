package communication;

public class Nack extends Event {
	
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
	
	public void setTargetSequenceNumber(Integer targetClock) {
		this.targetSequenceNumber = targetClock;
	}
	public Nack(int PID, int logClock) {
		super(PID, logClock);
	}

}
