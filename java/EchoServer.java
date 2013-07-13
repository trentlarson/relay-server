
import java.io.*;
import java.net.*;

public class EchoServer extends AbstractServer {
  
  public static void main(String[] args) {
    runServer(args, new AbstractServer.Responder() {
        public String response(String request) {
          return request;
        }
      });
  }


}
