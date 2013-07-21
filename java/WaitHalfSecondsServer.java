
import java.io.*;
import java.net.*;
import java.util.*;

public class WaitHalfSecondsServer extends ServerDirectOrRelayed {
  
  int seconds = 0;
  public WaitHalfSecondsServer(int _seconds) {
    seconds = _seconds;
  }

  public String response(String request) {
    seconds = seconds / 2;
    int rememberMySeconds = seconds;
    try {
      long endMillis = System.currentTimeMillis() + (rememberMySeconds * 1000);
      while (System.currentTimeMillis() < endMillis) {
        Thread.currentThread().sleep(1000);
        System.out.println("" + ((endMillis - System.currentTimeMillis()) / 1000.0) + " seconds left");
      }
      return "Done with " + rememberMySeconds + "-second count.";
    } catch (InterruptedException e) {
      Date errorTime = new Date();
      System.err.println("Error at " + errorTime + ".");
      e.printStackTrace();
      return "Errored at " + errorTime;
    }
  }

  public static void main(String[] args) {
    new WaitHalfSecondsServer(Integer.valueOf(args[2])).runServer(args);
  }

}
