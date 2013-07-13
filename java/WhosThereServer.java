
import java.io.*;
import java.net.*;

public class WhosThereServer extends ServerDirectOrRelayed {
  
  public static void main(String[] args) {
    runServer(args, new ServerDirectOrRelayed.Responder() {
        public String response(String request) {
          return "Who's there?!";
        }
      });
  }

}
