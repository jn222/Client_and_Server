import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
//Jonathan Ng
// FTPServer takes input from FTPClient after establishing a connection through a port that is given
// as an argument. It can then send files to the Client with a bytestream.

public class FTPServer {
	/*
	 * inputChecker takes in a token of input from the main method, echoes the
	 * command, and prints that the command is ok or what part of the command is
	 * wrong.First, a while loop is used to check if there are any carriage
	 * returns present in the line (endlines are already accounted for with the
	 * scanner delimiter).If there are, the carriage return is treated as the
	 * end of one command, and inputChecker is recursively called on that
	 * command. The original input is then trimmed of that command.Next, lengths
	 * are checked. If one command is less than 5 commands long, it is
	 * automatically invalid because no valid command is less than 5 characters
	 * long (with a trimmed endline). This is done to avoid out of bounds
	 * errors.Then the command is split into separate parts. The last character
	 * is taken as the crlf value, and the 5th to length-2 substring is taken as
	 * the argument of the command.The command is then evaluated in a switch
	 * statement based on the first 4 characters of the command, the command
	 * name. If the command is not found, then a command error is printed.For
	 * type, user, and pass, the argument is checked to make sure that it is
	 * valid. If not, the respective argument errors are thrown.Then, the CRLF
	 * is checked. The reason only the carriage return is checked is because
	 * endline is trimmed in the main method. If the carriage return is in the
	 * middle of the line, then it is replaced before being sent in the while
	 * loop to cause it to evaluate to a CRLF error.For the commands without
	 * arguments, the length is evaluated to make sure the command is right,
	 * then the crlf is checked in the same manner.inputChecker returns after an
	 * error or command ok is printed.
	 */

	private static final Exception Exception = null;
	static boolean userlog = false;
	static boolean passlog = false;
	static boolean portstate = false;
	static int retrcount = 0;
	static Socket control_socket = null;
	static Socket file_socket = null;
	static DataOutputStream output;
	static Scanner input_read;
	static int port_num;
	static ServerSocket welcomesocket;
	static int portaddress;

	public static void inputChecker(String input) throws Exception {
		inputChecker(input, false);
	}

