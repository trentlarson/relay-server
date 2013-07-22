
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Relay server, which creates public connections for servers and clients behind firewalls.
 *
 * Run with "-?" argument to see usage.
 */
public class Relay {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8080;

  private static String host = DEFAULT_HOST;
  private static int port = DEFAULT_PORT;
  private static int verbose = 0;

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-?")) {
        System.out.println("Usage:");
        System.out.println("");
        System.out.println(" java Relay [-h HOST] [-v] [PORT]");
        System.out.println("");
        System.out.println(" -h HOST is the address of the host to advertise (default is localhost)");
        System.out.println(" -v      log verbose messages");
        System.out.println(" -vv     log very verbose messages");
        System.out.println(" PORT    is the port on which the relay listens for servers (default is 8080)");
        System.exit(0);
      } else if (args[i].equals("-h")) {
        i++;
        host = args[i];
      } else if (args[i].equals("-v")) {
        verbose = 1;
      } else if (args[i].equals("-vv")) {
        verbose = 2;
      } else { // assume it's our number argument
        port = Integer.valueOf(args[i]).intValue();
      }
    }


    ServerSocket newServerServerSocket = null;
    Socket newServerConnection = null;

    try {
      newServerServerSocket = new ServerSocket(port);
      while (true) { // loop forever, accepting new servers
        newServerConnection = newServerServerSocket.accept();
        if (verbose > 0) System.out.println( "server connected: " + newServerConnection.getInetAddress()
                                             + ":" + newServerConnection.getPort());
        int portForServer = findNextOpenPortAbove(port);
        PassThroughServerSocket ptss = new PassThroughServerSocket(newServerConnection, host, portForServer);
        new Thread(ptss).start();
      }

    } catch (IOException e) {
      throw new RuntimeException("Got error listening for servers, so shutting down relay.", e);
    } finally {
      try { newServerConnection.close(); } catch (Exception e) {}
      try { newServerServerSocket.close(); } catch (Exception e) {}
    }

  }
    

  /**
   * Use the given public host and port for communications to the server connected by this socket.
   */
  public static class PassThroughServerSocket implements Runnable {
    private Socket serverSocket;
    private String hostForRelay;
    private int clientPort;
    public PassThroughServerSocket(Socket _serverSocket, String _hostForRelay, int _clientPort) {
      serverSocket = _serverSocket;
      hostForRelay = _hostForRelay;
      clientPort = _clientPort;
    }
    public void run() {
      ServerSocket clientServerSocket = null;
      PrintWriter requestToServer = null;
      BufferedReader responseFromServer = null;
      try {

        requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
        responseFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

        requestToServer.println(hostForRelay + ":" + clientPort);

        clientServerSocket = new ServerSocket(clientPort);
        while (true) { // loop forever, accepting clients

          Socket newClientConnection = clientServerSocket.accept();
            
          if (verbose > 0) System.out.println( "client connected: " + newClientConnection.getInetAddress()
                                               + ":" + newClientConnection.getPort());
            
          new Thread(new ClientPassThroughRequestWaiter(serverSocket, newClientConnection,
                                                        requestToServer, responseFromServer)).start();
          
        }
      } catch (IOException e) {
        throw new RuntimeException("Got error listening for clients, so won't listen any more.", e);
      } finally {
        try { responseFromServer.close(); } catch (Exception e) {}
        try { requestToServer.close(); } catch (Exception e) {}
        try { clientServerSocket.close(); } catch (Exception e) {}
      }
    }
  }
    

  /**
     For each client, we have to set up a separate socket for the
     server to talk with that particular client.
   */
  public static class ClientPassThroughRequestWaiter implements Runnable {
    Socket serverSocket = null, newClientConnection = null;
    PrintWriter requestToServer = null;
    BufferedReader responseFromServer = null;
    public ClientPassThroughRequestWaiter(Socket _serverSocket, Socket _newClientConnection,
                                          PrintWriter _requestToServer, BufferedReader _responseFromServer) {
      serverSocket = _serverSocket;
      newClientConnection = _newClientConnection;
      requestToServer = _requestToServer;
    }
    public void run() {
      ServerSocket newServerServerSocketForClient = null;
      try {
        int newPortForServer = findNextOpenPortAbove(newClientConnection.getPort());
        newServerServerSocketForClient = new ServerSocket(newPortForServer);
        ServerConnectionForClient connForClient = new ServerConnectionForClient();
        new Thread(new ServerNewConnectionWaiter(newServerServerSocketForClient, connForClient)).start();
        requestToServer.println(host + ":" + newPortForServer); // send port for linking to this client
        // now hang out and wait until the ServerConnectionForClient is filled in
        while (connForClient.serverSocket == null) {
          try {
            Thread.currentThread().sleep(100); // sleep .1 seconds
          } catch (InterruptedException e) {
            System.err.println("Got interrupted waiting for server " + serverSocket
                               + " to connect to new port at " + newPortForServer
                               + ".  Will continue to wait.");
            e.printStackTrace();
          }
        }
        // now that we've got the new connection for just this server & client, route
        new Thread(new PassThroughRequestWaiter(connForClient.serverSocket, newClientConnection)).start();
      } catch (IOException e) {
        throw new RuntimeException("Got error trying to connect server for client,"
                                   + " so won't serve clients to this this server any more.", e);
      } finally {
        try { newServerServerSocketForClient.close(); } catch (IOException e) {}
      }
    }
  }
    
  /**
     Placeholder for the relay: when it starts a thread to wait for
     the server to reconnect, this is what the server is waiting to
     become non-null.
   */
  private static class ServerConnectionForClient {
    public Socket serverSocket = null;
  }

  /**
   * Listen on this server socket for the server to reconnect to handle the new client.
   */
  public static class ServerNewConnectionWaiter implements Runnable {
    ServerSocket newServerServerSocketForClient = null;
    ServerConnectionForClient connForClient = null;
    public ServerNewConnectionWaiter(ServerSocket _newServerServerSocketForClient,
                                     ServerConnectionForClient _connForClient) {
      newServerServerSocketForClient = _newServerServerSocketForClient;
      connForClient = _connForClient;
    }
    public void run() {
      try {
        // We'll wait here for the server to come back...
        connForClient.serverSocket = newServerServerSocketForClient.accept();
        if (verbose > 1) System.out.println("server " + newServerServerSocketForClient
                                            + " connected back to relay on " + connForClient.serverSocket);
        // ... and now that we've got that new port set, we can exit.
      } catch (IOException e) {
        throw new RuntimeException("Got error trying to connect to this server,"
                                   +" so won't try any more for this client.", e);
      }
    }
  }


  /**
   * Pass messages back-and-forth between the server and the client.
   */
  public static class PassThroughRequestWaiter implements Runnable {
    Socket serverSocket = null, newClientConnection = null;
    public PassThroughRequestWaiter(Socket _serverSocket, Socket _newClientConnection) {
      serverSocket = _serverSocket;
      newClientConnection = _newClientConnection;
    }
    public void run() {
      BufferedReader requestFromClient = null;
      PrintWriter requestToServer = null;
      BufferedReader responseFromServer = null;
      PrintWriter responseToClient = null;
      try {
        requestFromClient = new BufferedReader(new InputStreamReader(newClientConnection.getInputStream()));
        requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
        responseFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        responseToClient = new PrintWriter(newClientConnection.getOutputStream(), true);

        String messageIn = requestFromClient.readLine();
        while (messageIn != null) { // loop until the stream is closed
          requestToServer.println(messageIn);
          if (verbose > 1) System.out.println("Starting message to server "+serverSocket
                                              +" from client "+newClientConnection+"...");
          String messageBack = responseFromServer.readLine();
          if (verbose > 1) System.out.println("... got response from server "+serverSocket
                                              +", so responding to client "+newClientConnection+".");
          responseToClient.println(messageBack);
          messageIn = requestFromClient.readLine();
        }

      } catch (IOException e) {
        throw new RuntimeException("Got error routin client & server request, so giving up.", e);
      } finally {
        try { requestFromClient.close(); } catch (Exception e) {}
        try { requestToServer.close(); } catch (Exception e) {}
        try { responseFromServer.close(); } catch (Exception e) {}
        try { responseToClient.close(); } catch (Exception e) {}
        try { newClientConnection.close(); } catch (Exception e) {}
        try { serverSocket.close(); } catch (Exception e) {}
      }

    }
  }


  /**
   * Look for an open port, starting with port+1.
   */
  public static int findNextOpenPortAbove(int port) throws IOException {
    boolean foundPort = false;
    int nextTrialPort = port;
    ServerSocket serverSocket = null;
    while (!foundPort) {
      nextTrialPort++;
      try {
        serverSocket = new ServerSocket(nextTrialPort);
        foundPort = true;
      } catch (IOException e) {
        // continue with the attempts until we find an open port
        if (nextTrialPort == 65535) throw new IOException("No open port.");
      } finally {
        try { serverSocket.close(); } catch (Exception e) {}
      }
    }
    return nextTrialPort;
  }

}



