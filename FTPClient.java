import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
	//Jonathan Ng
public class FTPClient {
	// takes user input (FTP Commands) and interacts with server based on those
	// commands. Can get files from server.
	static boolean connectlog = false;
	static InetAddress myInet;
	static int port;
	static String host;
	static Socket control_socket = null;
	static DataOutputStream output;
	static Scanner input;
	static int retrcount = 0;

	// checks input of user before handing command off to the corresponding
	// method
	public static void inputChecker(String input) {
		System.out.print(input);
		String[] split_input = input.split("\\s+");
		switch (split_input[0].toUpperCase()) {
		case "CONNECT":
			if (split_input.length < 2) {
				System.out.print("ERROR -- request\n");
				break;
			}
			String server_host = split_input[1];
			boolean a = false,
			ad = false;
			for (char c : server_host.toCharArray()) {
				if (!a && c > 64 && c < 123)
					a = true;
				else if (a && ((c > 47 && c < 58) || (c > 64 && c < 123)))
					ad = true;
				else if (a && ad && c == '.') {
					a = false;
					ad = false;
				} else {
					System.out.print("ERROR -- server-host\n");
					return;
				}
			}
			if (split_input.length < 3) {
				System.out.print("ERROR -- server-host\n");
				break;
			}
			if (split_input.length != 3) {
				System.out.print("ERROR -- server-port\n");
				break;
			}
			int server_port = -1;
			if (split_input[2].matches("^\\d+$"))
				server_port = Integer.valueOf(split_input[2]);
			if (server_port < 0 || server_port > 65535) {
				System.out.print("ERROR -- server-port\n");
				break;
			}
			connect(server_host, server_port);
			break;
		case "GET":
			if (split_input.length < 2) {
				System.out.print("ERROR -- pathname\n");
				return;
			}
			String pathname = input.substring(4, input.length() - 1)
					.replaceAll("^\\s+", "");
			// Loop Invariant: at the start of each iteration, there are no
			// characters with
			// ASCII value >127 before the character that is currently being
			// checked. If an illegal character is found, an error is thrown.
			for (char c : pathname.toCharArray()) {
				if (c > 127) {
					System.out.print("ERROR -- pathname\n");
					return;
				}
			}
			get(pathname);
			break;
		case "QUIT":
			if (!input.toUpperCase().equals("QUIT\n")) {
				System.out.print("ERROR -- request\n");
				return;
			}
			quit();
			break;
		default:
			System.out.print("ERROR -- request\n");
		}
	}

	public static void replyChecker(String input) throws Exception {
		replyChecker(input, false);
	}

	private static void replyChecker(String input, boolean stray)
			throws Exception {

		/*
		 * Loop invariant:at the start of each iteration of the while loop, the
		 * condition is checked which determines if there is a carriage return
		 * not at the end of the commannd.If this is true, inputChecker is run
		 * recursively on the substring before and including thecarriage return.
		 * The original input is then trim up to that carriage return.
		 */
		while (input.contains("\r")
				&& input.indexOf("\r") != input.length() - 1) {
			replyChecker(input.substring(0, input.indexOf("\r") + 1)
					.replaceFirst("\r", "\t"), true);
			input = input.substring(input.indexOf("\r") + 1);
		}
		if (input.length() < 4 || input.charAt(3) != ' '
				|| !input.substring(0, 3).matches("^\\d+$")
				|| Integer.valueOf(input.substring(0, 3)) < 100
				|| Integer.valueOf(input.substring(0, 3)) > 599) {
			System.out.print("ERROR -- reply-code\n");
			throw new FTPException();
		}
		String body = "";
		if (input.length() < 5) {
			System.out.print("ERROR -- reply-text\n");
			throw new FTPException();
		}
		if (input.length() > 5) {
			body = input.substring(4, input.length() - 1);
		}
		if (body.equals("")) {
			System.out.print("ERROR -- reply-text\n");
			throw new FTPException();
		}
		// Loop Invariant: at the start of each iteration, there are no
		// characters with
		// ASCII value >127 before the character that is currently being
		// checked. If an illegal character is found, an error is thrown.
		for (char c : body.toCharArray()) {
			if (c > 127) {
				System.out.print("ERROR -- reply-text\n");
				throw new FTPException();
			}
		}
		if (input.charAt(input.length() - 1) != ('\r')) {
			System.out.print("ERROR -- <CRLF>\n");
			throw new FTPException();
		}
		System.out.print("FTP reply " + input.substring(0, 3)
				+ " accepted. Text is : " + body + "\r\n");
		if (Integer.valueOf(input.substring(0, 3)) >= 400) {
			throw new FTPException();
		}
	}

