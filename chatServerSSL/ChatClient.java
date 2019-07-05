import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Arrays;
import java.text.SimpleDateFormat; 
import java.util.Calendar;
import javax.net.ssl.*;
import java.security.*;

//edited by Drejc Pesjak
//63180224

public class ChatClient extends Thread
{
	protected int serverPort = 1234;
	protected String[] files = {"jn.private","mn.private","fh.private"}; 

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public ChatClient() throws Exception {
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		Scanner scanner = new Scanner(System.in);

		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			System.out.print("Choose your certificate: \n{");
			for(int i = 0; i<files.length-1; i++) {
				System.out.print((i+1) + ":" + files[i] + ", ");	
			} 
			System.out.print((files.length) + ":" + files[files.length-1] + "} ");

			String cert = scanner.nextLine();

			int ix; int num;
			if((ix = cert.indexOf('1')) != -1) {
				num = cert.charAt(ix)-'0';
			} else if((ix = cert.indexOf('2')) != -1) {
				num = cert.charAt(ix)-'0';
			} else if((ix = cert.indexOf('3')) != -1) {
				num = cert.charAt(ix)-'0';
			} else { throw new Exception(); } 
			
			String passphrase = files[num-1].substring(0,2) + "pwd1";
			
			// preberi datoteko s strežnikovim certifikatom
			KeyStore serverKeyStore = KeyStore.getInstance("JKS");
			serverKeyStore.load(new FileInputStream("server.public"), "public".toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim ključem
			KeyStore clientKeyStore = KeyStore.getInstance("JKS");
			clientKeyStore.load(new FileInputStream(files[num-1]), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kakšni so moji tajni ključi in certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(serverKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(clientKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

			// kreiramo socket
			SSLSocketFactory sf = sslContext.getSocketFactory();
			SSLSocket socket1 = (SSLSocket) sf.createSocket("localhost", serverPort);
			socket1.setEnabledCipherSuites(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" }); // dovoljeni nacin kriptiranja (CipherSuite)
			socket1.startHandshake(); // eksplicitno sprozi SSL Handshake

			socket = socket1;
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");

			//instrucitons
			//System.out.println("Standard public incoming messages: [username time] message\n private messages start with p.\nFor private messaging use following format: @usename message\nUsername must be without spaces!");


			//branje uporabniskega imena
			String name = ((SSLSocket) socket).getSession().getLocalPrincipal().getName();

			System.out.print("You are logged in as '" + name.substring(3) + "'\n"); 
			//throw new IOException("False username!");

			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread

		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// read from STDIN and send messages to the chat server
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		while ((userInput = std_in.readLine()) != null) { // read a line from the console
			this.sendMessage(userInput, out); // send the message to the chat server
		}

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendMessage(String message, DataOutputStream out) {
		try {
			String time = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
			message = time + message;
			out.writeUTF(message); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
				System.out.println(message); // print the message to the console
			}
		} catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
