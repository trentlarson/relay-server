
import java.io.*;
import java.net.*;

public class EchoServer extends ServerDirectOrRelayed {
  
  public static void main(String[] args) {
    runServer(args, new ServerDirectOrRelayed.Responder() {
        public String response(String request) {
          return request;
        }
      });
  }


}
