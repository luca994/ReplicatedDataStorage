package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ServerRemote extends UnicastRemoteObject implements ServerRemoteInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 894730953553395078L;
	
	Server server;

	protected ServerRemote(Server server) throws RemoteException {
		super();
		this.server=server;
	}

	@Override
	public int read(int dataId) throws RemoteException {
		return server.getValue(dataId);
	}

	@Override
	public void write(int dataId, int integerValue) throws RemoteException {
		server.write(dataId, integerValue);
	}

}
