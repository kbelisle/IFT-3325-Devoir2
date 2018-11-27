package com.Devoir2.HDLC;

public class Main {
	public static void main(String[] args) {
		if(args.length < 1)
			System.out.println("Please enter arguments");
		
		if(args[0].compareTo("Sender") == 0 && args.length == 5) {
			if(args[4].compareTo("0") != 0) {
				System.out.println("Only supports Go-Back-N");
				System.out.flush();
				return;
			}
			try {
				int port = Integer.parseInt(args[2]);
				Sender s = new Sender(args[1],port,args[3]);
				s.connect();
				s.send();
				s.disconnect();
				System.out.println("Sender process ended.");
				System.out.flush();
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
				System.out.flush();
			}
		}
		else if(args[0].compareTo("Receiver") == 0 && args.length==2) {
			try {
				int port = Integer.parseInt(args[1]);
				Receiver r = new Receiver(port);
				r.listen();
				System.out.println("Receiver process ended.");
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
				System.out.flush();
			}
		}
		else {
			System.out.println("Invalid arguments");
			System.out.flush();
		}
	}
}
