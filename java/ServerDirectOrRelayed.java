
import java.io.*;
import java.net.*;

public abstract class ServerDirectOrRelayed {
  
  public abstract String response(String request);

  public void runServer(String[] args) {

    Socket clientSocket = null;

    try {

      if (args.length < 1) {
        System.out.println("Must supply a port number for listening,"
                           + " or a host and a port for the location of a relay server.");

      } else if (args.length < 2) {

        int port = Integer.valueOf(args[0]).intValue();

        ServerSocket serverSocket = null;
        try {
          serverSocket = new ServerSocket(port);

          while(true) { // loop forever, spawning a thread for each new client
            clientSocket = serverSocket.accept();
            new Thread(new ResponseHandler(clientSocket)).start();
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
            new Thread(new ResponseHandler(clientSocket)).start();
            
            // now wait for the next client
            messageIn = incoming.readLine();
          }
        } finally {
          try { incoming.close(); } catch (Exception e2) {}
          try { clientAddressSocket.close(); } catch (Exception e2) {}
        }

      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
    }
  }

  public class ResponseHandler implements Runnable {
    private Socket clientConn = null;
    public ResponseHandler(Socket _clientConn) throws IOException {
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
