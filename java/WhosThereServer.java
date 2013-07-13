
import java.io.*;
import java.net.*;

public class WhosThereServer extends AbstractServer {
  
  public static void main(String[] args) {
    runServer(args, new AbstractServer.Responder() {
        public String response(String request) {
          return "Who's there?!";
        }
      });
  }

}
