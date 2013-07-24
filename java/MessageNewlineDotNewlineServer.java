
import java.io.*;
import java.net.*;

public class MessageNewlineDotNewlineServer extends ServerDirectOrRelayed {
  
  protected ClientResponder getClientResponder() {
    return new ClientResponder() {
      boolean gotBlankLineLast = false;
      public String response(String request) {
        if (request.equals(".")
            && gotBlankLineLast) {
          gotBlankLineLast = false;
          return "EOM";
        } else {
          if (request.equals("")) {
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
