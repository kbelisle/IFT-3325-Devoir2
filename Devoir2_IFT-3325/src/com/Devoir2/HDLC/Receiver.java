package com.Devoir2.HDLC;

import java.io.*;
import java.util.Date;
import java.net.*;

public class Receiver {
	/*Attributes*/
	boolean connected;
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
	                connected=false;
	        while(true) {
	        	Frame frame =waitForFrame();
	        	System.out.println("Received : " + frame.getMessage());
	        	String message="N";
	        	if(frame.isValid())
	        	    message=frame.getMessage();
	        	if("F0".equals(message))
	        	    break;
	        	if(("I").equals(message.substring(0, 1))) {
	        		if(message.length()>2&&(""+next).equals(message.substring(1, 2))){
	        		    txt+=message.substring(2);//save
	        		    Frame f = new Frame("A"+next,true);
	        		    output.writeUTF(f.getFrame());
	        		    System.out.println("Sent : " + f.getMessage());
	        		    next++;
	        		}else {
	        			Frame f = new Frame("R"+next,true);
	        			output.writeUTF(f.getFrame());
	        			System.out.println("Sent : " + f.getMessage());
	        		}		
	        	}else {
	        		Frame f = new Frame("A"+next,true);
        			output.writeUTF(f.getFrame());
        			System.out.println("Sent : " + f.getMessage());
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
		timer=(new Date()).getTime();
	        while(input.available()<1) {
	            long tmp=(new Date()).getTime();
	            if(tmp-timer>=3000) {
	            	if(connected) {
	    	            System.out.println("sending A"+next);
	            	    output.writeUTF(new Frame("A"+next,true).getFrame());
	            	}
	        	timer=tmp;
	            }
	        }
	        return new Frame(input.readUTF(),false);
		}catch(Exception e) {return new Frame("N0",true);}
	}
}
