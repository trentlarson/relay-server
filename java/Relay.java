
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
          e.printStackTrace();
          System.exit(1);
          try { newServerServerSocket.close(); } catch (Exception e2) {}
        }

        while(true) {
          try {
            newServerConnection = newServerServerSocket.accept();
          } catch (IOException e) {
            System.err.println("Unable to listen for more server connections.");
            e.printStackTrace();
            System.exit(1);
          }

          System.out.println( "???? THE SERVER"+" "+ newServerConnection.getInetAddress() +":"+newServerConnection.getPort()+" IS CONNECTED ");
            
          //BufferedReader inFromServer = new BufferedReader(new InputStreamReader(newServerConnection.getInputStream()));
            
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
          //serverSockets.put(clientServerSocket.getLocalPort(), clientServerSocket);
          PassThroughServerSocket ptss = new PassThroughServerSocket(newServerConnection, clientServerSocket);
          new Thread(ptss).start();
          
          /**
          try {
            PrintWriter outToServer = new PrintWriter(newServerConnection.getOutputStream(),true);
            outToServer.print("????? connected!" + "\n");
          } catch (IOException e) {
            System.err.println("Unable to open connection to server.");
            e.printStackTrace();
            System.exit(1);
          }
          **/
        }
      } finally {
        try { newServerConnection.close(); } catch (Exception e) {}
        try { newServerServerSocket.close(); } catch (Exception e) {}
      }

/**
      BufferedReader newServerIn = null;
      PrintWriter newServerOut = null;

      try {
        newServerSocket = new Socket(host, port);
        newServerIn = new BufferedReader(new InputStreamReader(newServerSocket.getInputStream()));
        newServerOut = new PrintWriter(newServerSocket.getOutputStream(), true);
        
        String newServer = newServerIn.readLine();
        while(newServer != null) {
          System.out.println("hi");
          //clientSocket;

        }
      } catch (UnknownHostException e) {
        System.err.println("Don't know about host: " + host);
        e.printStackTrace();
        System.exit(1);
      } catch (IOException e) {
        System.err.println("Couldn't get I/O for the connection to: " + host + ":" + port);
        e.printStackTrace();
        System.exit(1);
      } finally {
        try { newServerIn.close(); } catch (Exception e) {}
        try { newServerOut.close(); } catch (Exception e) {}
        try { newServerIn.close(); } catch (Exception e) {}
        try { newServerSocket.close(); } catch (Exception e) {}
      }
**/
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
        BufferedReader requestFromClient = null, responseFromServer = null;
        PrintWriter requestToServer = null, responseToClient = null;
        try {
          try {
            newClientConnection = clientServerSocket.accept();
          } catch (IOException e) {
            System.err.println("Unable to listen for more client connections.");
            e.printStackTrace();
            System.exit(1);
          }

          System.out.println( "???? THE client"+" "+ newClientConnection.getInetAddress() +":"+newClientConnection.getPort()+" IS CONNECTED ");
          
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
            System.err.println("Unable to read request or write response.");
            e.printStackTrace();
            System.exit(1);
          }
        } finally {
          try { requestFromClient.close(); } catch (Exception e) {}
          try { responseFromServer.close(); } catch (Exception e) {}
          try { requestToServer.close(); } catch (Exception e) {}
          try { responseToClient.close(); } catch (Exception e) {}
          try { newClientConnection.close(); } catch (Exception e) {}
        }
            
      }
    }
  }


}



