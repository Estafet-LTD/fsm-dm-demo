package com.estafet.fsm.example;
/**
 * This is a simple version of the easyFSM example without any action overriding. So it is
 * as simple as it can be.
 **/
import java.io.*;
import java.util.logging.*;
import java.util.ArrayList;

import org.xml.sax.*;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;

import javax.xml.parsers.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.estafet.fsm.example.fakedns.*;
import com.estafet.scribble.easyfsm.Action.FSMAction;
import com.estafet.scribble.easyfsm.FSM.FSM;
import com.estafet.scribble.easyfsm.States.FSMStateAction;

public class SocketProgramDM 
{

	private String 	m_thisRoleName = "role";
    private String 	m_configFileName = "/Users/stalbot/Documents/workspace/FSM-Example/resources/config.xml";
    private String 	m_machineName = "127.0.0.1";
    private String 	m_portNumber = "9091";
    private FakeDNS m_dns = null;
    private Boolean m_client = false;
    
    private io.opentracing.Tracer   m_tracer;
    
    public SocketProgramDM()
    {
    	
    }
    public SocketProgramDM(String config)
    {
    	setConfigFileName(config);
    }
    public SocketProgramDM(String r, String config, String dnsconfig, String client)
    {
    	setThisRoleName(r);
    	setConfigFileName(config);
    	setDNS(dnsconfig);
    	m_client = (client.compareTo("client") == 0);
    }


    public void setTracer(io.opentracing.Tracer t) { m_tracer = t; }
    public io.opentracing.Tracer getTracer() { return m_tracer; }
    
    public Boolean client() { return m_client; }
    public Boolean server() { return !client(); }
    public void setDNS(String f) { m_dns = new FakeDNS(f); m_dns.loadDNS(); }
    public FakeDNS getDNS() { return m_dns; }
    public SocketDNSEntry lookUpServer(String r) { return getDNS().findDNSServerEntry(r); }
    public SocketDNSEntry lookUpClient(String r) { return getDNS().findDNSClientEntry(r); }
    public SocketDNSEntry lookUpClientServer(String cs) { return getDNS().findDNSClientServerEntry(cs); }

    
    public void setThisRoleName(String r) { m_thisRoleName = r; }
    public String getThisRoleName() { return m_thisRoleName; }
    public void setConfigFileName(String f) { m_configFileName = f; }
    public String getConfigFileName() { return m_configFileName; }
	public void setMachineName(String m) { m_machineName = m; }
	public String getMachineName() { return m_machineName; }
	public void setPortNumber(String p) { m_portNumber = p; }
	public String getPortNumber() { return m_portNumber; }
	
