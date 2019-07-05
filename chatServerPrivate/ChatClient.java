import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.text.SimpleDateFormat; 
import java.util.Calendar;


//edited by Drejc Pesjak
//63180224

public class ChatClient extends Thread
{
	protected int serverPort = 1234;
	protected String userName;

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public ChatClient() throws Exception {
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		userName = "";
		Scanner scanner = new Scanner(System.in);

		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			socket = new Socket("localhost", serverPort); // create socket connection
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");

			//instrucitons
			System.out.println("Standard public incoming messages: [username time] message\n private messages start with p.\nFor private messaging use following format: @usename message\nUsername must be without spaces!");


			//branje uporabniskega imena
			System.out.print("Enter your username: "); 
			String newName = scanner.nextLine();
			if(!newName.contains(" ") && newName.length()>0)
				this.sendMessage("//"+newName, out);
			else
				throw new IOException("False username!");
			userName = newName;

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
			message += " |"+userName;
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
