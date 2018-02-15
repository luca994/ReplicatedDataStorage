package client;

import java.net.Socket;

public class Client {

	Socket socket;
	
	public Client(Socket socket) {
		this.socket=socket;
	}
	
	public void remoteWrite(int dataId, int value) {
		
	}
	
	public int remoteRead(int dataId) {
		return 0; //per non far dare l'errore
	}
	
}
