
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
      boolean gotBlankLineLast = false;
      public String response(String request) {
        String request2 = request.substring(0, request.length() - 1); // to strip the LF
        if (request2.equals(".")
            && gotBlankLineLast) {
          gotBlankLineLast = false;
          return "EOM";
        } else {
          if (request2.equals("")) {
            gotBlankLineLast = true;
          } else {
            gotBlankLineLast = false;
          }
          return "";
        }
      }
    };
  }

  public static void main(String[] args) {
    new MessageNewlineDotNewlineServer().runServer(args);
  }

}
