
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * Server to wait input number of seconds
 */
public class WaitCustomSecondsServer extends ServerDirectOrRelayed {
  
  public static boolean VERBOSE = true;

  protected ClientResponder getClientResponder() {
    return new ClientResponder() {
      public String response(String request) {
        String request2 = request.substring(0, request.length() - 1); // to strip the LF
        int seconds = Integer.valueOf(request2);
        try {
          long endMillis = System.currentTimeMillis() + (seconds * 1000);
          while (System.currentTimeMillis() < endMillis) {
            Thread.currentThread().sleep(1000);
            if (VERBOSE)  {
              System.out.println("" + ((endMillis - System.currentTimeMillis()) / 1000.0) + " seconds left");
            }
          }
          return "Done with " + seconds + "-second count.\n";
        } catch (InterruptedException e) {
          Date errorTime = new Date();
          System.err.println("Error at " + errorTime + ".");
          e.printStackTrace();
          return "Errored at " + errorTime + "\n";
        }
      }
    };
  }

  public static void main(String[] args) {
    new WaitCustomSecondsServer().runServer(args);
  }

}
