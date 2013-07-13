
import java.io.*;
import java.net.*;
import java.util.*;

public class Relay {

  public static HashMap<Integer, ServerSocket> serverSockets = new HashMap<Integer, ServerSocket>();
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

        while (true) {
          try {
            newServerConnection = newServerServerSocket.accept();
          } catch (IOException e) {
            throw new IOException("Unable to listen for more server connections.", e);
          }

          System.out.println( "???? THE SERVER"+" "+ newServerConnection.getInetAddress() +":"+newServerConnection.getPort()+" IS CONNECTED ");
            
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
      try {
        clientServerSocket = new ServerSocket(clientPort);
        while (true) {
          Socket newClientConnection = null;
          try {
            
            PrintWriter requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
            requestToServer.println(serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());
            
            newClientConnection = clientServerSocket.accept();
            
            System.out.println( "???? THE client"+" "+ newClientConnection.getInetAddress() +":"+newClientConnection.getPort()+" IS CONNECTED ");
            
            new Thread(new PassThroughRequestWaiter(serverSocket, newClientConnection, requestToServer)).start();
            
          } finally {
            try { newClientConnection.close(); } catch (Exception e) {}
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try { clientServerSocket.close(); } catch (Exception e) {}
      }
    }
  }
    

  public static class PassThroughRequestWaiter implements Runnable {
    Socket serverSocket = null, newClientConnection = null;
    PrintWriter requestToServer = null;
    public PassThroughRequestWaiter(Socket _serverSocket, Socket _newClientConnection, PrintWriter _requestToServer) {
      serverSocket = _serverSocket;
      newClientConnection = _newClientConnection;
      requestToServer = _requestToServer;
    }
    public void run() {
      BufferedReader requestFromClient = null, responseFromServer = null;
      PrintWriter responseToClient = null;
      try {
        requestFromClient = new BufferedReader(new InputStreamReader(newClientConnection.getInputStream()));
        requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
        responseFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        responseToClient = new PrintWriter(newClientConnection.getOutputStream(), true);

        String messageIn = requestFromClient.readLine();
        while (messageIn != null) {
          requestToServer.println(messageIn);
          String messageBack = responseFromServer.readLine();
          responseToClient.println(messageBack);
          messageIn = requestFromClient.readLine();
        }

      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try { requestFromClient.close(); } catch (Exception e) {}
        try { responseFromServer.close(); } catch (Exception e) {}
        try { requestToServer.close(); } catch (Exception e) {}
        try { responseToClient.close(); } catch (Exception e) {}
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