	private static void inputChecker(String input, boolean stray)
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
			inputChecker(input.substring(0, input.indexOf("\r") + 1)
					.replaceFirst("\r", "\t"), true);
			input = input.substring(input.indexOf("\r") + 1);
		}
		if (stray)
			System.out.print(input.substring(0, input.length() - 1) + "\r\n");
		else
			System.out.print(input + "\n");
		if (input.length() < 5) {
			write(output, input_read,
					"500 Syntax error, command unrecognized.\r\n");
			return;
		}
		String body = "";
		if (input.length() > 6) {
			body = input.substring(5, input.length() - 1);
		}
		char crlf = input.charAt(input.length() - 1);
		switch (input.substring(0, 4).toUpperCase()) {
		case "TYPE":
			if (input.charAt(4) != ' ') {
				write(output, input_read,
						"500 Syntax error, command unrecognized.\r\n");
				return;
			}
			if ((!body.equals("A") && !body.equals("I"))
					|| input.charAt(input.length() - 2) == ' '
					|| crlf != ('\r')) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			type(body);
			return;
		case "USER":
			if (input.charAt(4) != ' ') {
				write(output, input_read,
						"500 Syntax error, command unrecognized.\r\n");
				return;
			}
			if (body.equals("") || crlf != ('\r')) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			// Loop Invariant: at the start of each iteration, there are no
			// characters with
			// ASCII value >127 before the character that is currently being
			// checked. If an illegal character is found, an error is thrown.
			for (char c : body.toCharArray()) {
				if (c > 127) {
					write(output, input_read,
							"501 Syntax error in parameter.\r\n");
					return;
				}
			}
			user(body);
			return;
		case "RETR":
			if (input.charAt(4) != ' ') {
				write(output, input_read,
						"500 Syntax error, command unrecognized.\r\n");
				return;
			}
			if (body.equals("") || crlf != ('\r')) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			// Loop Invariant: at the start of each iteration, there are no
			// characters with
			// ASCII value >127 before the character that is currently being
			// checked. If an illegal character is found, an error is thrown.
			for (char c : body.toCharArray()) {
				if (c > 127) {
					write(output, input_read,
							"501 Syntax error in parameter.\r\n");
					return;
				}
			}
			retr(body);
			return;
		case "PASS":
			if (input.charAt(4) != ' ') {
				write(output, input_read,
						"500 Syntax error, command unrecognized.\r\n");
				return;
			}
			if (body.equals("") || crlf != ('\r')) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			// Loop Invariant: at the start of each iteration, there are no
			// characters with
			// ASCII value >127 before the character that is currently being
			// checked. If an illegal character is found, an error is thrown.
			for (char c : body.toCharArray()) {
				if (c > 127) {
					write(output, input_read,
							"501 Syntax error in parameter.\r\n");
					return;
				}
			}
			pass(body);
			return;
		case "PORT":
			if (input.charAt(4) != ' ') {
				write(output, input_read,
						"500 Syntax error, command unrecognized.\r\n");
				return;
			}
			String[] ports = body.split(",");
			if (body.equals("") || body.charAt(0) == ','
					|| body.charAt(body.length() - 1) == ','
					|| ports.length != 6 || crlf != ('\r')) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			//
			for (int i = 0; i < 5; i++) {
				if (!ports[i].matches("^\\d+$")
						|| Integer.valueOf(ports[i]) < 0
						|| Integer.valueOf(ports[i]) > 255) {
					write(output, input_read,
							"501 Syntax error in parameter.\r\n");
					return;
				}
			}
			port(ports);
			return;
		case "SYST":
			if (crlf != ('\r') || input.length() != 5) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			syst();
			return;
		case "NOOP":
			if (crlf != ('\r') || input.length() != 5) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			noop();
			return;
		case "QUIT":
			if (crlf != ('\r') || input.length() != 5) {
				write(output, input_read, "501 Syntax error in parameter.\r\n");
				return;
			}
			quit();
			return;
		default:
			write(output, input_read, "502 Command not implemented.\r\n");
			return;
		}
	}

	// Checks if logged in, if not, outputs not logged in error and returns
	// true.
	// Then, checks if a username has been entered without a password. If so,
	// then returns bad sequence error.
	public static boolean stateChecker() throws Exception {
		if (!userlog && !passlog) {
			write(output, input_read, "530 Not logged in.\r\n");
			return true;
		}
		if (userlog && !passlog) {
			write(output, input_read, "503 Bad sequence of commands.\r\n");
			return true;
		}
		return false;
	}

	// Checks if logged in, then prints output for typecode command, formatting
	// and printing the code entered.
	private static void type(String typecode) throws Exception {
		if (stateChecker()) {
			return;
		}
		write(output, input_read, "200 Type set to " + typecode + ".\r\n");
	}

	// Checks if another username has already been entered, if not, prepares
	// server for password to be entered.
	private static void user(String username) throws Exception {
		userlog = true;
		passlog = false;
		write(output, input_read, "331 Guest access OK, send password.\r\n");

	}

	// Cheks if logged in and if port command has been entered yet. If so,
	// splits filepath
	// all the way to the last slash or backslash
	// Then, grabs bytes from file and sends it to the client
	private static void retr(String filepath) throws Exception {
		if (stateChecker()) {
			return;
		}
		if (!portstate) {
			write(output, input_read, "503 Bad sequence of commands.\r\n");
			return;
		}
		try {
			if (!new File(filepath).exists()) {
				write(output, input_read,
						"550 File not found or access denied.\r\n");
				return;
			}
			file_socket = new Socket(control_socket.getInetAddress().getHostName(), portaddress);
			write(output, input_read, "150 File status okay.\r\n");
		} catch (Exception e) {
			write(output, input_read, "425 Can not open data connection.\r\n");
			return;
		}
		try {
			OutputStream file_writer = file_socket.getOutputStream();
			String[] filename = filepath.split("(/)|(\\\\)");
			File file = new File(filepath);
			BufferedInputStream reader = new BufferedInputStream(
					new FileInputStream(file));
			byte[] bytes = new byte[(int) file.length()];
			reader.read(bytes);
			file_writer.write(bytes, 0, bytes.length);
			portstate = false;
			reader.close();
			file_socket.close();
			file_socket = null;
		} catch (Exception e) {
			file_socket.close();
			file_socket = null;
			return;
		}
		write(output, input_read, "250 Requested file action completed.\r\n");
	}

	// Checks if logged in, sets the portstate to be true so retr
	// can later be called, then formats and prints port address
	// Sets up file socket with client
	private static void port(String[] ports) throws Exception {
		if (stateChecker()) {
			return;
		}
		portaddress = (Integer.valueOf(ports[4]) * 256)
				+ Integer.valueOf(ports[5]);
		if (file_socket != null)
			file_socket.close();
		write(output, input_read, "200 Port command successful (" + ports[0]
				+ "." + ports[1] + "." + ports[2] + "." + ports[3] + ","
				+ portaddress + ").\r\n");
		portstate = true;
	}

	// Checks if username has been entered, then checks if another
	// password has been entered already. If not, logs user in.
	private static void pass(String password) throws Exception {
		if (!userlog) {
			write(output, input_read, "530 Not logged in.\r\n");
			return;
		}
		if (passlog) {
			write(output, input_read, "503 Bad sequence of commands.\r\n");
			return;
		}
		passlog = true;
		write(output, input_read, "230 Guest login OK.\r\n");
	}

	// Checks if logged in, then prints output for syst command
	private static void syst() throws Exception {
		if (stateChecker()) {
			return;
		}
		write(output, input_read, "215 UNIX Type: L8.\r\n");
	}

	// Checks if logged in, then prints output for noop command
	private static void noop() throws Exception {
		if (stateChecker()) {
			return;
		}
		write(output, input_read, "200 Command OK.\r\n");
	}

	// Prints proper response for quit, then exits program
	private static void quit() throws Exception {
		write(output, input_read, "221 Goodbye.\r\n");
		control_socket.close();
		control_socket = null;
	}

	/*
	 * The main method takes in user input, and separates it using the newline
	 * as a delimiter.inputChecker is called on each token until there are no
	 * more tokens to be checked.
	 */

	public static void write(DataOutputStream output, Scanner input,
			String message) throws Exception {
		System.out.print(message);
		output.writeBytes(message);
	}

	// gets port_num as input
	public static void main(String[] args) {
		port_num = Integer.parseInt(args[0]);
		String input = null;
		try {
			welcomesocket = new ServerSocket(port_num);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			System.exit(0);
		}
		/*
		 * At the start of each iteration of the do-while loop, a new string
		 * token with delimiternewline is passed to inputChecker.
		 */
		while (true) {
			try {
				control_socket = welcomesocket.accept();
				output = new DataOutputStream(control_socket.getOutputStream());
				input_read = new Scanner(new InputStreamReader(
						control_socket.getInputStream()));
				input_read.useDelimiter("\n");
				write(output, input_read, "220 COMP 431 FTP server ready.\r\n");
				do {
					input = input_read.next();
					if (input != null)
						inputChecker(input);
					if (input != null && input.toUpperCase().equals("QUIT"))
						break;
				} while (control_socket != null);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}

