package com.estafet.fsm.example.fakedns;

public class AbstractDNSEntry {
	
	
	private String 	m_serverRoleName = "server";
	private String 	m_clientRoleName = "client";

	
	public AbstractDNSEntry()
	{
		
	}
	public AbstractDNSEntry(String s)
	{
		setServerRoleName(s);
	}
	
	public void setServerRoleName(String s) { m_serverRoleName = s;  }
	public String getServerRoleName() { return m_serverRoleName; }
	public void setClientRoleName(String c) { m_clientRoleName = c;  }
	public String getClientRoleName() { return m_clientRoleName; }
	
	public void send(String message) {}
	public String receive() { return "abstract"; }

	public String toString()
	{
		String s = getClientRoleName() + "/" + getServerRoleName();
		return s;
	}

}

