package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerStart implements Runnable {
	
	private int port;
	private Socket socketClient;
	
	public ServerStart(int port, Socket socketClient) {
		this.port=port;
		this.socketClient=socketClient;
	}

	@Override
	public void run() {
		try {
			ServerSocket acceptSocket = new ServerSocket(port);
			socketClient = acceptSocket.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
