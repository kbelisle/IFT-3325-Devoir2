package com.Devoir2.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Date;

import com.Devoir2.HDLC.*;

public class Scenario {
	public static void main(String[] args) {
		
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
		
		//Receiver Unit test
		
		String C0=new Frame("C0",true).getFrame(),
				I0=new Frame("I0X",true).getFrame(),
				I1=new Frame("I1",true).getFrame(),
				A0=new Frame("A0",true).getFrame(),
				P0=new Frame("P0",true).getFrame(),
				F0=new Frame("F1",true).getFrame();
		String[][] liste= {
				{"No connection",I0,I0,F0},
				{"Noise",A0,C0,I0,I1,F0,A0},
				{"Corruption",C0,A0,F0},
				{"Out of order",C0,I1,I1,F0},
				{"Repetition",C0,I0,I0,I0,I0,F0},
				{"Timeout",C0,I0,"w",I0,"w",I1,"w",F0},
				{"P",C0,I0,I0,P0,I1,F0}
				};
		try {
			Socket s = new Socket("127.0.0.1",5000);
			DataInputStream in = new DataInputStream(s.getInputStream());
		    DataOutputStream out = new DataOutputStream(s.getOutputStream());
		    for(int i=0;i<liste.length;i++) {
		    	System.out.print("Test "+1+i+", "+liste[i][0]+": ");
				for(int j=1;j<liste[i].length;j++) {
					if(!"w".equals(liste[i][j]))
						out.writeUTF(liste[i][j]);
		            long timer=new Date().getTime();
		            while(in.available()<1&&new Date().getTime()-timer<3000) {}
		            if(in.available()>=1)
		            	System.out.println(new Frame(in.readUTF(),false).getMessage());
				}
				System.out.println("Success");
			}
		    out.writeUTF(new Frame("F0",true).getFrame());
		    out.close();
		    in.close();
		    s.close();
		} catch (Exception e) {
			System.out.println("Failed");
		}
	}
}
