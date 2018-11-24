package com.Devoir2.HDLC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Sender {
	/*Constant*/
	private final static int DATA_SIZE_LIMIT = 100;
	private final static int MAX_BUFFER_WINDOW_SIZE = 7;
	private final static int MAX_WAIT_TIME = 3000;
	private final static int MAX_ATTEMPT = 30;
	/*Attributes*/
	Socket s;
	DataInputStream in;
	DataOutputStream out;
	BufferedReader file;
	Frame[] frameBuffer = new Frame[7]; /*2^(3)-1 = 7*/
	int nextEmpty = 0;
	int lastACK = MAX_BUFFER_WINDOW_SIZE - 1;
	boolean sendingData = false;
	
	/*Properties*/
	
	/*Constructors*/
	public Sender(String name, int port, String fileName) throws Exception {
		try {
			/*Connect to server*/
			s = new Socket(name,port);
			in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
			/*Open File*/
			File f = new File(fileName);
			if(!f.exists() || !f.canRead()) throw new Exception("File doesn't exists or unreadable");
			file = new BufferedReader(new FileReader(f));
			
		}
		catch(UnknownHostException e) {
			throw new Exception ("L'hote n'existe pas");
		}
		catch(FileNotFoundException e) {
			throw new Exception("Le fichier n'existe pas");
		}
		catch(Exception e) {
			/*Unexpected Exception*/
			throw e;
		}
	}
	/*Methods*/
	private void waitForACK() throws Exception {
		waitForACK(0);
	}
	private void waitForACK(int attempt) throws Exception {
		if(attempt > MAX_ATTEMPT) {
			throw new Exception("Sender ended after multiples (>30) "
					+ "failed attempt to received ACKs/NACKs (on waitForACK();)");
		}
		Frame f = getNextFrame();
		if(f == null) {
			/*IF wait > 3s*/
			resend();
			waitForACK(attempt+1);
			return;
		}
		if(!f.isValid()) {
			/*Invalid Frame, treat it as it never reached */
			waitForACK(attempt+1);
			return;
		}
		char type = f.getMessage().charAt(0);
		int num = Character.getNumericValue(f.getMessage().charAt(1));
		if(num < 0 || num >= MAX_BUFFER_WINDOW_SIZE) {
			/*Invalid Num, treat it as it never reached */
			waitForACK(attempt+1);
			return;
		}
		if(type == 'A') {
			/*ACK(num) received*/
			/*Remove from lastAck +1 to num*/
			int i = (lastACK+1)%MAX_BUFFER_WINDOW_SIZE;
			do {
				frameBuffer[i] = null;
			}while((i++)%MAX_BUFFER_WINDOW_SIZE != num);
			lastACK = num;
		}
		else if(type == 'R') {
			/*NACK(num) received*/
			resend();
			waitForACK(attempt+1);
			return;
		}
		else {
			/*Invalid Type, treat it as it never reached */
			waitForACK(attempt+1);
			return;
		}
	}
	public void connect() throws Exception {
		connect(0);
	}
	private void connect(int attempt) throws Exception {
		if(attempt > MAX_ATTEMPT) {
			throw new Exception("Sender ended after multiples (>30) "
					+ "failed attempt to received ACKs/NACKs (on connect();)");
		}
		Frame f = new Frame("C0",true);
		out.writeUTF(f.getFrame());
		/*Wait for RR*/
		Frame ack = getNextFrame();
		if(ack == null) {
			/*No frame after 3s*/
			connect(attempt+1);
		}
		else if(ack.getMessage().compareTo("A0") == 0) {
			sendingData = true;
		}
		else {
			/*Wrong Frame received*/
			throw new Exception("Expecting RR, Received : " + ack.getMessage());
		}
	}
	
	public void disconnect() throws IOException {
		Frame f = new Frame("F0",true);
		out.writeUTF(f.getFrame());
		/*TODO: Wait for UA??*/
		sendingData = false;
		in.close();
		out.close();
		s.close();
		file.close();
	}
	
	private String getDataFromFile() throws IOException {
		int value = -1;
		int index = 0;
		char[] fileData = new char[DATA_SIZE_LIMIT];
		while(index < DATA_SIZE_LIMIT) {
			if((value = file.read()) == -1) break;
			fileData[index] = (char)value;
			index++;
		}
		return new String(fileData, 0, index);
	}
	
	public void send() throws Exception {
		while(sendingData) {
			if(frameBuffer[nextEmpty] != null) {
				/*Buffer is full, wait for ACK*/
				waitForACK();
			}
			
			String data = getDataFromFile();
			if(data.length() == 0) {
				/*No Data left*/
				waitForACK();
			}
			/*if data.length() < DATA_SIZE_LIMIT, we are sending the last frame*/
			sendingData = data.length() == DATA_SIZE_LIMIT;
			
			Frame nextFrame = new Frame("I"+ Integer.toString(nextEmpty) + data,true);
			frameBuffer[nextEmpty] = nextFrame;
			nextEmpty = (nextEmpty + 1) % MAX_BUFFER_WINDOW_SIZE;
			
			out.writeUTF(nextFrame.getFrame());
			System.out.println("Sent : " + nextFrame.getMessage());
		}
		/*All the data was sent at least once, waiting for all ACKs/NACKs*/
		while(true) {
			waitForACK();
			/*The buffer is empty, the receiver has received the entire file*/
			if(lastACK + 1 == nextEmpty) break;
		}
	}
	private void resend() throws IOException {
		/*Resend everything from lastAck+1 to nextEmpty -1*/
		int i = (lastACK+1)%MAX_BUFFER_WINDOW_SIZE;
		do {
			out.writeUTF(frameBuffer[i].getFrame());
			System.out.println("Re-sent : " + frameBuffer[i].getMessage());
			i = (i+1)%MAX_BUFFER_WINDOW_SIZE;
		}while(i != nextEmpty);
	}
	
	private Frame getNextFrame() throws IOException {
		/*ACK,NACK, RR, UA have size 6 for TP uses.*/
		/*wait for 6 characters have been received*/
		long timeout = System.currentTimeMillis() + MAX_WAIT_TIME;
		/*6 octets = 48 bits*/
		while(in.available() < 48) {
			if(System.currentTimeMillis() >= timeout) {
				/*No Frame in the last MAX_WAIT_TIME seconds*/
				return null;
			}
		}
		Frame f = new Frame(in.readUTF(),false);
		System.out.println("Received: " + f.getMessage());
		return f;
	}
}
