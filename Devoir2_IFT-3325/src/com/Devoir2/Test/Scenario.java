package com.Devoir2.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.Devoir2.HDLC.*;

public class Scenario {
	public static void main(String[] args) {
		testFrame();
		testReceiver();
	}
	
	private static void testFrame() {
		//Frame Unit test	
		System.out.print("Test 1, no flags: ");
		System.out.println(new Frame("0",false).isValid()?"X":"Success");
		
		System.out.print("Test 2, one flag: ");
		System.out.println(new Frame("01111110",false).isValid()?"X":"Success");
		
		System.out.print("Test 3, only flags: ");
		System.out.println(new Frame("0111111001111110",false).isValid()?"X":"Success");
		
		System.out.print("Test 4, only crc: ");
		System.out.println(new Frame("01111110000000000000000001111110",false).isValid()?"X":"Success");
		
		System.out.print("Test 5, no Num: ");
		System.out.println(new Frame(new Frame("A",true).getFrame(),false).isValid()?"X":"Success");
		
		System.out.print("Test 6, Num not a number 0-8: ");
		System.out.println(new Frame(new Frame("Success",true).getFrame(),false).isValid()?"X":"Success");
		
		System.out.print("Test 7, CRC validity: ");
		System.out.println("011111100010000000100000000000000000000001111110".equals(new Frame("  ",true).getFrame())?"X":"Success");
		
		System.out.print("Test 8, CRC validity: ");
		System.out.println("011111100010000000100000001000101000010001111110".equals(new Frame("  ",true).getFrame())?"Success":"X");// Expected CRC
		
		System.out.print("Test 9, message consistency: ");
		System.out.println("Success".equals(new Frame(new Frame("Success",true).getFrame(),false).getMessage())?"Success":"X");
		
		System.out.print("Test 10, bit stuffing: ");
		System.out.println("01111110001111101001111101110100100001011101111110".equals(new Frame("??",true).getFrame())?"Success":"X");
	}
	
	private static void testReceiver() {
		//Receiver Unit test
		String C0=new Frame("C0",true).getFrame(),
				I0=new Frame("I0X",true).getFrame(),
				I1=new Frame("I1",true).getFrame(),
				A0=new Frame("A0",true).getFrame(),
				P0=new Frame("P0",true).getFrame(),
				F0=new Frame("F0",true).getFrame();
		String[][] liste= {
				{"No connection",I0,I0,F0},
				{"Noise",A0,C0,A0,I0,A0,I1,A0,F0},
				{"Corruption",C0,A0,F0},
				{"Out of order",C0,I1,I1,F0},
				{"Repetition",C0,I0,I0,I0,I0,F0},
				{"Timeout",C0,I0,"w",I0,"w",I1,"w",F0},
				{"P",C0,I0,I0,P0,I1,F0}
				};	
		ArrayList<String> results = new ArrayList<String>();
		int testNumber = 11;
		for(String[] s : liste) {
			results.add("Test #" + testNumber + " " + s[0] + " : " + testReceiverWithCommand(s,testNumber));
			testNumber++;

		}
		/*Allow the console to catch-up*/
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\n\n");
		for(String res : results) {
			System.out.println(res);
		}
	}
	
	private static String testReceiverWithCommand(String[] command, int testNumber) {
		try {
			/*Start a thread for the receiver*/
			Thread tr = new Thread() {
				public void run() {
					try {
						new Receiver(8750 + testNumber).listen();
					}
					catch (Exception e) {
						System.out.println("Receiver thread wasnt able to start : " + e.getMessage());
					}
				}
			};
			tr.start();
			/*Wait for server to start*/
			Thread.sleep(1000);
			Socket s = new Socket("localhost",8750 + testNumber);
			DataInputStream in = new DataInputStream(s.getInputStream());
		    DataOutputStream out = new DataOutputStream(s.getOutputStream());
	    	System.out.print("Test "+testNumber+", "+command[0]+": ");
			for(int j=1;j<command.length;j++) {
				if(!"w".equals(command[j]))
					out.writeUTF(command[j]);
	            long timer=new Date().getTime();
	            while(in.available()<1&&new Date().getTime()-timer<3000) {}
	            if(in.available()>=1)
	            	System.out.println("Received from Receiver : " + new Frame(in.readUTF(),false).getMessage());
			}
		    out.writeUTF(new Frame("F0",true).getFrame());
		    out.close();
		    in.close();
		    s.close();
		    return "Success";
		} catch (Exception e) {
			return "Failed : " + e.getMessage();
		}
	}
	
	public static void testSender() {
		//Sender Unit test
		String C0=new Frame("C0",true).getFrame(),
				I0=new Frame("I0X",true).getFrame(),
				I1=new Frame("I1",true).getFrame(),
				A0=new Frame("A0",true).getFrame(),
				P0=new Frame("P0",true).getFrame(),
				F0=new Frame("F0",true).getFrame();
		String[][] liste= {
				{"No connection",I0,I0,F0},
				{"Noise",A0,C0,A0,I0,A0,I1,A0,F0},
				{"Corruption",C0,A0,F0},
				{"Out of order",C0,I1,I1,F0},
				{"Repetition",C0,I0,I0,I0,I0,F0},
				{"Timeout",C0,I0,"w",I0,"w",I1,"w",F0},
				{"P",C0,I0,I0,P0,I1,F0}
				};	
		ArrayList<String> results = new ArrayList<String>();
		int testNumber = 11;
		for(String[] s : liste) {
			results.add("Test #" + testNumber + " " + s[0] + " : " + testReceiverWithCommand(s,testNumber));
			testNumber++;

		}
		//Allow the console to catch-up
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\n\n\n");
		for(String res : results) {
			System.out.println(res);
		}
	}
	
	private static String testSenderWithCommand(String[] command, int testNumber) {
		try {
			ServerSocket server = new ServerSocket(7750 + testNumber);	
			//Start a thread for the sender
			Thread tr = new Thread() {
				public void run() {
					try {
						Sender s = new Sender("localhost",7750 + testNumber,"");
					}
					catch (Exception e) {
						System.out.println("Receiver thread wasnt able to start : " + e.getMessage());
					}
				}
			};
			tr.start();
			//Wait for server to start
			server.accept();
			Socket s = new Socket("localhost",8750 + testNumber);
			DataInputStream in = new DataInputStream(s.getInputStream());
		    DataOutputStream out = new DataOutputStream(s.getOutputStream());
		    
	    	System.out.print("Test "+testNumber+", "+command[0]+": ");
			for(int j=1;j<command.length;j++) {
				if(!"w".equals(command[j]))
					out.writeUTF(command[j]);
	            long timer=new Date().getTime();
	            while(in.available()<1&&new Date().getTime()-timer<3000) {}
	            if(in.available()>=1)
	            	System.out.println("Received from Receiver : " + new Frame(in.readUTF(),false).getMessage());
			}
		    out.writeUTF(new Frame("F0",true).getFrame());
		    out.close();
		    in.close();
		    s.close();
		    return "Success";
		} catch (Exception e) {
			return "Failed : " + e.getMessage();
		}
	}
}
