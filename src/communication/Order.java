package communication;

import java.util.Comparator;

public class Order implements Comparator<Message>{
	
	@Override
	public int compare(Message o1, Message o2) {
		if (o1.getLogicalClock()<o2.getLogicalClock())
			return -1;
		else if (o1.getLogicalClock()>o2.getLogicalClock())
			return 1;
		else if (o1.getProcessId()>o2.getProcessId())
			return 1;
		else
			return -1;
	}
	
}
