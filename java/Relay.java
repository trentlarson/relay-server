
import java.io.*;
import java.net.*;
import java.util.*;

public class Relay {

  public static String host = "localhost";
  public static int lastUsedPort = 8080;

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Must supply a port.");
    } else {
      int port = Integer.valueOf(args[0]).intValue();
      lastUsedPort = port;

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

          //System.out.println( "server connected: " + newServerConnection.getInetAddress() + ":" + newServerConnection.getPort());
            
          int portForServer = findNextOpenPortAbove(lastUsedPort);
          PassThroughServerSocket ptss = new PassThroughServerSocket(newServerConnection, portForServer);
          new Thread(ptss).start();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try { newServerConnection.close(); } catch (Exception e) {}
        try { newServerServerSocket.close(); } catch (Exception e) {}
      }

    }
  }
    

  public static class PassThroughServerSocket implements Runnable {
    private Socket serverSocket;
    private int clientPort;
    public PassThroughServerSocket(Socket _serverSocket, int _clientPort) {
      serverSocket = _serverSocket;
      clientPort = _clientPort;
    }
    public void run() {
      ServerSocket clientServerSocket = null;
      PrintWriter requestToServer = null;
      BufferedReader responseFromServer = null;
      try {

        requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
        responseFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

        requestToServer.println(host + ":" + clientPort);

        clientServerSocket = new ServerSocket(clientPort);
        while (true) { // loop forever

          Socket newClientConnection = clientServerSocket.accept();
            
          //System.out.println( "client connected: " + newClientConnection.getInetAddress() + ":" + newClientConnection.getPort());
            
          new Thread(new PassThroughRequestWaiter(serverSocket, newClientConnection, requestToServer, responseFromServer)).start();
          
        }
      } catch (IOException e) {
        e.printStackTrace();
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
        e.printStackTrace();
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



