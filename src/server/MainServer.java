package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Scanner;

public class MainServer {

	public static void main(String[] args) {
		try {
			Scanner scan = new Scanner(System.in);
			System.out.println("Insert the port:\n");
			int port = scan.nextInt();
			int groupLength;
			do {
				System.out.println("Insert the group length:\n");
				groupLength = scan.nextInt();
				if (groupLength <= 1) {
					System.out.println("Invalid Choice!");
					scan = new Scanner(System.in);
				}
			} while (groupLength <= 1);
			int processId;
			do {
				System.out.println("Insert your process Id (0 random choice):\n");
				processId = scan.nextInt();
				if (processId < 0) {
					System.out.println("Invalid Choice!");
					scan = new Scanner(System.in);
				}
			} while (processId < 0);
			Server server;
			if (processId == 0)
				server = new Server(Math.abs(new Random().nextInt()), groupLength);
			else
				server = new Server(processId, groupLength);
			Registry registry = LocateRegistry.createRegistry(port);
			ServerRemote serverMethods = new ServerRemote(server);
			registry.bind("ServerRemote", serverMethods);
			System.out.println("Register Ready!\n");
			scan.close();
			server.updateDatabase();
		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
	}
}
