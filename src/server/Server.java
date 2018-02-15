package server;

import java.net.Socket;
import java.util.Map;
import java.util.Queue;

import communication.Message;

public class Server {
	
	private Socket socketClient;
	private Map<Integer, Integer> database;
	private Queue<Message> messageQueue;
	//socket UDP, bisogna vedere come inizializzarlo
	private int processId;
	private int logicalClock;
	private Sender sender;
	private Receiver receiver;
	
	public Server(Socket sock) {
		socketClient=sock;
		//oltre a gestire la connessione con il client inizializzi il socket udp
	}
	
	public Server() {
		//inizializzi solo il socket per l'udp
	}
	
	public int read(int dataId) {
		return 0; //per non far dare l'errore
	}
	
	public void write(int dataId, int value) {
		
	}
	
	
	
}
