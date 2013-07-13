
import java.io.*;
import java.net.*;

public class AbstractServer {
  
  protected interface Responder {
    public String response(String message);
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
          System.err.println("Unable to open " + port + " to start server.");
          throw e;
        }

        while(true) {
          try {
            clientSocket = serverSocket.accept();
          } catch (IOException e) {
            System.err.println("Unable to listen for more server connections.");
            throw e;
          }
          new Thread(new RequestWaiter(responder, clientSocket, null)).start();
        }

      } else { // must have supplied a host & port for relay
        
        String host = args[0];
        int port = Integer.valueOf(args[1]).intValue();

          BufferedReader incoming = null;
          try {
            clientSocket = new Socket(host, port);
            incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = incoming.readLine();
            System.out.println(message); // tell the relayed host & port
          } catch (IOException e) {
            System.err.println("Unable to route through relay server.");
            throw e;
          } finally {
            try { incoming.close(); } catch (Exception e) {}
          }
          new Thread(new RequestWaiter(responder, clientSocket, incoming)).start();
          System.out.println("??? done with main");

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
    public RequestWaiter(Responder _responder, Socket _clientSocket, BufferedReader _incoming) throws IOException {
      responder = _responder;
      clientSocket = _clientSocket;
    }
    /**
       Loop forever, responding appropriately to requests.
    */
    public void run() {
      PrintWriter outgoing = null;
      try {
        if (incoming == null) {
          incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        outgoing = new PrintWriter(clientSocket.getOutputStream(), true);
      
        String messageIn = incoming.readLine();
        while (messageIn != null) {
          outgoing.println(responder.response(messageIn));
          messageIn = incoming.readLine();
        }
      } catch (IOException e) {
        System.err.println("Got error communicating messages.");
        e.printStackTrace();
      } finally {
        try { incoming.close(); } catch (Exception e) {} // should let outer if passed in
        try { outgoing.close(); } catch (Exception e) {}
      }
    }    
  }

}
