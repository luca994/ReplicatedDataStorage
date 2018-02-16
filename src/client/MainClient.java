package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import server.ServerRemoteInterface;

public class MainClient {

	public static void main(String[] args){
		try {
			Scanner scan = new Scanner(System.in);
			System.out.println("Inserisci l'indirizzo ip del server:\n");
			String ip = scan.nextLine();
			System.out.println("Inserisci la porta del server");
			int port = scan.nextInt();
			Registry registry = LocateRegistry.getRegistry(ip, port);
			ServerRemoteInterface stub = (ServerRemoteInterface) registry.lookup("ServerRemote");
			
			Client client = new Client(stub);
			client.request();
			scan.close();
			
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
		
		
	}

}