	// checks if user is logged in
	public static boolean stateChecker() {
		if (!connectlog) {
			System.out.print("ERROR -- expecting CONNECT\n");
			return true;
		}
		return false;
	}

	// tries to connect to server by establishing welcoming socket
	public static void connect(String server_host, int server_port) {
		try {
			if (control_socket != null)
				control_socket.close();
			port = server_port;
			host = server_host;
			control_socket = new Socket(host, port);
			output = new DataOutputStream(control_socket.getOutputStream());
			input = new Scanner(new InputStreamReader(
					control_socket.getInputStream()));
			input.useDelimiter("\n");
		} catch (Exception e) {
			System.out.print("CONNECT failed\r\n");
			return;
		}
		try {
			System.out.print("CONNECT accepted for FTP server at host "
					+ server_host + " and port " + server_port + "\r\n");
			connectlog = true;
			String line;
			// waits for server reply
			for (int i = 0; i < 1;) {
				if ((line = input.next()) != null) {
					i++;
					replyChecker(line);
				}
			}
			write(output, input, "USER anonymous\r\n", 1);
			write(output, input, "PASS guest@\r\n", 1);
			write(output, input, "SYST\r\n", 1);
			write(output, input, "TYPE I\r\n", 1);
		} catch (Exception e) {
			return;
		}
	}

	// sends port and retr command to server, gets file from server in bytes and
	// writes to new file
	public static void get(String pathname) {
		if (stateChecker())
			return;
		int[] port_number = { (++port - (port % 256)) / 256,
				port % 256 };
		ServerSocket fileserver;
		try {
			fileserver = new ServerSocket(port);
			System.out.print("GET accepted for " + pathname + "\n");
		} catch (Exception e) {
			System.out.print("Get failed, FTP-data port not allocated.\n");
			if (new File("retr_files/file" + retrcount).exists())
				new File("retr_files/file" + retrcount).delete();
			return;
		}
		try{
			write(output, input, "PORT "
					+ myInet.getHostAddress().replace(".", ",") + ","
					+ port_number[0] + "," + port_number[1] + "\r\n",1);
			write(output, input, "RETR " + pathname + "\r\n", 1);
			Socket file_socket = fileserver.accept();
			InputStream reader = file_socket.getInputStream();
			if (new File("retr_files/file" + ++retrcount).exists())
				new File("retr_files/file" + retrcount).delete();
			File file = new File("retr_files/file" + retrcount);
			FileOutputStream writer = new FileOutputStream("retr_files/file"
					+ retrcount);
			byte[] bytes = new byte[8192];
			int count = -1;
			// reads bytes and writes them to file
			while ((count = reader.read(bytes)) > 0) {
				writer.write(bytes, 0, count);
			}
			file_socket.close();
			file_socket = null;
			fileserver.close();
			fileserver = null;
			// waits for reply from server
			String line;
			for (int i = 0; i < 1;) {
				if ((line = input.next()) != null) {
					i++;
					replyChecker(line);
				}
			}
		} catch (Exception e) {
			return;
		}
	}

	// closes socket
	public static void quit() {
		if (stateChecker())
			return;
		System.out.print("QUIT accepted, terminating FTP client\n");
		try {
			write(output, input, "QUIT\r\n", 1);
		} catch (Exception e) {
			return;
		}
		System.exit(0);
	}

	// writes to server and prints to output
	public static void write(DataOutputStream output, Scanner input,
			String message, int expected_message) throws Exception {
		System.out.print(message);

			output.writeBytes(message);
			String line;
			// waits for output from server
			for (int i = 0; i < expected_message;) {
				if ((line = input.next()) != null) {
					i++;
					replyChecker(line);
				}
			}

	}

	// reads commands from user until no commands are left.
	public static void main(String[] args) {
		Reader r = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(r);
		String input = null;
		try {
			myInet = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		/*
		 * At the start of each iteration of the do-while loop, a new string
		 * line is passed to inputChecker.
		 */
		try {
			while ((input = br.readLine()) != null) {
				inputChecker(input + '\n');
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
}

