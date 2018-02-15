package server;

import java.net.Socket;
import java.util.Scanner;

public class Main{

	public static void main(String[] args) {
		Socket socketClient = null;
		Scanner scan = new Scanner(System.in);
		System.out.println("Inserisci la porta del server:\n");
		int port = scan.nextInt();
		ServerStart serverStart = new ServerStart(port, socketClient);
		Thread t = new Thread(serverStart);
		t.start();
		
		
	}
}
