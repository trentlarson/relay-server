
import java.io.*;
import java.net.*;

public abstract class ServerDirectOrRelayed {
  
  public abstract String response(String request);

  public void runServer(String[] args) {

    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    Socket clientAddressSocket = null;

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
          clientAddressSocket = new Socket(host, port);
          incoming = new BufferedReader(new InputStreamReader(clientAddressSocket.getInputStream()));
          outgoing = new PrintWriter(clientAddressSocket.getOutputStream(), true);
          String publicHostAndPort = incoming.readLine();
          System.out.println(publicHostAndPort); // publish this to the world
        } catch (IOException e) {
          try { incoming.close(); } catch (Exception e2) {}
          try { outgoing.close(); } catch (Exception e2) {}
          throw new IOException("Unable to route through relay server.", e);
        }
        new RequestWaiter(clientAddressSocket, incoming, outgoing).run();

      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
      try { serverSocket.close(); } catch (Exception e) {}
    }
  }

  public class RequestWaiter implements Runnable {
    private Socket newClientAddressSocket = null;
    private BufferedReader incoming = null;
    private PrintWriter outgoing = null;
    public RequestWaiter(Socket _newClientAddressSocket, BufferedReader _incoming, PrintWriter _outgoing) throws IOException {
      newClientAddressSocket = _newClientAddressSocket;
      incoming = _incoming;
      outgoing = _outgoing;

      if (incoming == null) {
        incoming = new BufferedReader(new InputStreamReader(newClientAddressSocket.getInputStream()));
      }
      if (outgoing == null) {
        outgoing = new PrintWriter(newClientAddressSocket.getOutputStream(), true);
      }
    }
    /**
       Loop forever, responding appropriately to requests.
    */
    public void run() {
      try {
        String messageIn = incoming.readLine();
        while (messageIn != null) { // loop until the stream is closed
          // parse out the host and port where to connect for this new client
          String clientHost = messageIn.substring(0, messageIn.indexOf(":"));
          int clientPort = Integer.valueOf(messageIn.substring(messageIn.indexOf(":") + 1));
          new Thread(new ResponseHandler(clientHost, clientPort)).start();

          // now wait for the next client
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

  public class ResponseHandler implements Runnable {
    private String clientHost = null;
    private int clientPort = 0;
    public ResponseHandler(String _clientHost, int _clientPort) throws IOException {
      clientHost = _clientHost;
      clientPort = _clientPort;
    }
    /**
       Send output when finished processing (which may take time).
     */
    public void run() {
      Socket clientConn = null;
      BufferedReader incoming = null;
      PrintWriter outgoing = null;
      try {
        clientConn = new Socket(clientHost, clientPort);
        incoming = new BufferedReader(new InputStreamReader(clientConn.getInputStream()));
        outgoing = new PrintWriter(clientConn.getOutputStream(), true);
        String messageIn = incoming.readLine();
        while (messageIn != null) {
          outgoing.println(response(messageIn));
          messageIn = incoming.readLine();
        }
      } catch (IOException e) {
        throw new RuntimeException("Got an error communicating with a client.", e);
      } finally {
        try { outgoing.close(); } catch (Exception e) {}
        try { incoming.close(); } catch (Exception e) {}
        try { clientConn.close(); } catch (Exception e) {}
      }
    }
  }

}
