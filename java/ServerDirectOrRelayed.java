
import java.io.*;
import java.net.*;

public abstract class ServerDirectOrRelayed {
  
  public abstract String response(String request);

  public void runServer(String[] args) {

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

        while(true) { // loop forever, spawning a thread for each new client
          try {
            clientSocket = serverSocket.accept();
          } catch (IOException e) {
            throw new IOException("Unable to listen for more server connections.", e);
          }
          new Thread(new RequestWaiter(clientSocket, null, null)).start();
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
          String publicHostAndPort = incoming.readLine();
          System.out.println(publicHostAndPort); // publish this to the world
        } catch (IOException e) {
          try { incoming.close(); } catch (Exception e2) {}
          try { outgoing.close(); } catch (Exception e2) {}
          throw new IOException("Unable to route through relay server.", e);
        }
        new Thread(new RequestWaiter(clientSocket, incoming, outgoing)).start();
        
        while (true) { // loop forever while listening across the relay line
          try { Thread.currentThread().sleep(10000); } catch (InterruptedException e) {}
        }

      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
      try { serverSocket.close(); } catch (Exception e) {}
    }
  }

  public class RequestWaiter implements Runnable {
    private Socket clientSocket = null;
    private BufferedReader incoming = null;
    private PrintWriter outgoing = null;
    public RequestWaiter(Socket _clientSocket, BufferedReader _incoming, PrintWriter _outgoing) throws IOException {
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
          outgoing.println(response(messageIn));
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
