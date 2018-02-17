package communication;

public class LamportAck extends Event {

	public LamportAck(int PID, int logClock) {
		super(PID, logClock);
	}

}
