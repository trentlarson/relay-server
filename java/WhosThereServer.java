
import java.io.*;
import java.net.*;

public class WhosThereServer {
  
  public static void main(String[] args) {
    ServerDirectOrRelayed.runServer(args, new ServerDirectOrRelayed.Responder() {
        public String response(String request) {
          return "Who's there?!";
        }
      });
  }

}
