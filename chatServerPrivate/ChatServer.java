import java.io.*;
import java.net.*;
import java.util.*;


//edited by Drejc Pesjak
//63180224

public class ChatServer {

	protected int serverPort = 1234;
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients
	protected List<String> usernames = new ArrayList<String>(); //list of usernames, corresponding indexes
	protected Socket socketOne;

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		Iterator<Socket> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			Socket socket = (Socket) i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void sendToSpecificClient(String message, String receiver, String sender) throws Exception {
		/*System.out.println(message + "," + receiver + ","+ sender);
		System.out.println(usernames);
		System.out.println(clients);*/

		//find receiver
		for(String u : this.usernames) {
			if(receiver.equals(u)){
				/*System.out.println(receiver +" "+ u);
				System.out.println(usernames.indexOf(u));*/				
				socketOne = clients.get(usernames.indexOf(u));
				break;
			}
		}

		//message = "p[" +sender+ "] " + message; 
		message = "p[" +sender+ " " + message.substring(0,5) + "] " + message.substring(5);
		try {
			DataOutputStream out = new DataOutputStream(socketOne.getOutputStream()); // create output stream for sending messages to the client
			out.writeUTF(message); // send message to the client
			socketOne = null;
		} catch (Exception e) {
			System.err.println("[system] could not send message to a client");
			
			for(String u : this.usernames) {
				if(sender.equals(u)){
					socketOne = clients.get(usernames.indexOf(u));
					break;
				}
			}
			DataOutputStream out = new DataOutputStream(socketOne.getOutputStream()); // create output stream for sending messages to the client
			out.writeUTF("[system] Could not send message to specified user!");
			socketOne = null;
			e.printStackTrace(System.err);
		}
	}

	public boolean addUsername(String user) {
		if(clients.size() == usernames.size()+1) {
			usernames.add(user);
			return true;
		}
		return false;
	}

	public void removeClient(Socket socket) {
		synchronized(this) {
			usernames.remove(clients.indexOf(socket));
			clients.remove(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;

	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	public void run() {
		System.out.println("[system] connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());

		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client
			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

			if (msg_received.length() == 0) // invalid message
				continue;

			System.out.println("[RKchat] [" + this.socket.getPort() + "] : " + msg_received); // print the incoming message in the console


			String time = msg_received.substring(0, 5);
			msg_received = msg_received.substring(5);

			String msg_send = ""; String rec = ""; String snd = "";
			if(msg_received.startsWith("//")) {	
				String us = msg_received.substring(2, msg_received.length()-2);
				if(server.addUsername(us))
					msg_send = us + " joined chat server";	
			} else {
				// @receiver message |sender
				if(msg_received.startsWith("@")) {
					rec = msg_received.substring(1,msg_received.indexOf(" "));
					msg_received = msg_received.substring(msg_received.indexOf(" ")+1);
				}
				snd = msg_received.substring(msg_received.indexOf("|")+1);
				msg_send = msg_received.substring(0,msg_received.indexOf("|"));
			}
			//msg_send = time +" "+ msg_send;
 
			try {
				if(rec != "") {
					//private
					this.server.sendToSpecificClient(time+msg_send,rec,snd);
				} else {
					//public
					if(snd != "")
						this.server.sendToAllClients("["+ snd +" " + time + "] "+msg_send); // send message to all clients
					else
						//user joined server
						this.server.sendToAllClients("["+time+"] "+msg_send);
				}				
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
}
