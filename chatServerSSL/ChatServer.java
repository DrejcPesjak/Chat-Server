import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;

//edited by Drejc Pesjak
//63180224

public class ChatServer {

	protected int serverPort = 1234;
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients
	protected Socket socketOne;

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			String passphrase = "serverpwd";

			// preberi datoteko z odjemalskimi certifikati
			KeyStore clientKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje odjemalčevih javnih ključev (certifikatov)
			clientKeyStore.load(new FileInputStream("client.public"), "public".toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim ključem
			KeyStore serverKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje strežnikovega tajnega in javnega ključa
			serverKeyStore.load(new FileInputStream("server.private"), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kakšni so moji tajni ključi in certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(clientKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(serverKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

			// kreiramo socket
			SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
			SSLServerSocket ss = (SSLServerSocket) factory.createServerSocket(serverPort);
			ss.setNeedClientAuth(true); // tudi odjemalec se MORA predstaviti s certifikatom
			ss.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA256"});

			serverSocket = ss;
			//serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket socket = serverSocket.accept(); // vzpostavljena povezava
				((SSLSocket)socket).startHandshake(); // eksplicitno sprozi SSL Handshake
				String username = ((SSLSocket) socket).getSession().getPeerPrincipal().getName();
				System.out.println("[system] established SSL connection with: " + username);

				synchronized(this) {
					clients.add(socket); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, socket); // create a new thread for communication with the new client
				conn.start(); // run the new thread


				//print connected users
				String s = "";
				for(int i = 0; i<clients.size()-1; i++){
					s +=  " : " + ((SSLSocket) clients.get(i)).getSession().getPeerPrincipal().getName().substring(3);
				}
				sendToAllClients(s+"]", socket);
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
	public void sendToAllClients(String message, Socket senderSoc) throws Exception {

		String sender = ((SSLSocket) senderSoc).getSession().getPeerPrincipal().getName();
		message = "[" + sender.substring(3) + message;

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

	public void sendToSpecificClient(String message, String receiver, Socket senderSoc) throws Exception {	
		
		//find receiver
		for(Socket user : this.clients) {
			String s = ((SSLSocket) user).getSession().getPeerPrincipal().getName();
			if(receiver.equals(s.substring(3))){	
				socketOne = user;		
				break;
			}
		}

		String sender = ((SSLSocket) senderSoc).getSession().getPeerPrincipal().getName();
		//message = p[sender time] message; 
		message = "p[" +sender.substring(3)+ " " + message.substring(0,5) + "] " + message.substring(5);
		try {
			DataOutputStream out = new DataOutputStream(socketOne.getOutputStream()); // create output stream for sending messages to the client
			out.writeUTF(message); // send message to the client
			socketOne = null;
		} catch (Exception e) {
			//backward message of failure
			System.err.println("[system] could not send message to a client");
			socketOne = senderSoc;
			DataOutputStream out = new DataOutputStream(socketOne.getOutputStream()); // create output stream for sending messages to the client
			out.writeUTF("[system] Could not send message to specified user!");
			socketOne = null;
			e.printStackTrace(System.err);
		}
	}


	public void removeClient(Socket socket) {
		synchronized(this) {
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

			String msg_send = ""; String receiver = ""; 			
			// @[receiver] message
			if(msg_received.startsWith("@") && msg_received.contains("]")) {
				receiver = msg_received.substring(2,msg_received.indexOf("]"));
				msg_received = msg_received.substring(msg_received.indexOf("]")+1);
			}
			msg_send = msg_received;
		
			//msg_send = time +" "+ msg_send;
 
			try {
				if(receiver != "") {
					//private
					this.server.sendToSpecificClient(time+msg_send,receiver, this.socket);
				} else {
					//public
					this.server.sendToAllClients(" " + time + "] "+msg_send, this.socket); // send message to all clients
				}				
			} catch (Exception e) {
				System.err.println("[system] there was a problem while sending the message to all clients");
				e.printStackTrace(System.err);
				continue;
			}
		}
	}
}
