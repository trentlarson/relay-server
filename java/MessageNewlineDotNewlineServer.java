
import java.io.*;
import java.net.*;


/**
 * Server to send EOM after newline-dot-newline
 *
 * This assumes all input strings are length less than 1024.
 */
public class MessageNewlineDotNewlineServer extends ServerDirectOrRelayed {
  
  protected ClientResponder getClientResponder() {
    return new ClientResponder() {
      String previousLine = "";
      StringBuilder message = new StringBuilder();
      public String response(String request) {
        if (previousLine.equals("\n")
            && request.equals(".\n")) {
          previousLine = "";
          String result = message.toString();
          message = new StringBuilder();
          return result.toString() + "---\n";
        } else {
          message.append(previousLine);
          previousLine = request;
          return "";
        }
      }
    };
  }

  public static void main(String[] args) {
    new MessageNewlineDotNewlineServer().runServer(args);
  }

}
