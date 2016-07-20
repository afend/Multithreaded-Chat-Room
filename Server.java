import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/*
 * A chat server that delivers public and private messages.
 */
public class Server {
    
  //12 current colors, need this number to make sure we dont go outofbounds on color array
  static final Integer COLORCOUNT = 12;
  
  public static ArrayList < String > activeUsers = new ArrayList<String>();  
  // The server socket.
  private static ServerSocket serverSocket = null;
  // The client socket.
  private static Socket clientSocket = null;

  // This chat server can accept up to maxClientsCount clients' connections.
  private static final int maxClientsCount = 20;
  private static final clientThread[] threads = new clientThread[maxClientsCount];
 
    public static void main(String args[]) throws IOException{
        
    //Create a new file object for chat history
    File f = new File("ChatHistory.txt");
                
    //If the file does not exist, create it.
    if(!f.exists())
         f.createNewFile();
                
    //Clear the existing chat history
    BufferedWriter out = new BufferedWriter(new FileWriter("ChatHistory.txt", false));
    
    int portNumber = Integer.parseInt(args[0]);

    // The default port number.
    int colorNumber = 0;
    if (args.length != 1) {
      System.out.println("Please only specify the port number when creating Server." );
      System.exit(1);
    }

    /*
     * Open a server socket on the portNumber (default 2222). Note that we can
     * not choose a port less than 1023 if we are not privileged users (root).
     */
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      System.out.println(e);
    }

