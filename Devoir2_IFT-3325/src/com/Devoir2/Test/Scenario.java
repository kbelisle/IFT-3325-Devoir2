package com.Devoir2.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.stream.FileCacheImageInputStream;

import com.Devoir2.HDLC.*;



public class Scenario {
	public static void main(String[] args) {
		testFrame();
		testReceiver();
		testSender();
	}
	
	private static void testFrame() {
		//Frame Unit test	
		System.out.print("Test 1, no flags: ");
		System.out.println(new Frame("0",false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 2, one flag: ");
		System.out.println(new Frame("01111110",false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 3, only flags: ");
		System.out.println(new Frame("0111111001111110",false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 4, only crc: ");
		System.out.println(new Frame("01111110000000000000000001111110",false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 5, no Num: ");
		System.out.println(new Frame(new Frame("A",true).getFrame(),false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 6, Num not a number 0-8: ");
		System.out.println(new Frame(new Frame("Success",true).getFrame(),false).isValid()?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 7, CRC validity: ");
		System.out.println("011111100010000000100000000000000000000001111110".equals(new Frame("  ",true).getFrame())?"X":"Success");
		System.out.flush();
		
		System.out.print("Test 8, CRC validity: ");
		System.out.println("011111100010000000100000001000101000010001111110".equals(new Frame("  ",true).getFrame())?"Success":"X");// Expected CRC
		System.out.flush();
		
		System.out.print("Test 9, message consistency: ");
		System.out.println("Success".equals(new Frame(new Frame("Success",true).getFrame(),false).getMessage())?"Success":"X");
		System.out.flush();
		
		System.out.print("Test 10, bit stuffing: ");
		System.out.println("01111110001111101001111101110100100001011101111110".equals(new Frame("??",true).getFrame())?"Success":"X");
		System.out.flush();
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
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\n\n----------Receiver Test Results----------\n");
		System.out.flush();
		for(String res : results) {
			System.out.println(res);
			System.out.flush();
		}
	}
	
	private static String testReceiverWithCommand(String[] command, int testNumber) {
		try {
			/*Start a thread for the receiver*/
			Thread tr = new Thread() {
				public void run() {
					try {
						Receiver r = new Receiver(8750 + testNumber);
						r.listen();
					}
					catch (Exception e) {
						System.out.println("Receiver thread threw unexpected exception : " + e.getMessage());
						System.out.flush();
					}
				}
			};
			tr.start();
			/*Wait for server to start*/
			Thread.sleep(100);
			Socket s = new Socket("localhost",8750 + testNumber);
			DataInputStream in = new DataInputStream(s.getInputStream());
		    DataOutputStream out = new DataOutputStream(s.getOutputStream());
	    	System.out.print("Test "+testNumber+", "+command[0]+": ");
			for(int j=1;j<command.length;j++) {
				if(!"w".equals(command[j])) {
					out.writeUTF(command[j]);
					out.flush();
				}
				long timer=new Date().getTime();
	            while(in.available()<1&&new Date().getTime()-timer<3000) {}
	            if(in.available()>=1) {
	            	System.out.println("Received from Receiver : " + new Frame(in.readUTF(),false).getMessage());
	        		System.out.flush();
	            }
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
		String C0=new Frame("C0",true).getMessage(),
				I0=new Frame("I0",true).getMessage(),
				I1=new Frame("I1",true).getMessage(),
				A0=new Frame("A0",true).getMessage(),
				A1=new Frame("A1",true).getMessage(),
				P0=new Frame("P0",true).getMessage(),
				F0=new Frame("F0",true).getMessage();
		String[][] liste= {
				{"Retry connection","","w",C0,A0,F0},
				{"Lost ACK","abcdefghijabcdefghijabcdefghijabcdefghi"
						+ "jabcdefghijabcdefghijabcdefghijabcdefghi"
						+ "jabcdefghijabcdefghijz",C0,A0,I0,I1,A1,F0},
				};	
		ArrayList<String> results = new ArrayList<String>();
		int testNumber = 18;
		for(String[] s : liste) {
			results.add("Test #" + testNumber + " " + s[0] + " : " + testSenderWithCommand(s,testNumber));
			testNumber++;
		}
		//Allow the console to catch-up
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			/*Shouldn't happen in current context*/
			System.out.println(e.getMessage());
			return;
		}
		
		System.out.println("\n\n----------Sender Test Results----------\n");
		System.out.flush();
		for(String res : results) {
			System.out.println(res);
    		System.out.flush();
		}
	}
	
	private static String testSenderWithCommand(String[] command, int testNumber) {
		try {
			ArrayList<String> remainingData = new ArrayList<String>();
		    int index = 0;
		    while (index < command[1].length()) {
		    	remainingData.add(command[1].substring(index, Math.min(index + 100,command[1].length())));
		        index += 100;
		    }
			/*Create command test file*/
			String fileTxt = command[1];
			String fileName = "test" + testNumber + ".txt";
			File f = new File(fileName);
			f.setExecutable(true,false);
			f.setReadable(true,false);
			f.setWritable(true, false);
			f.deleteOnExit();
			f.createNewFile();
			FileWriter fw = new FileWriter(f);
			fw.write(fileTxt);
			fw.close();	
			//Start a thread for the sender
			Thread tr = new Thread() {
				public void run() {
					try {
						Sender s = new Sender("localhost",7700 + testNumber,fileName);
						/*Wait for server*/
						Thread.sleep(100);
						s.connect();
						s.send();
						s.disconnect();
					}
					catch (Exception e) {
						System.out.println("Sender thread threw unexpected exception : " + e.getMessage());
		        		System.out.flush();
					}
				}
			};
			ServerSocket server = new ServerSocket(7700 + testNumber);
			tr.start();
			Socket s = server.accept();
			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());   
	    	System.out.print("Test "+testNumber+", "+command[0]+": \n");
    		System.out.flush();
			for(int j=2;j<command.length;j++) {
				/*Logic test*/
				while(in.available() < 1) {}
				Frame frame = new Frame(in.readUTF(),false);
				if(!frame.isValid()) {
					throw new Exception("Invalid Frame received " + frame.getFrame());
				}
				String frameHeader = frame.getMessage().substring(0, 2);
				if("w".equals(command[j])) {
					/*Wait 3s*/
					long timeout = System.currentTimeMillis() + 3000;
					while(System.currentTimeMillis() >= timeout) {}
					continue;
				}
				if (command[j].charAt(0) == 'C') {
					if(!command[j].equals(frameHeader)) {
						throw new Exception("Must be connected first");
					}
					j++;/*Go next*/
				}
				if(command[j].charAt(0) == 'A' || command[j].charAt(0) == 'R') {
					/*Send ACK/NACK*/
					String output = new Frame(command[j],true).getFrame();
					out.writeUTF(output);
					out.flush();
				}
				else if(command[j].charAt(0) == 'I') {
					String expectedData;
					if(frame.getMessage().length() > 2) {
						if(remainingData.size() > 1) {
							expectedData = remainingData.get(0);
							remainingData.remove(0);
						}
						else {
							expectedData = remainingData.get(0);
						}
						String actualData = frame.getMessage().substring(2);
						if(!expectedData.equals(actualData)) {
							throw new Exception("Data not equal Expected : " + expectedData + ", Actual : " + actualData);
						}
					}
				}
				else if(command[j].charAt(0) == 'F') {
					/*Check if buffer is empty, otherwise fail test*/
				}
				else {
					throw new Exception("Expected : " + command[j] + ", Actual : " + frameHeader);
				}
			}
		    out.close();
		    in.close();
		    s.close();
		    server.close();
		    return "Success";
		} catch (Exception e) {
			return "Failed : " + e.getMessage();
		}
	}
}
