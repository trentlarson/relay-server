
import java.io.*;
import java.net.*;

public class EchoServer extends AbstractServer implements AbstractServer.Responder {
  
  public static void main(String[] args) {
    runServer(args, new EchoServer());
  }

  public String response(String message) {
    return message;
  }

}
