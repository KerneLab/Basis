package org.kernelab.basis.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketKit
{
	private Socket	socket;

	public SocketKit()
	{
		this(null);
	}

	public SocketKit(Socket socket)
	{
		this.setSocket(socket);
	}

	public Socket getSocket()
	{
		return socket;
	}

	@SuppressWarnings("unchecked")
	public <T> T readObject() throws IOException
	{
		T object = null;

		ObjectInputStream objectReader = new ObjectInputStream(this.getSocket().getInputStream());

		try
		{
			object = (T) objectReader.readObject();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		return object;
	}

	public void sendObject(Object object) throws IOException
	{
		ObjectOutputStream objectSender = new ObjectOutputStream(this.getSocket().getOutputStream());

		objectSender.writeObject(object);
	}

	public void setSocket(Socket socket)
	{
		this.socket = socket;
	}
}
