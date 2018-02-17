package communication;

public class Nack extends Event {
	
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
	public Nack(int PID, int logClock) {
		super(PID, logClock);
	}

}
