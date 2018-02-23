package communication;

public class Ack extends Event {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5081379629606664819L;
	
	private int targetSequenceNumber;
	private int targetProcId;
	
	public int getTargetProcId() {
		return targetProcId;
	}

	public int getTargetSequenceNumber() {
		return targetSequenceNumber;
	}

	public void setTargetProcId(int targetProcId) {
		this.targetProcId = targetProcId;
	}
	
	public void setTargetSequenceNumber(int targetSequenceNumber) {
		this.targetSequenceNumber = targetSequenceNumber;
	}
	
	public Ack(int PID) {
		super(PID, 0);
	}

}
