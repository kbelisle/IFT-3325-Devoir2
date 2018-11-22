package com.Devoir2.HDLC;

public class Main {
	public static void main(String[] args) {
		if(args.length < 1)
			System.out.println("Please enter arguments");
		
		if(args[0] == "Sender" && args.length == 5) {
			if(args[4] != "0") {
				System.out.println("Only supports Go-Back-N");
				return;
			}
			try {
				int port = Integer.parseInt(args[2]);
				new Sender(args[1],port,args[3]);
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		else if(args[0] == "Receiver" && args.length==2) {
			try {
				int port = Integer.parseInt(args[1]);
				new Receiver(port);
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		else {
			System.out.println("Invalid arguments");
		}
	}
}
