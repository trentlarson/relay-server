
import java.io.*;
import java.net.*;
import java.util.*;

public class WaitOneSecondLessServer extends ServerDirectOrRelayed {
  
  public static boolean VERBOSE = true;

  int seconds = 0;
  public WaitOneSecondLessServer(int _seconds) {
    seconds = _seconds;
  }

  protected ClientResponder getClientResponder() {
    return new ClientResponder() {
      public String response(String request) {
        seconds = seconds - 1;
        int rememberMySeconds = seconds;
        try {
          long endMillis = System.currentTimeMillis() + (rememberMySeconds * 1000);
          while (System.currentTimeMillis() < endMillis) {
            Thread.currentThread().sleep(1000);
            if (VERBOSE) {
              System.out.println("" + ((endMillis - System.currentTimeMillis()) / 1000.0) + " seconds left");
            }
          }
          return "Done with " + rememberMySeconds + "-second count.";
        } catch (InterruptedException e) {
          Date errorTime = new Date();
          System.err.println("Interrupted at " + errorTime + ".");
          e.printStackTrace();
          return "Interrupted at " + errorTime;
        }
      }
    };
  }

  public static void main(String[] args) {
    new WaitOneSecondLessServer(Integer.valueOf(args[args.length - 1]))
      .runServer(Arrays.copyOf(args, args.length - 1));
  }

}
