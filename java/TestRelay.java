
import java.io.*;
import java.net.*;

public class TestRelay {

  public static void main(String[] args) {
    System.out.println("Doesn't work yet!");
    new RelayThread().start();
    try {
      Thread.currentThread().sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    EchoServerThread serverThread1 = new EchoServerThread(8081);
    serverThread1.start();
    try {
      Thread.currentThread().sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    EchoServerThread serverThread2 = new EchoServerThread(8081);
    serverThread2.start();
    try {
      Thread.currentThread().sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    EchoServerThread serverThread3 = new EchoServerThread(8081);
    serverThread3.start();
    try {
      Thread.currentThread().sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    
    Socket clientSocket = null;
    BufferedReader incoming = null;
    PrintWriter outgoing = null;
    try {
      //System.out.println("going to open a socket to " + serverThread1.server.relayHost + ":" + serverThread1.server.relayPort);
      //clientSocket = new Socket(serverThread1.server.relayHost, serverThread1.server.relayPort);
      outgoing = new PrintWriter(clientSocket.getOutputStream(), true);
      incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      outgoing.println("asdf");
      String message = incoming.readLine();
      System.out.println("got message: " + message); // tell the relayed host & port
    } catch (IOException e) {
      try { incoming.close(); } catch (Exception e2) {}
      try { outgoing.close(); } catch (Exception e2) {}
      try { clientSocket.close(); } catch (Exception e2) {}
      throw new RuntimeException(e);
    }
      
  }

  public static class RelayThread extends Thread {
    public void run() {
      Relay.main(new String[]{});
    }
  }

  public static class EchoServerThread extends Thread {
    private int port;
    protected ServerDirectOrRelayed server;
    public EchoServerThread(int _port) {
      port = _port;
      server = new EchoServer();
    }
    public void run() {
      server.runServer(new String[]{"" + port});
    }
  }

}
