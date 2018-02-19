package client;

import java.rmi.RemoteException;
import java.util.Scanner;

import server.ServerRemoteInterface;

public class Client {
	
	ServerRemoteInterface server;
	
	public Client(ServerRemoteInterface stub) {
		server = stub;
	}
	
	public void request() throws RemoteException {
		Scanner scan = new Scanner(System.in);
		String input = "";
		while(!(input.equals("q") || input.equals("4"))) {
			System.out.println("Inserisci l'operazione:\n1)read\n2)write\n3)print\n4)q (per uscire)\n");
			input = scan.nextLine();
			switch(input){
			case "read":
			case "1":
				System.out.println("Inserisci l'id del dato che vuoi leggere:\n");
				int idRead = scan.nextInt();
				System.out.println("Valore="+server.read(idRead)+"\n");
				scan = new Scanner(System.in);
				break;
			case "write":
			case "2":
				System.out.println("inserisci l'id del dato che vuoi modificare/aggiungere:\n");
				int idWrite = scan.nextInt();
				System.out.println("Inserisci il valore del dato");
				int value = scan.nextInt();
				server.write(idWrite, value);
				scan = new Scanner(System.in);
				break;
			case "print":
			case "3":
				server.print();
				System.out.println("Database printed on the server\n");
				scan = new Scanner(System.in);
			case "q":
			case "4":
				System.out.println("Uscita");
				break;
			default:
				System.out.println("Input non corretto\n");
			}
		}
		scan.close();
	}
}
