package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRemoteInterface extends Remote{

	String read(int dataId) throws RemoteException;
	void write(int dataId, int integerValue) throws RemoteException;
	void print() throws RemoteException;
	
}
