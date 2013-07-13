
import java.io.*;
import java.net.*;

public class ServerDirectOrRelayed {
  
  protected interface Responder {
    public String response(String request);
  }

  public static void runServer(String[] args, Responder responder) {

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {

      if (args.length < 1) {
        System.out.println("Must supply a port, or a relay host & port.");
      } else if (args.length < 2) {

        int port = Integer.valueOf(args[0]).intValue();
        try {
          serverSocket = new ServerSocket(port);
        } catch (IOException e) {
          throw new IOException("Unable to open " + port + " to start server.", e);
        }

        while(true) { // loop forever, spawning threads for each connection
          try {
            clientSocket = serverSocket.accept();
          } catch (IOException e) {
            throw new IOException("Unable to listen for more server connections.", e);
          }
          new Thread(new RequestWaiter(responder, clientSocket, null, null)).start();
        }

      } else { // must have supplied a host & port for relay
        
        String host = args[0];
        int port = Integer.valueOf(args[1]).intValue();

        BufferedReader incoming = null;
        PrintWriter outgoing = null;
        try {
          clientSocket = new Socket(host, port);
          incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          outgoing = new PrintWriter(clientSocket.getOutputStream(), true);
          String message = incoming.readLine();
          System.out.println(message); // tell the relayed host & port
        } catch (IOException e) {
          try { incoming.close(); } catch (Exception e2) {}
          try { outgoing.close(); } catch (Exception e2) {}
          throw new IOException("Unable to route through relay server.", e);
        }
        new Thread(new RequestWaiter(responder, clientSocket, incoming, outgoing)).start();
        
        while (true) { } // loop forever while the thread runs

      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
      try { serverSocket.close(); } catch (Exception e) {}
    }
  }

  public static class RequestWaiter implements Runnable {
    private Responder responder = null;
    private Socket clientSocket = null;
    private BufferedReader incoming = null;
    private PrintWriter outgoing = null;
    public RequestWaiter(Responder _responder, Socket _clientSocket, BufferedReader _incoming, PrintWriter _outgoing) throws IOException {
      responder = _responder;
      clientSocket = _clientSocket;
      incoming = _incoming;
      outgoing = _outgoing;

      if (incoming == null) {
        incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      }
      if (outgoing == null) {
        outgoing = new PrintWriter(clientSocket.getOutputStream(), true);
      }
    }
    /**
       Loop forever, responding appropriately to requests.
    */
    public void run() {
      try {
        String messageIn = incoming.readLine();
        while (messageIn != null) { // loop until the stream is closed
          outgoing.println(responder.response(messageIn));
          messageIn = incoming.readLine();
        }
      } catch (IOException e) {
        throw new RuntimeException("Got error communicating messages.", e);
      } finally {
        try { incoming.close(); } catch (Exception e) {}
        try { outgoing.close(); } catch (Exception e) {}
      }
    }    
  }

}
