package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class MainServer{

	public static void main(String[] args) {
		try {
			Scanner scan = new Scanner(System.in);
			System.out.println("Inserisci la porta:\n");
			int port = scan.nextInt();
			Server server = new Server();
			LocateRegistry.createRegistry(port);
			ServerRemote serverMethods = new ServerRemote(server);
			Naming.rebind("ServerRemote", serverMethods);
			System.out.println("Registro pronto\n");
			scan.close();
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
		}		
	}
}
