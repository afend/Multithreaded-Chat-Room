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
  
//Attempt to connect to the Server, if cannot connect, wait 2.5 seconds and try again.
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
  
//To validate the user entered a vaild port number. Makes sure its only digits from 1024-65535
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
  
  public static void main(String[] args) throws IOException
  {
    Scanner sc = new Scanner(System.in);
    int port = 0;
    String hostname, portAttempt;
    //If there arn't 2 command line arguments force the application to close
    if( args.length != 2)
    {
        System.out.println("Not enough arguments to connect to server, please specify IP/Hostname and Port in that order when connecting.");
        System.exit(1);
    }
    
    //Validate port
    boolean portValid = validatePort(args[1]);
    hostname = args[0];

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
      System.err.println("Unable to retrieve I/O connection to host "
          + hostname);
    } catch(Exception e) {
      System.err.println(e);
    }


    if (userSocket != null && os != null && is != null) {
      try {

        //Create a thread for reading from server
        new Thread(new Client()).start();
        while (!closed) {
          os.println(toServer.readLine().trim());
        }
   
        os.close();
        is.close();
        userSocket.close();
      } catch (IOException e) {
        System.out.println("Connection to the Server has been lost.");
      }
    }
  }
 
  //Create a thread to run take input from user until server closes or client chooses to exit.
  public void run() {
    String responseLine;
    try {
      while ((responseLine = is.readLine()) != null) {
        System.out.println(responseLine);
        if (responseLine.indexOf("$$$ Bye") != -1)
          break;
      }
      closed = true;
    } catch (IOException e) {
      System.err.println("IOException:  " + e);
      System.out.println("Unexpected shut down. Goodbye");
      System.exit(1);
    } 
  }
}