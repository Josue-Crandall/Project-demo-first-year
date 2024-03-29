package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import Model.Connection;
import Model.Pair;

public class Chat_Server {
	private static final int PORT_NUMBER = 55555;

	private static List<Connection> connections = new LinkedList<Connection>();

	public static void main(String... args) throws IOException {
		while (true) {
			if (connections.size() < 2) {
				try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
						BufferedReader ipReader = new BufferedReader(
								new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));) {

					System.out.println("Ready on");
					System.out.println("Port: " + serverSocket.getLocalPort());
					System.out.println("remote IP: " + ipReader.readLine() + "\n");

					Socket chatSocket = serverSocket.accept();

					Thread chatThread = new Thread(new Runnable() {
						public void run() {
							try {
								serverChatProtocol1(chatSocket);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
					});
					chatThread.start();
				}
			}
		}
	}

	private static void serverChatProtocol1(Socket soc) throws IOException, ClassNotFoundException {
		Connection con = new Connection(soc);

		// get handle & target
		con.handle = (String) con.in.readObject();
		con.target = (String) con.in.readObject();

		connections.add(con);

		System.out.println("Connection with " + con.handle + " established.");

		// catchup messages
		try (ObjectInputStream backlogReader = new ObjectInputStream(
				new FileInputStream(new File(con.handle + ".ser")));) {
			List<Pair<String, String>> log = (List<Pair<String, String>>) backlogReader.readObject();

			for (Pair<String, String> pair : log) {
				System.out.println(log.size());
				System.out.println("MESSAGE: " + pair.getKey() + " " + pair.getValue());
				con.out.writeObject(pair.getKey());
				con.out.writeObject(pair.getValue());
			}
		} catch (Exception e) {
			// fail silently, not always messages to send
		}
		new File(con.handle + ".ser").delete();

		// keepup messages // check for message overflow and shut down
		// server
		while (true) {
			try {
				String message = (String) con.in.readObject();

				System.out.println("Got message from: " + con.handle + "\nMessage: " + message + "\nForwarding to: "
						+ con.target + "\n");

				boolean found = false;

				for (Connection c : connections)
					if (c.handle.equals(con.target)) {
						c.out.writeObject(con.handle);
						c.out.writeObject(message);
						found = true;
						break;
					}
				if (!found) {
					List<Pair<String, String>> log;
					try (ObjectInputStream backlogReader = new ObjectInputStream(
							new FileInputStream(new File(con.target + ".ser")))) {
						log = (List<Pair<String, String>>) backlogReader.readObject();
					} catch (Exception e) {
						System.out.println("Creating new list, can't load old one.");
						log = new LinkedList<>();
					}

					try (ObjectOutputStream backlogWriter = new ObjectOutputStream(
							new FileOutputStream(new File(con.target + ".ser")))) {
						log.add((new Pair<String, String>(con.handle, message)));
						backlogWriter.writeObject(log);
					} catch (Exception e) {
						System.out.println("Failed to save message to forward later.");
					}
				}
			} catch (Exception e) {
				System.out.println(con.handle + "'s connection dropped.");

				break;
			}
		}
		connections.remove(con);
	}
}
