package communication;

public class Ack extends Event {

	private Integer targetClock;
	private Integer targetProcId;
	
	public int getTargetProcId() {
		return (int)targetProcId;
	}

	public int getTargetClock() {
		return (int)targetClock;
	}

	public void setTargetProcId(Integer targetProcId) {
		this.targetProcId = targetProcId;
	}
	
	public void setTargetClock(Integer targetClock) {
		this.targetClock = targetClock;
	}
	
	public Ack(int PID, int logClock) {
		super(PID, logClock);
	}

}