    /*
     * Create a client socket for each connection and pass it to a new client
     * thread.
     */
    while (true) {
      try {
        clientSocket = serverSocket.accept();
        int i = 0;
        for (i = 0; i < maxClientsCount; i++) {
          if (threads[i] == null) {
            (threads[i] = new clientThread(clientSocket, threads, colorNumber)).start();
                colorNumber++;
                    //If were past the last element in the array we need to start over
            if(colorNumber > (COLORCOUNT - 1))
                colorNumber = 0;
            break;
          }
        }
        if (i == maxClientsCount) {
          PrintStream os = new PrintStream(clientSocket.getOutputStream());
          os.println("Server too busy. Try later.");
          os.close();
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}




/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */

class clientThread extends Thread {
    
      //Colors for users
  public static final String RED = "\033[31m";
  public static final String GREEN = "\033[32m";
  public static final String YELLOW = "\033[33m";
  public static final String BLUE = "\033[34m";
  public static final String PURPLE = "\033[35m";
  public static final String CYAN = "\033[36m";
  public static final String WHITE = "\033[37m";
  public static final String BRIGHTGREEN = "\033[2;32m";
  public static final String BRIGHTRED = "\033[2;31m";
  public static final String BRIGHTBLUE = "\033[2;34m";
  public static final String BRIGHTPURPLE = "\033[2;35m";
  public static final String BRIGHTCYAN = "\033[2;36m";
  
  //have to finish with reset for color
  public static final String RESET = "\033[0m";    

  //Array for with all possible colors for users
  public String[] COLORS = {RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE, BRIGHTGREEN, BRIGHTRED, BRIGHTBLUE, BRIGHTPURPLE, BRIGHTCYAN};

  private final ReentrantLock lock = new ReentrantLock();
  private String clientName = null;
  private String clientColor = null;
  private DataInputStream is = null;
  private PrintStream os = null;
  private Socket socket = null;
  private final clientThread[] threads;
  public ArrayList<clientThread> active = new ArrayList();
  private int maxUserCount;

  public clientThread(Socket clientSocket, clientThread[] threads, Integer colorNumber) {
    
    this.socket = clientSocket;
    this.threads = threads;
    this.clientColor = COLORS[colorNumber];
    maxUserCount = threads.length;
  }
  
  
  public void ReadHistory() throws Exception
  {
        BufferedReader in = new BufferedReader(new FileReader("ChatHistory.txt"));
        String line;
        while((line = in.readLine()) != null)
        {
            os.println(line); //displaying all previous chat history
        }
  }
  
  public clientThread releaseClientSocket(clientThread[] privateUsers, String name)
  {
        synchronized (this) 
        {
            for (int i = 0; i < maxUserCount; i++) 
            {
                if (privateUsers[i] != null && privateUsers[i] != this
                && privateUsers[i].clientName != null
                && privateUsers[i].clientName.equals(name)) 
                {
                    return privateUsers[i];
                }
            }
        }
    return null;                 
  }

  public void run(){
    int maxUserCount = this.maxUserCount;
    clientThread[] activeUsers = this.threads;
    try (FileWriter fw = new FileWriter("ChatHistory.txt", true);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter toHistory = new PrintWriter(bw)){

       // Create input and output streams for this client.

      is = new DataInputStream(socket.getInputStream());
      os = new PrintStream(socket.getOutputStream());
      
      
      os.println("Please login using @name <your user name>");
            String name = "";
            while (true) {
                name = is.readLine().trim();
                if (name.startsWith("@name")) {
                    name = name.substring(6, name.length());
                    if (!Server.activeUsers.contains(name)) {
                        Server.activeUsers.add(name);
                        break;
                    } else {
                        os.println("Error: Name already taken! Please select another name.");
                    }
                } else {
                    os.println("Please login using @name <your user name>");
                }
            }
      // Welcome the new the client. 
      os.println("Welcome " + name + " to our chat room. To leave enter @exit in a new line.");
      ReadHistory();
      synchronized (this) {
        for (int i = 0; i < maxUserCount; i++) {
          if (activeUsers[i] != null && activeUsers[i] == this) {
            clientName = name;
            break;
          }
        }
        for (int i = 0; i < maxUserCount; i++) {
          if (activeUsers[i] != null && activeUsers[i] != this) {
            activeUsers[i].os.println("$$$ User " + name + " has entered the chat room! $$$  ");
          }
        }
      }
      // Start the conversation. 
      while (true) {
        String line = is.readLine();
        if (line.startsWith("@exit")) {
          break;
        }
        else if(line.startsWith("@end"))
        {
            if(active.isEmpty())
                this.os.println("Cannot end private chat because you are not in any");
            else
            {
                String[] words = line.split("\\s", 2);
                if (words.length > 1 && words[1] != null) 
                {
                    words[1] = words[1].trim();
                    if (!words[1].isEmpty()) 
                    {
                        for (int i=0; i < active.size(); i++) 
                        {
                            if(active.get(i).clientName.equals(words[1]))
                            {
                                active.remove(i);
                                releaseClientSocket(activeUsers, words[1]).lock.unlock();
                                this.os.println("Stopped private communication with user: " + words[1]);
                            }
                        }
                    }
                }
            }
        }
        else if (line.startsWith("@who")) {
             for (String temp: Server.activeUsers) {
                        os.println(temp);
                    }
                }
        //If the message is private sent it to the given client. 
        else if (line.startsWith("@private")) {
          String[] who = line.split("\\s", 2);
          //System.out.println(words[0]);
          if (who.length > 1 && who[1] != null) {
            who[1] = who[1].trim();
            if (!who[1].isEmpty()) 
            {
              synchronized (this) 
              {
                for (int i = 0; i < maxUserCount; i++) 
                {
                  if (activeUsers[i] != null && activeUsers[i] != this
                      && activeUsers[i].clientName != null
                      && activeUsers[i].clientName.equals(who[1])) 
                  {
                      if( activeUsers[i].lock.tryLock() )
                      {
                        active.add(activeUsers[i]);
                        this.os.println("Private communication has begun with User: " + who[1]);
                      }
                      else
                        this.os.println("User is already in a private communication with another user: " + who[1] 
                                        + "\nPlease try again in a little while.");

                    break;
                  }
                }
              }
            }
          }
        } else if(!active.isEmpty()) 
        {
            for (clientThread ct : active) {
                ct.os.println(clientColor + name + "::" + line + RESET);
            }
        }
        else {
                toHistory.println(clientColor + name + "::" + line + RESET);
                toHistory.flush();
          // The message is public, broadcast it to all other clients. 
          synchronized (this){
            for (int i = 0; i < maxUserCount; i++) {
              if (activeUsers[i] != null && activeUsers[i].clientName != null) {
                activeUsers[i].os.println(clientColor + name + "::" + line + RESET);
                //If a message is being broadcast, write it to file
              }
            }
          }
        }
      }
      synchronized (this) {
        for (int i = 0; i < maxUserCount; i++) {
          if (activeUsers[i] != null && activeUsers[i] != this
              && activeUsers[i].clientName != null) {
            activeUsers[i].os.println("*** The user " + name
                + " is leaving the chat room !!! ***");
          }
        }
      }
      os.println("*** Bye " + name + " ***");

      
        //Clean up. Set the current thread variable to null so that a new client
        //could be accepted by the server.
       
      synchronized (this) {
        for (int i = 0; i < maxUserCount; i++) {
          if (activeUsers[i] == this) {
            activeUsers[i] = null;
          }
        }
      }
      
        // Close the output stream, close the input stream, close the socket.
       
      is.close();
      os.close();
      socket.close();
      toHistory.close();
    } catch (IOException e) {
    }
    catch (Exception e) {
    }
  }
} 