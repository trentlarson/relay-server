
import java.io.*;
import java.net.*;

public class TrackClientsMessagesServer extends ServerDirectOrRelayed {
  
  private int clientCount = 0;

  protected ClientResponder getClientResponder() {
    clientCount++;
    return new ClientResponder() {
      int clientNum = clientCount;
      int messageCount = 0;
      public String response(String request) {
        messageCount++;
        return "client " + clientNum + " message " + messageCount + ": " + request;
      }
    };
  }

  public static void main(String[] args) {
    new TrackClientsMessagesServer().runServer(args);
  }

}
