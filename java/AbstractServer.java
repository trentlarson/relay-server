
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
      } else {

        if (args.length < 2) {

          int port = Integer.valueOf(args[0]).intValue();
          try {
            serverSocket = new ServerSocket(port);
          } catch (IOException e) {
            System.err.println("Unable to open " + port + " to start server.");
            System.exit(1);
            try { serverSocket.close(); } catch (Exception e2) {}
          }

          PrintWriter outgoing = null;
          BufferedReader incoming = null;

          try {
            while(true) {
              try {
                clientSocket = serverSocket.accept();
              } catch (IOException e) {
                System.err.println("Unable to listen for more server connections.");
                System.exit(1);
              }
              incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
              outgoing = new PrintWriter(clientSocket.getOutputStream(), true);

              String messageIn = incoming.readLine();
              while (messageIn != null) {
                outgoing.println(responder.response(messageIn));
                messageIn = incoming.readLine();
              }
            }
          } catch (IOException e) {
            System.err.println("Got error communicating messages.");
            System.exit(1);
          } finally {
            try { incoming.close(); } catch (Exception e) {}
            try { outgoing.close(); } catch (Exception e) {}
          }

        } else {
          System.err.println("???? unimplemented");
        }

      }

    } finally {
      try { clientSocket.close(); } catch (Exception e) {}
      try { serverSocket.close(); } catch (Exception e) {}
    }
  }

}
