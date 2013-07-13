
import java.io.*;
import java.net.*;

public class WhosThereServer extends AbstractServer implements AbstractServer.Responder {
  
  public static void main(String[] args) {
    runServer(args, new WhosThereServer());
  }

  public String response(String message) {
    return "Who's there?!";
  }

}
