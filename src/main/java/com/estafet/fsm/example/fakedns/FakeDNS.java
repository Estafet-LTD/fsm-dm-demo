package com.estafet.fsm.example.fakedns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

public class FakeDNS {
	
	private String	m_dnsFileName = "fakedns/ipconfig.txt";
	private HashMap<String, SocketDNSEntry>	m_dnsMapByServerRoleName = new HashMap<String, SocketDNSEntry>();
	private HashMap<String, SocketDNSEntry>	m_dnsMapByClientRoleName = new HashMap<String, SocketDNSEntry>();
	private HashMap<String, SocketDNSEntry>	m_dnsMapByClientServerRoleName = new HashMap<String, SocketDNSEntry>();
	private ArrayList<SocketDNSEntry>			m_dnsMap = new ArrayList<SocketDNSEntry>();
	public FakeDNS()
	{
		
	}
	public FakeDNS(String dnsFileName)
	{
		setDNSFileName(dnsFileName);
	}
	
	public void setDNSFileName(String f) { m_dnsFileName = f; }
	public String getDNSFileName() { return m_dnsFileName; }
	
	public void loadDNS()
	{
		String	role = null;
		String	ip = null;
		String  port = null;
		
		ArrayList<String>	rawText = null;
		try {
			//use . to get current directory
			File dir = new File(".");
			File fin = new File(dir.getCanonicalPath() + File.separator + getDNSFileName());
			rawText = readFile(fin);
			if (rawText.isEmpty())
				return;
			for (int i=0; (i < rawText.size()); i++)
			{
				String str = rawText.get(i);
				List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
				if (items.size() == 3)
				{
					role = items.get(0);
					ip = items.get(1);
					port = items.get(2);
					SocketDNSEntry e = new SocketDNSEntry(role,ip,port);
					//addDNSEntryToServerMap(e);
					addDNSEntry(e);
				} else {
					System.out.println("Error parsing " + getDNSFileName() + " at line " + i);
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private ArrayList<String> readFile(File fin) throws IOException 
	{
		FileInputStream fis = new FileInputStream(fin);
		ArrayList<String>	rtn = new ArrayList<String>();
	 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	 
		String line = null;
		while ((line = br.readLine()) != null) {
			rtn.add(line);
		}
	 
		br.close();
		return rtn;
	}
	
	public void addDNSEntryToServerMap(SocketDNSEntry d)
	{
		if (m_dnsMapByClientRoleName.get(d.getServerRoleName()) == null)
			m_dnsMapByServerRoleName.put(d.getServerRoleName(), d);
	}
	public SocketDNSEntry findDNSServerEntry(String r)
	{
		SocketDNSEntry d = m_dnsMapByServerRoleName.get(r);
		return d;
	}
	public ArrayList<SocketDNSEntry> getDNSServerEntries()
	{
		ArrayList<SocketDNSEntry> list = new ArrayList<SocketDNSEntry>(m_dnsMapByServerRoleName.values());
		return list;
	}
	public void addDNSEntryToClientMap(SocketDNSEntry d)
	{
		if (m_dnsMapByClientRoleName.get(d.getClientRoleName()) == null)
			m_dnsMapByClientRoleName.put(d.getClientRoleName(), d);
	}
	public SocketDNSEntry findDNSClientEntry(String r)
	{
		SocketDNSEntry d = m_dnsMapByClientRoleName.get(r);
		return d;
	}
	public ArrayList<SocketDNSEntry> getDNSClientEntries()
	{
		ArrayList<SocketDNSEntry> list = new ArrayList<SocketDNSEntry>(m_dnsMapByClientRoleName.values());
		return list;
	}
	
	public void addDNSEntryToClientServerMap(SocketDNSEntry d)
	{
		if (m_dnsMapByClientServerRoleName.get(d.getClientRoleName()+":"+d.getServerRoleName()) == null)
			m_dnsMapByClientServerRoleName.put(d.getClientRoleName()+":"+d.getServerRoleName(), d);
	}
	public void removeDNSEntryToClientServerMap(SocketDNSEntry d)
	{
		m_dnsMapByClientServerRoleName.remove(d);
	}
	public void addDNSEntry(SocketDNSEntry d)
	{
		m_dnsMap.add(d);
	}
	public SocketDNSEntry findDNSEntry(SocketDNSEntry d)
	{
		for (int i=0; (i < m_dnsMap.size()); i++)
		{
			if (m_dnsMap.get(i) == d)
			{
				return m_dnsMap.get(i);
			}
		}
		return null;
	}
	public SocketDNSEntry findDNSEntryForServer(String s)
	{
		for (int i=0; (i < m_dnsMap.size()); i++)
		{
			if (m_dnsMap.get(i).getServerRoleName().compareTo(s) == 0)
			{
				return m_dnsMap.get(i);
			}
		}
		return null;
	}
	public SocketDNSEntry findDNSEntryForClient(String c)
	{
		for (int i=0; (i < m_dnsMap.size()); i++)
		{
			if (m_dnsMap.get(i).getClientRoleName().compareTo(c) == 0)
			{
				return m_dnsMap.get(i);
			}
		}
		return null;
	}
	public SocketDNSEntry findDNSEntryForClientServer(String c, String s)
	{
		//System.out.println("findDNSEntryForClientServer<" + c + "><" + s + ">");
		for (int i=0; (i < m_dnsMap.size()); i++)
		{
			SocketDNSEntry e = m_dnsMap.get(i);
			//System.out.println(e);
			if ((e.getServerRoleName().compareTo(s) == 0) && (e.getClientRoleName().compareTo(c) == 0) ||
				(e.getServerRoleName().compareTo(c) == 0) && (e.getClientRoleName().compareTo(s) == 0))
				return e;
		}
		System.out.println("Found nothing for <" + c + "><" + s + ">");
		return null;
	}
	public void removeDNSEntry(SocketDNSEntry d)
	{
		for (int i=0; (i < m_dnsMap.size()); i++)
		{
			if (m_dnsMap.get(i) == d)
			{
				m_dnsMap.remove(i);
			}
		}
	}
	public ArrayList<SocketDNSEntry> getDNSMap()
	{
		return m_dnsMap;
	}
	
	public SocketDNSEntry findDNSClientServerEntry(String r)
	{
		SocketDNSEntry d = m_dnsMapByClientServerRoleName.get(r);
		return d;
	}
	public ArrayList<SocketDNSEntry> getDNSClientServerEntries()
	{
		ArrayList<SocketDNSEntry> list = new ArrayList<SocketDNSEntry>(m_dnsMapByClientServerRoleName.values());
		return list;
	}
	
    public static void main(String[] args) 
    {
    	FakeDNS dns = null;
    	try {
        	if (args.length >= 1)
        		 dns = new FakeDNS(args[0]);
        	else 
        		 dns = new FakeDNS();
            dns.loadDNS();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        ArrayList<SocketDNSEntry> l = dns.getDNSServerEntries();
        for (int i=0; (i < l.size()); i++)
        {
        	SocketDNSEntry d = l.get(i);
        	System.out.println("Entry " + i + "<" + d.getServerRoleName() + "><" + d.getIP() + "><" + d.getPort() + ">");
        }  
    }
}
