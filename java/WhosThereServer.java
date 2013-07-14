
import java.io.*;
import java.net.*;

public class WhosThereServer extends ServerDirectOrRelayed {
  
  public String response(String request) {
    return "Who's there?!";
  }

  public static void main(String[] args) {
    new WhosThereServer().runServer(args);
  }

}
