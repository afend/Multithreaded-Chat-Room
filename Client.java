import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client implements Runnable {

  // The client socket
  private static Socket userSocket = null;
  // The output stream
  private static PrintStream os = null;
  // The input stream
  private static DataInputStream is = null;

  private static BufferedReader toServer = null;
  private static boolean closed = false;
  
  private static Socket connect( String host, int port ) throws Exception
  {
    try
    {
	return new Socket( host, port );
    }
    catch ( ConnectException ce )
    {
        System.out.println("Cannot connect to server, waiting 3 seconds to reconnect.");
        Thread.sleep(2500);
	return null;
    }
  }
  
  private static boolean validatePort(String num) {
    	Pattern pattern = Pattern.compile("[0-9]+"); //checks if it is a digit
		if(pattern.matcher(num).matches()) {
        	//now check to see if it is in the range of 1-65535 
                //(valid port number) but we want above 1024
			int portNum = Integer.parseInt(num);
			if(portNum > 1024 && portNum <= 65535) {
	        	return true;
        	} else {
	        	return false;
        	}
		}
      
		return false;
}
  
  public static void main(String[] args) 
  {
    Scanner sc = new Scanner(System.in);
    int port = 0;
    Socket socket;
    String name, hostname, portAttempt;
    //If there arn't 3 command line arguments force the application to close
    if( args.length != 3)
    {
        System.out.println("Not enough arguments to connect to server, please specify IP/Hostname, Port and Username in that order when connecting.");
        System.exit(1);
    }
    
    boolean portValid = validatePort(args[1]);
    hostname = args[0];
    name = args[2];

    if(portValid)
    {
        port = Integer.parseInt(args[1]);
    }
    else
    {
        while(portValid != true)
        {
           System.out.println("Port not valid, please enter a valid port number to connect, or enter 1 to exit.");
           portAttempt = sc.nextLine();
           portValid = validatePort(portAttempt);
           if(portAttempt == "1")
               System.exit(1);
        }
        port = Integer.parseInt(args[1]);
    }


    try {
        //Attempt to connect to the server, will try again in 2.5 seconds if failed.
        do {
            userSocket = connect(hostname, port);
        } while ( userSocket == null );
       
      os = new PrintStream(userSocket.getOutputStream());
      is = new DataInputStream(userSocket.getInputStream());  
      toServer = new BufferedReader(new InputStreamReader(System.in));
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + hostname);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to the host "
          + hostname);
    } catch(Exception e) {
      System.err.println(e);
    }

    /*
     * If everything has been initialized then we want to write some data to the
     * socket we have opened a connection to on the port portNumber.
     */
    if (userSocket != null && os != null && is != null) {
      try {

        /* Create a thread to read from the server. */
        new Thread(new Client()).start();
        while (!closed) {
          os.println(toServer.readLine().trim());
        }
        /*
         * Close the output stream, close the input stream, close the socket.
         */
        os.close();
        is.close();
        userSocket.close();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }
    }
  }

  /*
   * Create a thread to read from the server. (non-Javadoc)
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    /*
     * Keep on reading from the socket till we receive "Bye" from the
     * server. Once we received that then we want to break.
     */
    String responseLine;
    try {
      while ((responseLine = is.readLine()) != null) {
        System.out.println(responseLine);
        if (responseLine.indexOf("*** Bye") != -1)
          break;
      }
      closed = true;
    } catch (IOException e) {
      System.err.println("IOException:  " + e);
    }
  }
}