
import java.io.*;
import java.net.*;
import java.util.*;

public class Relay {

  public static String DEFAULT_HOST = "localhost";
  public static int DEFAULT_PORT = 8080;

  public static void main(String[] args) {
    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    boolean verbose = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-?")) {
        System.out.println("Usage:");
        System.out.println("");
        System.out.println(" java Relay [-h HOST] [-v] [PORT]");
        System.out.println("");
        System.out.println(" -h HOST is the address of the host to advertise (default is localhost)");
        System.out.println(" -v      log verbose messages");
        System.out.println(" PORT    is the port on which the relay listens for servers (default is 8080)");
        System.exit(0);
      } else if (args[i].equals("-h")) {
        i++;
        host = args[i];
      } else if (args[i].equals("-v")) {
        verbose = true;
      } else { // assume it's our number argument
        port = Integer.valueOf(args[i]).intValue();
      }
    }


    ServerSocket newServerServerSocket = null;
    Socket newServerConnection = null;

    try {

      try {
        newServerServerSocket = new ServerSocket(port);
      } catch (IOException e) {
        throw new IOException("Unable to open " + port + " to start relay.", e);
      }

      while (true) { // loop forever, accepting new servers
        try {
          newServerConnection = newServerServerSocket.accept();
        } catch (IOException e) {
          throw new IOException("Unable to listen for more server connections.", e);
        }

        if (verbose) System.out.println( "server connected: " + newServerConnection.getInetAddress() + ":" + newServerConnection.getPort());
            
        int portForServer = findNextOpenPortAbove(port);
        PassThroughServerSocket ptss = new PassThroughServerSocket(newServerConnection, host, portForServer, verbose);
        new Thread(ptss).start();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try { newServerConnection.close(); } catch (Exception e) {}
      try { newServerServerSocket.close(); } catch (Exception e) {}
    }

  }
    

  public static class PassThroughServerSocket implements Runnable {
    private Socket serverSocket;
    private String hostForRelay;
    private int clientPort;
    private boolean verbose;
    public PassThroughServerSocket(Socket _serverSocket, String _hostForRelay, int _clientPort, boolean _verbose) {
      serverSocket = _serverSocket;
      hostForRelay = _hostForRelay;
      clientPort = _clientPort;
      verbose = _verbose;
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
        while (true) { // loop forever

          Socket newClientConnection = clientServerSocket.accept();
            
          if (verbose) System.out.println( "client connected: " + newClientConnection.getInetAddress() + ":" + newClientConnection.getPort());
            
          new Thread(new PassThroughRequestWaiter(serverSocket, newClientConnection, requestToServer, responseFromServer)).start();
          
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        try { responseFromServer.close(); } catch (Exception e) {}
        try { requestToServer.close(); } catch (Exception e) {}
        try { clientServerSocket.close(); } catch (Exception e) {}
      }
    }
  }
    

  public static class PassThroughRequestWaiter implements Runnable {
    Socket serverSocket = null, newClientConnection = null;
    PrintWriter requestToServer = null;
    BufferedReader responseFromServer = null;
    public PassThroughRequestWaiter(Socket _serverSocket, Socket _newClientConnection, PrintWriter _requestToServer, BufferedReader _responseFromServer) {
      serverSocket = _serverSocket;
      newClientConnection = _newClientConnection;
      requestToServer = _requestToServer;
      responseFromServer = _responseFromServer;
    }
    public void run() {
      BufferedReader requestFromClient = null;
      PrintWriter responseToClient = null;
      try {
        requestFromClient = new BufferedReader(new InputStreamReader(newClientConnection.getInputStream()));
        responseToClient = new PrintWriter(newClientConnection.getOutputStream(), true);

        String messageIn = requestFromClient.readLine();
        while (messageIn != null) { // loop until the stream is closed
          requestToServer.println(messageIn);
          String messageBack = responseFromServer.readLine();
          responseToClient.println(messageBack);
          messageIn = requestFromClient.readLine();
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        try { requestFromClient.close(); } catch (Exception e) {}
        try { responseToClient.close(); } catch (Exception e) {}
        try { newClientConnection.close(); } catch (Exception e) {}
      }

    }
  }


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



