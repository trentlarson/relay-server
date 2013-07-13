
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
          System.err.println("Unable to open " + port + " to start relay.");
          throw e;
        }

        while (true) {
          try {
            newServerConnection = newServerServerSocket.accept();
          } catch (IOException e) {
            System.err.println("Unable to listen for more server connections.");
            throw e;
          }

          System.out.println( "???? THE SERVER"+" "+ newServerConnection.getInetAddress() +":"+newServerConnection.getPort()+" IS CONNECTED ");
            
          boolean foundPort = false;
          int nextTrialPort = lastUsedPort;
          ServerSocket clientServerSocket = null;
          while (!foundPort) {
            nextTrialPort++;
            try {
              clientServerSocket = new ServerSocket(nextTrialPort);
              foundPort = true;
            } catch (IOException e) {
              // continue with the attempts until we find an open port
            }
          }
          PassThroughServerSocket ptss = new PassThroughServerSocket(newServerConnection, clientServerSocket);
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
    private ServerSocket clientServerSocket;
    public PassThroughServerSocket(Socket _serverSocket, ServerSocket _clientServerSocket) {
      serverSocket = _serverSocket;
      clientServerSocket = _clientServerSocket;
    }
    public void run() {

      while (true) {
        Socket newClientConnection = null;
        try {

          PrintWriter requestToServer = new PrintWriter(serverSocket.getOutputStream(), true);
          requestToServer.println(serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getPort());

          newClientConnection = clientServerSocket.accept();

          System.out.println( "???? THE client"+" "+ newClientConnection.getInetAddress() +":"+newClientConnection.getPort()+" IS CONNECTED ");
          
          new Thread(new PassThroughRequestWaiter(serverSocket, newClientConnection, requestToServer)).start();

        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try { newClientConnection.close(); } catch (Exception e) {}
        }
            
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
}



