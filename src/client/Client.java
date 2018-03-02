package client;

import java.rmi.RemoteException;
import java.util.InputMismatchException;
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
		while (!(input.equals("q") || input.equals("4"))) {
			printBorder();
			System.out.println("Choose the operation you want to perform:\n1)read\n2)write\n3)print\n4)q (quit)");
			printBorder();
			input = scan.nextLine();
			switch (input) {
			case "read":
			case "1":
				try {
					System.out.println("Insert the id of the element you want to read:\n");
					int idRead = scan.nextInt();
					System.out.println("Value=" + server.read(idRead) + "\n");
					scan = new Scanner(System.in);
				} catch (InputMismatchException e) {
					System.out.println("Invalid Input!");
					scan = new Scanner(System.in);
				}
				break;
			case "write":
			case "2":
				try {
					System.out.println("Insert the id of the element you want to edit/add:\n");
					int idWrite = scan.nextInt();
					System.out.println("Insert the value");
					int value = scan.nextInt();
					server.write(idWrite, value);
					System.out.println("Written: <"+idWrite+","+value+">");
					scan = new Scanner(System.in);
				} catch (InputMismatchException e) {
					System.out.println("Invalid Input!");
					scan = new Scanner(System.in);
				}
				break;
			case "print":
			case "3":
				server.print();
				System.out.println("Database printed on the server\n");
				scan = new Scanner(System.in);
				break;
			case "q":
			case "4":
				System.out.println("Exit...");
				break;
			default:
				System.out.println("Invalid Input!\n");
			}
		}
		scan.close();
	}
	private void printBorder() {
		System.out.println("=============================================================");
		System.out.flush();
	}
}
