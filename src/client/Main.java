package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws UnknownHostException, IOException {
		Scanner scan = new Scanner(System.in);
		System.out.println("Inserisci l'indirizzo ip del server:\n");
		String ipAddress = scan.nextLine();
		System.out.println("Inserisci la porta del server:\n");
		int port = scan.nextInt();
		Socket socketClient = new Socket(ipAddress, port);
		Client client = new Client(socketClient);
		
		while(true) {
			//inserisci cosa vuoi fare (inviare o ricevere messaggi)
		}

	}

}
