package com.Devoir2.HDLC;

import java.io.*;
import java.util.Date;
import java.net.*;

public class Receiver {
	/*Attributes*/
	Frame lastMsg;
	int next;
	long timer;
	String txt="";
	/*socket*/
	ServerSocket server;
	Socket socket;
	DataInputStream input;
	DataOutputStream output;
	
	/*Constructors*/
	public Receiver(int port) {
		// To cycle through 0-7. Next+7%8 should tell which value shouldn't appear.
		next=0;
		
		// We need an arg to know the port. Need additionnal condition to know it's an int between some numbers
		try {/*Connect to socket*/
			server = new ServerSocket(port);
	        socket = server.accept(); 
	        input = new DataInputStream(socket.getInputStream());
	        output = new DataOutputStream(socket.getOutputStream());

	        // Simple logic loop
	        while(true) {
	        	Frame frame =waitForFrame();
	        	String message=frame.getMessage();
	        	System.out.println("Received : " + message);
	        	if("F0".equals(message))
	        	    break;
	        	if("C0".equals(message)&&lastMsg==null) {
	        		lastMsg==new Frame("A0",true);
	        		output.writeUTF(lastMsg.getMessage());
	    			timer=(new Date()).getTime();
	        		System.out.println("Sent : " + lastMsg.getMessage());
	        	}
	        	if(lastMsg!=null) {
	        		if("P0".equals(message)) {
	        			lastMsg==new Frame("A"+next,true);
		        		output.writeUTF(lastMsg.getMessage());
		    			timer=(new Date()).getTime();
		        		System.out.println("Sent : " + lastMsg.getMessage());	
		        	}
	        		if(message.length()>=2&&("I").equals(message.substring(0, 1))) {
		        		if((""+next).equals(message.substring(1, 2))){
		        		    txt+=message.substring(2);//save
		        		    lastMsg = new Frame("A"+next,true);
		        		    next=(next+1)%8;
		        		}else{
		        			lastMsg = new Frame("R"+next,true);
		        		}
		        		output.writeUTF(lastMsg.getFrame());
		    			timer=(new Date()).getTime();
		        		System.out.println("Sent : " + lastMsg.getMessage());		
		        	}
	        	}
	        }
	        
	        // Save file
	        File file = new File("Data.txt");
	        BufferedWriter out = new BufferedWriter(new FileWriter(file)); 
	        out.write(txt);
	        out.close();
	        
	        // Releases ressources
	        input.close();
	        output.close();
	        socket.close();
	        server.close();
		} catch (NumberFormatException e) {
			return;
		} catch (IOException e) {
			return;
		}
	}
	/*Methods*/
	public Frame waitForFrame() {
		try {
			while(true) {
				while(input.available()<1) {
					long tmp=(new Date()).getTime();
					if(tmp-timer>=3000) {
						if(lastMsg!=null) {
							System.out.println("sending "+lastMsg.getMessage());
		            	    output.writeUTF(lastMsg.getFrame());
		            	}
		            	timer=tmp;
		            }
		        }
		        Frame rsp=new Frame(input.readUTF(),false);
		        if(rsp.isValid())
		        	return rsp;
			}
		}catch(Exception e) {return new Frame("N0",true);}
	}
}
