/**
 * An echo server listening on port 6007.
 * This server reads from the client
 * and echoes back the result.
 *
 * This services each request in a separate thread.
 *
 * This conforms to RFC 862 for echo servers.
 *
 * @author - Greg Gagne.
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class  EchoServerMT
{
	public static final int DEFAULT_PORT = 8080;

    // construct a thread pool for concurrency
	private static final Executor exec = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException {
		ServerSocket sock = null;
		Configuration configuration = null;

		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);

			while (true) {
				/**
				 * now listen for connections
				 * and service the connection in a separate thread.
				 */
				 String location = args[0];
				try{
					 configuration = new Configuration(location);
				}catch(ConfigurationException e){
					System.err.println(e);
				}

				Runnable task = new Connection(sock.accept(),configuration);
				exec.execute(task);
			}
		}
		catch (IOException ioe) { }
		finally {
			if (sock != null)
				sock.close();
		}
	}
}
