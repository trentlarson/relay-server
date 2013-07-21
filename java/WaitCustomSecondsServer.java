
import java.io.*;
import java.net.*;
import java.util.*;

public class WaitCustomSecondsServer extends ServerDirectOrRelayed {
  
  public String response(String request) {
    int seconds = Integer.valueOf(request);
    try {
      long endMillis = System.currentTimeMillis() + (seconds * 1000);
      while (System.currentTimeMillis() < endMillis) {
        Thread.currentThread().sleep(1000);
        System.out.println("" + ((endMillis - System.currentTimeMillis()) / 1000.0) + " seconds left");
      }
      return "Done with " + seconds + "-second count.";
    } catch (InterruptedException e) {
      Date errorTime = new Date();
      System.err.println("Error at " + errorTime + ".");
      e.printStackTrace();
      return "Errored at " + errorTime;
    }
  }

  public static void main(String[] args) {
    new WaitCustomSecondsServer().runServer(args);
  }

}
