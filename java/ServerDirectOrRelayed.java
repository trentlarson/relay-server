
import java.io.*;
import java.net.*;

/**
 * Server which can run as its own server or through a Relay server.
 *
 * Usage: extend the class, implement the response method, then create
 * a 'main' method for the new class that instantiates itself and
 * calls 'runServer'.
 */
public abstract class ServerDirectOrRelayed {
  
  protected interface ClientResponder {
    public String response(String request);
  }
  protected abstract ClientResponder getClientResponder();



  public void runServer(String[] args) {

    Socket clientSocket = null;
    int clientNum = 0;

    try {

      if (args.length < 1
          || args[0].equals("-?")) {
        System.out.println("Must supply a port number for listening,"
                           + " or a host and a port for the location of a relay server.");

      } else if (args.length < 2) {

        int port = Integer.valueOf(args[0]).intValue();

        ServerSocket serverSocket = null;
        try {
          serverSocket = new ServerSocket(port);

          while(true) { // loop forever, spawning a thread for each new client
            clientSocket = serverSocket.accept();
            new Thread(new ResponseHandler(getClientResponder(), clientSocket)).start();
          }
        } finally {
          try { serverSocket.close(); } catch (Exception e) {}
        }

      } else { // must have supplied a host & port for relay
        
        String host = args[0];
        int port = Integer.valueOf(args[1]).intValue();

        Socket clientAddressSocket = null;
        BufferedReader incoming = null;
        try {
          clientAddressSocket = new Socket(host, port);

          // grab public address from the relay
          incoming = new BufferedReader(new InputStreamReader(clientAddressSocket.getInputStream()));
          String publicHostAndPort = incoming.readLine();
          System.out.println(publicHostAndPort); // this is the HOST:PORT to give clients

          // now listen on that socket for new connections to make for new clients
          String messageIn = incoming.readLine();
          while (messageIn != null) { // loop until the stream is closed
            // parse out the host and port where to connect for this new client
            String clientHost = messageIn.substring(0, messageIn.indexOf(":"));
            int clientPort = Integer.valueOf(messageIn.substring(messageIn.indexOf(":") + 1));
            clientSocket = new Socket(clientHost, clientPort);
            new Thread(new ResponseHandler(getClientResponder(), clientSocket)).start();
            
            // now wait for the next client
            messageIn = incoming.readLine();
          }
        } finally {
          try { incoming.close(); } catch (Exception e2) {}
          try { clientAddressSocket.close(); } catch (Exception e2) {}
        }

      }

    } catch (IOException e) {
      throw new RuntimeException("Got error running the server, so it's aborting.", e);
    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
    }
  }

  /**
   * Thread with a client connection to loop and respond to data.
   */
  public class ResponseHandler implements Runnable {
    private ClientResponder clientResponder = null;
    private Socket clientConn = null;
    public ResponseHandler(ClientResponder _clientResponder, Socket _clientConn) throws IOException {
      clientResponder = _clientResponder;
      clientConn = _clientConn;
    }
    /**
       Send output when finished processing (which may take time).
     */
    public void run() {
      BufferedReader incoming = null;
      PrintWriter outgoing = null;
      try {
        incoming = new BufferedReader(new InputStreamReader(clientConn.getInputStream()));
        outgoing = new PrintWriter(clientConn.getOutputStream(), true);
        String messageIn = incoming.readLine();
        while (messageIn != null) {
          outgoing.println(clientResponder.response(messageIn));
          messageIn = incoming.readLine();
        }
      } catch (IOException e) {
        throw new RuntimeException("Got an error communicating with a client, so we're aborting it.", e);
      } finally {
        try { outgoing.close(); } catch (Exception e) {}
        try { incoming.close(); } catch (Exception e) {}
        try { clientConn.close(); } catch (Exception e) {}
      }
    }
  }

}
