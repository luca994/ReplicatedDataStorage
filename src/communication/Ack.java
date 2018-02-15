package communication;

public class Ack extends Event {

	public Ack(int PID, int logClock) {
		super(PID, logClock);
	}

}
