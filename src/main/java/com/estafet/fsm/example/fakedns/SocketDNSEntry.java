package com.estafet.fsm.example.fakedns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketDNSEntry extends AbstractDNSEntry {
	
	
	private String 	m_ip = null;
	private String	m_port = null;
	private ServerSocket	m_serverSocket = null;
	private Socket	m_socket = null;
	private BufferedReader reader = null;
	private PrintStream writer = null;
	
	public SocketDNSEntry()
	{
		
	}
	public SocketDNSEntry(String s, String i, String p)
	{
		setServerRoleName(s);
		setIP(i);
		setPort(p);
	}
	
	// ADDITION
	public void deConfigureClient(String m)
	{
		try
		{
			//System.out.println("deConfigureClient(" + m + ") for [" + this +"]");
			send(m);
			getSocket().close();
			setClientRoleName("client");
			setSocket(null);
			//System.out.println("deConfigureClient done" + " with [" + this +"]");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void configureClientConnection(String client)
	{
		try
		{
			//System.out.println("configureClientConnection(" + client + ") for [" + this +"]");
			Socket s = new Socket(getIP(), new Integer(getPort()));
			setSocket(s);
			setClientRoleName(client);
			//System.out.println("configureClientConnection done for " + client + " with [" + this +"]");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void configureClient(String client)
	{
		try {
			//System.out.println("configureClient(" + client + ") for [" + this +"]");

			setClientRoleName(client);
			if (getSocket() == null)
			{
				Socket s = new Socket(getIP(), new Integer(getPort()));
				setSocket(s);
			}
			//System.out.println("configureClient done for " + client + " with [" + this +"]");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void configureServerConnection(String client)
	{
		try {
			//System.out.println("configureServerConnection(" + client + ") for [" + this +"]");

			ServerSocket ss = new ServerSocket(new Integer(getPort()));
			setServerSocket(ss);
			
			// This bit needs to be done before an accept otherwise it waits until accept is answered
			Socket s = getServerSocket().accept();
			setSocket(s);
			// Set the client role name to the role in the first message we can receive
			setClientRoleName(client);
			//System.out.println("configureServerConnection done for " + client + " with [" + this +"]");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void configureServer(String server)
	{
		try {
			//System.out.println("configureServer(" + server + ") for [" + this +"]");

			setServerRoleName(server);
			Socket s = new Socket(getIP(), new Integer(getPort()));
			setSocket(s);
			//System.out.println("configureServer done for " + server + " with [" + this +"]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void setIP(String i) { m_ip = i; }
	public String getIP() { return m_ip; }
	public void setPort(String p) { m_port = p; }
	public String getPort() { return m_port; }
	public void setServerSocket(ServerSocket ss) { m_serverSocket = ss; }
	public ServerSocket getServerSocket() { return m_serverSocket; }
	public void setSocket(Socket s) { m_socket = s; }
	public Socket getSocket() { return m_socket; }
	
	public void send(String message)
	{
		try
		{
			if (writer == null)
			{
				//System.out.println("setting up writer for [" + this + "]....");
				writer = new PrintStream(getSocket().getOutputStream());
			}
			//System.out.println("sending message <" + message + "> to [" + this + "]");
			writer.println(message);
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String receive()
	{
		String message = "";

		try {
			if (reader == null)
			{
				//System.out.println();
				//System.out.println("Set up reader for [" + this + "]");
				reader = new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
			}
			
			//System.out.println("waiting to read on reader [" + this + "]...");
			message = reader.readLine();
			if (message == null)
				message = "blank";
			//System.out.println("got <" + message +">");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}
	
	
	public String toString()
	{
		String s = getClientRoleName() + "/" + getServerRoleName() + " - " + getIP() + ":" + getPort() + " " + getSocket();
		return s;
	}

}
