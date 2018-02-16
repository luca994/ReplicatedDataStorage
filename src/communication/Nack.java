package communication;

public class Nack extends Event {

	public Nack(int PID, int logClock) {
		super(PID, logClock);
	}

}