    public  void runFSM() 
    {    	
        try 
        {
        	System.out.println("SocketProgramDM - tonight Matthew I shall be " + getThisRoleName() + " as a " + (client() ? "client" : "server"));
        	System.out.println();
        	
        	setTracer(initTracer(getThisRoleName()));
        	 
        	System.out.println("Open config file " + getConfigFileName());
        	System.out.println("Class: " + this.getClass().getName());
        	System.out.println("ClassLoader: " + this.getClass().getClassLoader().getClass().getName());
        	System.out.println("resource stream: " + this.getClass().getClassLoader().getResourceAsStream(getConfigFileName()));
        	InputStream inputS = null;
        	String clear = "[H[2J";
        	System.out.print(clear);
        	System.out.println("SocketProgramDM - tonight Matthew I shall be " + getThisRoleName() + " as a " + (client() ? "client" : "server"));
        	Thread.sleep(5000);
        	
    		try 
    		{
    			FileInputStream m_fileReader = new FileInputStream(getConfigFileName());
    			inputS = m_fileReader;
    			if (m_fileReader == null)
    				System.out.println("Not got file!!!" + getConfigFileName());
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        	
        	if (inputS == null)
        	{
        		System.out.println("Input stream is null!!!!");
        		return;
        	}
        	
        	//
        	// I think the @Override enables various actions within the FSM to be overridden at compile time by 
        	// injecting logic into the overridden methods. This is where I would bind the business logic to the 
        	// state machine and use introspection to do it.
        	//
            FSM f = new FSM(inputS, null);
            
            f.setAction("receiveMessage(getSuppliers(uuid))", new FSMAction() {
                @Override
                public boolean action(String curState, String message, String nextState, Object args) {
                    System.out.println("Customized receiveMessage(getSuppliers(uuid))!");
                    return true;
                }
            });
        	
            //
            // The range of messages that an easyFSM can act upon are:
            // accept_connection
            // request_connection
            // receiveMessage
            // sendMessage
            //
            // each has attached to it the name of the target role
            // So what we do is check the message from the config file
            // and create the duel prior to acting out.
            //
            // This for any role p receiving a request_connection message
            // from some role r this translates to a command at the FSM level
            // to do an accept_connection from r rather than a request_connection
            // to p. Concretely:
            //		request_connection__to_authorisersvc 
            // becomes
            //  	accept_connection__from_requestor 
            // and
            //		sendMessage(getsuppliersRtn(supplierdetails))__to_suppliersvc
            //		receiveMessage(getsuppliersRtn(supplierdetails))__from_authorisersvc
            // Where authorisersvc is each case is thisrole
            //
            // In effect we translate the incoming message and not the outbound
            //	
            String command = "";
            String message = "";
            String currentState = f.getCurrentState();
            String nextStates[] = f.getNextStates();
            String validActions[] = f.getValidActions();
            String validCommands[] = f.getValidCommands();
            
            String items[] = validCommands;
            Integer choice = 0;
            
            int loopcount = 0;
            //
            // START LOOPING THROUGH THE FSM
            // If this is a server then the first command should be an ACCEPT
            //		wait on socket for request
            // If this is a client then the first command should be on the command line itself
            //		read from command line
            // If the next command(s) is a commandLineRequest, that is I send, then
            //		read from command line
            // If the next command(s) is a receiveChoice of some sort, then
            //		wait on the socket
            //
            // The sendMessage option always has the destination so consider that!!
            //
        	SocketDNSEntry me = getDNS().findDNSEntryForServer(getThisRoleName());
        	System.out.println("my DNS entry is [" + me + "]");

        	//
        	// Big loop to process states
        	//
        	currentState = f.getCurrentState();
        	nextStates = f.getNextStates();
        	validActions = f.getValidActions();
        	validCommands = f.getValidCommands();
        	items = validCommands;

        	int loop = 0;
            while (items.length > 0)
            {          

            	
            	Boolean clr = commandLineRequest(items);

            	if (loop == 0 && server()) // Initialise server and output first set of states because of the accept
            	{
            			// START
            			doPreamble(clr,f);
            			me.configureServerConnection(getRoleFromMessage(items[0].trim()));
            	} else {
            			doPreamble(clr,f);
            	}

        		//
        		// If it's a client then
        		//		if the choices are sends/requests then the locus of control is the client
        		//		if the choices are receives then the locus of control is elsewhere and it
        		//		needs to receive on a socket
        		// If it's a server then
        		//		if the choices are sends/requests then the locus of control is with the server
        		//		if the choices are receives then the locus of control is elsewhere and it 
        		//		needs to receive on a socket
        		//
        		
        		if (server()) // Server
        			choice = doServer(clr, items);
        		else // Client
        			choice = doClient(clr, items);	
           		whatINeedToDo(validCommands[choice].trim());
            	//Thread.sleep(2000);
           		
                if (f.ProcessFSM(validCommands[choice].trim()) == null)
                {
                	System.out.println("*** ERROR ***");
                	System.out.println("    <" + validCommands[choice].trim() + "> has no matching state transition");
                }	
            	currentState = f.getCurrentState();
            	nextStates = f.getNextStates();
            	validActions = f.getValidActions();
            	validCommands = f.getValidCommands();
            	items = validCommands;
            	loop++;
            }
            System.out.println("FINISHED!");

        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    public Boolean commandLineRequest(String items[])
    {
    	Boolean clr = true;
    	if (items.length == 0) // hidden
    		return false;
		String role = getRoleFromMessage(items[0].trim());

		for (int i=0; (i < items.length); i++)
		{
			clr = clr && (items[i].startsWith("sendMessage") || items[i].startsWith("request_"));
			if (role.compareTo(getRoleFromMessage(items[i].trim())) != 0)
					System.out.println("PROBLEM - roles do not match in choice"); 
		}
		return clr;
    }
    
    public void doPreamble(Boolean clr, FSM f)
    {
    	String	nextStates[] = f.getNextStates();
    	String 	validActions[] = f.getValidActions();
    	String 	validCommands[] = f.getValidCommands();
    	String  items[] = validCommands;
    	//
    	// Telling the process what it can do
    	//
    	System.out.println("====== "+ f.getCurrentState() + " =======");
    	ArrayList<SocketDNSEntry> list = getDNS().getDNSMap();
    	for (int i=0; (i < list.size()); i++)
    	{
    		System.out.println("    " + list.get(i));
    	}
		System.out.println("========================================");
		System.out.println("Valid next commands from here are: ");
		for (int i=0; (i < items.length); i++)
			if (clr)
				System.out.println("\tPress " + i + "\tfor " + items[i]);
			else
				System.out.println("\t" + items[i]);
		System.out.println();
		System.out.println("========================================");

		
		if (clr)
			System.out.print("Command? ");
		else 
			System.out.print("Command: ");
    }
    
    public Integer doServer(Boolean clr, String items[])
    {
		SocketDNSEntry me = getDNS().findDNSEntryForServer(getThisRoleName());
    	String command = "";
    	Integer choice = 0;
    	try
    	{
			if (clr) 	// If the next set of actions are requests of some sort then get the input from the command line
			{
				System.out.println("items length is " + items.length);
					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
					String entry = bufferRead.readLine();
					choice = new Integer(entry);
				
	    		command = items[choice];
	           // System.out.println(command + "- selected.");
			}
			else 				// otherwise they must be receives or accepts
			{
				// Who am I waiting on to receive a command? 
				// Have to look at the next messages to determine that one.
				// 		Receive accept on socket for me
				// 		If next state is accept_connection then it's me
				// 		If next state is anything else other than 
				SocketDNSEntry listenTo = me;
				if (getRoleFromMessage(items[0]).trim().compareTo(getThisRoleName()) != 0)
				{
					listenTo = getDNS().findDNSEntryForClientServer(getThisRoleName(),getRoleFromMessage(items[0]).trim());
					if (listenTo == null) // Bootstrap on first message to receive as DNSEntry is not set for the actual client
						listenTo = getDNS().findDNSEntryForClientServer(getThisRoleName(),"client");
				}
	        	//System.out.println("Waiting at " + listenTo);
	            command = listenTo.receive();
	            for (int i=0; (i < items.length); i++)
	            {
	            	String s = items[i].trim();
	            	if (s.compareTo(command.trim()) == 0)
	            		choice = i;
	            }
	            //System.out.println();
	            //System.out.println(command + "- received (" + choice + ").");
			}
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	return choice;
    }
    
    public Integer doClient(Boolean clr, String items[])
    {
		SocketDNSEntry target = getDNS().findDNSEntryForServer(getRoleFromMessage(items[0]).trim());
    	String command = "";
    	Integer choice = 0;
    	try
    	{
			if (clr)
			{
				// Get instructions from command line
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
	    		String entry = bufferRead.readLine();
	    		choice = new Integer(entry);
	    		command = items[choice];
	    		Span span = getTracer().buildSpan("get_msg_from_command_line").startManual();
	    		span.setTag("get_msg_from_command_line", command);
	            //System.out.println(command + "- selected.");
			} else {
				// Receive accept on socket for target
				//System.out.println("Waiting at " + target);
    		    command = target.receive();
	            for (int i=0; (i < items.length); i++)
	            {
	            	String s = items[i].trim();
	            	if (s.compareTo(command.trim()) == 0)
	            		choice = i;
	            }
	    		Span span = getTracer().buildSpan("get_msg_from_socket").startManual();
	    		span.setTag("get_msg_from_socket", command);
	            //System.out.println();
	            //System.out.println(command + "- received (" + choice + ").");
			} 
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	return choice;
    }
    
    public void whatINeedToDo(String msg)
    {

    	try
    	{
	    	//System.out.println("Processing Message: <" + msg + ">");
	    	// I am sending a request connection to some server
	    	// and they will receive an accept_connection
	    	if (msg.contains("request_connection")) // SEND ACCEPT
	    	{
	    		Span span = getTracer().buildSpan("request_connection").startManual();
	    		span.setTag("request_connection", msg);
	    		requestConnection(msg);
	    	}
	    	// I am ready to take an accept_connect which must have resulted in the client
	    	// having sent a request_connection to me.
	    	else if (msg.contains("accept_connection")) // NOTHING
	    	{
	    		Span span = getTracer().buildSpan("accept_connection").startManual();
	    		span.setTag("accept_connection", msg);
	    		acceptConnection(msg);
	    	}
	    	else if (msg.contains("request_disconnection"))  // DISCONNECT
	    	{
	    		Span span = getTracer().buildSpan("request_disconnection").startManual();
	    		span.setTag("request_disconnection", msg);
	    		requestDisconnection(msg);
	    	}
	    	else if (msg.contains("sendMessage"))
	    	{
	    		Span span = getTracer().buildSpan("sendMessage").startManual();
	    		span.setTag("sendMessage", msg);
	    		sendMessage(msg);
	    	} 
	    	else if (msg.contains("receiveMessage"))
	    	{
	    		Span span = getTracer().buildSpan("receiveMessage").startManual();
	    		//System.out.println("Finished processing received message " + msg);
	    	}
	    	else
	    	{
	    		System.out.println("System Error - illegal message <" + msg + ">");
	    		System.exit(1);
	    	}
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    //
    // This is a client requesting a connection to a server
    //
    public void requestConnection(String msg)
    {
    	String m = createDuelCommand(msg);
    	SocketDNSEntry target = getDNS().findDNSEntryForServer(getRoleFromMessage(msg));
		//System.out.println(msg);
		//System.out.println("SEND <" + m + "> to "+ target);
		// Send m to target
		try
		{
			target.configureClientConnection(getThisRoleName());
			Socket s = new Socket(target.getIP(), new Integer(target.getPort()));
			target.send(m);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    public void requestDisconnection(String msg)
    {
    	String m = createDuelCommand(msg);
    	SocketDNSEntry target = getDNS().findDNSEntryForServer(getRoleFromMessage(msg));
		//System.out.println("request_disconnection");
		// Send the paired duel message m to the target
		// And then collect up the socket as a client socket.
		//System.out.println("SEND <" + m + "> from " + target);
		try
		{
			//Socket s = target.getSocket();
			//PrintStream output = new PrintStream(target.getSocket().getOutputStream());
			//output.println(m);
			//output.flush();
			//target.getSocket().close();
			target.deConfigureClient(m);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    public void acceptConnection(String msg)
    {
    	String m = createDuelCommand(msg);
    	SocketDNSEntry me = getDNS().findDNSEntryForServer(getThisRoleName());
		//System.out.println("accept_connection");
		//System.out.println("Housekeeping... " + m);
		me.configureClient(getRoleFromMessage(msg).trim());
    }
    
	//
	// Use the active channel for the message. So if the message is to role
	// TO and the message is M and the sender is FROM then we look for a DNSEntry
	// that has both client and server ends set to either TO/FROM or FROM/TO.
	// Only when the ends are set is the channel active.
	//
    public void sendMessage(String msg)
    {
    	String m = createDuelCommand(msg);
		String toRole = getRoleFromMessage(msg).trim();
		String fromRole = getRoleFromMessage(m).trim();
		SocketDNSEntry sendTo = getDNS().findDNSEntryForClientServer(toRole, fromRole);
		//System.out.println("toRole(" + toRole + ") fromRole(" + fromRole +")");
		//System.out.println("SEND <" + m + " to " + sendTo);
		try
		{
			if (sendTo == null) // to deal with implicit connection
			{
				//System.out.println("Implicit connection to <" + toRole + ">");
				sendTo = getDNS().findDNSEntryForServer(toRole);
				sendTo.configureClient(getThisRoleName());
			}
	
			// This means that the client will send a message to the server
			// Which translates to sending message m to the target
			sendTo.send(m);
			//System.out.println("SENT <" + m + " to " + sendTo);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    public String createDuelCommand(String c)
    {
    	String duel = "";
    	if (c.startsWith("accept_connection"))
    	{
    		duel = c.replaceAll("accept_connection", "request_connection");
    		duel = duel.replaceAll("__from", "__to");
    		duel = duel.replaceAll("to_.*", "to_" + getThisRoleName());
    	}
    	else if (c.startsWith("request_connection"))
    	{
    		duel = c.replaceAll("request_connection", "accept_connection");
    		duel = duel.replaceAll("__to", "__from");
    		duel = duel.replaceAll("from_.*", "from_" + getThisRoleName());
    	}
    	else if (c.startsWith("sendMessage"))
    	{
    		duel = c.replaceAll("sendMessage", "receiveMessage");
    		duel = duel.replaceAll("__to", "__from");
    		duel = duel.replaceAll("from_.*", "from_" + getThisRoleName());
    	}
    	else if (c.startsWith("receiveMessage"))
    	{
    		duel = c.replaceAll("receiveMessage", "sendMessage");
    		duel = duel.replaceAll("__from", "__to");
    		duel = duel.replaceAll("to_.*", "to_" + getThisRoleName());
    	} else if(c.startsWith("request_disconnection"))
    	{
    		duel = c.replaceAll("__to", "_from");
    		duel = duel.replaceAll("from_.*", "from_" + getThisRoleName());	
    	} else
    		return null;
    	return duel;
    }
    
    public String getRoleFromMessage(String m)
    {
    	String fromPrefix = "__from_";
    	String toPrefix = "__to_";
    	String role = "";
    	if (m.contains(fromPrefix))
    	{
    		role = m.substring(m.indexOf(fromPrefix)+fromPrefix.length());
    	}
    	else if (m.contains(toPrefix))
    	{
    		role = m.substring(m.indexOf(toPrefix)+toPrefix.length());
    	}
    	return role;
    }
    
    public static com.uber.jaeger.Tracer initTracer(String service)
    {
        SamplerConfiguration samplerConfig = new SamplerConfiguration("const", 1);
        ReporterConfiguration reporterConfig = new ReporterConfiguration(true, null, null, null, null);
        Configuration config = new Configuration(service, samplerConfig, reporterConfig);
        return (com.uber.jaeger.Tracer) config.getTracer();
    }

    public static void main(String[] args) {
        try {
        	SocketProgramDM p = null;
        	if (args.length >= 3)
        	{
        		 p = new SocketProgramDM(args[0], args[1], args[2], args[3]);
        	} else {
        		System.out.println("Usage Error:\nSocketProgramDM role easyFSMConfig dnsConfig [client|server]");
        		System.exit(1);
        	}
            p.runFSM();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        System.out.println("*** STOPPED ***");
    }
}