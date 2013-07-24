
import java.io.*;
import java.net.*;

public class EchoServer extends ServerDirectOrRelayed {
  
  protected ClientResponder getClientResponder() {
    return new ClientResponder() {
      public String response(String request) {
        return request;
      }
    };
  }

  public static void main(String[] args) {
    new EchoServer().runServer(args);
  }

}
