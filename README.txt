
Problem: write a generic TCP relay.

We have two processes that can reach a relay box via TCP, but can not reach
each other (because of NAT or firewall issues). You are asked to:
 1. Implement a relay server. This server will listen for connections on behalf
    of another process, alert the other process when an incoming connection has
    been made, and forward all incoming/outgoing traffic from the incoming
    connection to the process and vice versa, regardless of application
    protocol (maybe HTTP/SSL/SMTP/POP3/who knows). The relay server will be
    contacted by two types of applications - processes needing a relay, and
    other processes wanting to talk to the process needing a relay.
 2. Describe in clear terms to another programmer how they would enable a
    process they have, behind some firewall, to be able to use your relay
    server. Remember that every process in this scenario can contact the relay
    server, but the relay server can't *initiate* communication with any other
    process (due to firewalls, etc).
 3. To help yourself debug and for us to evaluate your work, write a small
    application that uses the relay server (maybe an echo server). Once your
    server has established a successfully relayed port, it should output on
    stdout what its new public address is, and we should be able to contact
    it, through the relay, with telnet or netcat or something. Note that this
    requires your relay server interface to notify relayed clients of their
    public address.

Use whatever language and tools you feel most comfortable with.

In order to test this, make sure that your relay process can be started via the
invocation:
  ./relay port
where port is how your echoserver will contact the relay.
You can (and probably should) accept other arguments, but choose reasonable
defaults.

Your echo server process should be started with the invocation:
  ./echoserver relayhost relayport
where relayhost is the host the relay is running on, and port is the port you
provide to the relay process. Upon receiving a (host, port) pair from the
relay, the echo server process should notify us what its relayed hostname/port
pair is.

Example session:
$ ./relay 8080 &
$ ./echoserver localhost 8080 &
localhost:8081
$ telnet localhost 8081
Hello, world
Hello, world


________________________________________________________________________________
Assumptions

Here are my assumptions.  I'm going ahead with this rather than asking and waiting for answers, so gently point out if there's anything I'm thinking that is just plain missing the point.

I can use a server/socket library (eg. for Java).

These two sentences cover the same requirement:
"it should output on stdout what its new public address is"
"this requires your relay server interface to notify relayed clients of their public address"
(In other words, it's possible that your second sentence means there is some other notification method that is required, but I'm assuming not.)

I don't have to provide great usability info (eg. if they provide a port of "xyz"), or build instructions.  (It'll be in Java; I'll provide the class files.)

I don't have to write signals back-and-forth for when, say, the relay shuts down or when the servers shut down.  I can do my own thing if a server shuts down (like return "null" or blank responses to clients).

I can ignore problems of too much data on one newline, too many servers, too many clients, connections being open for too long, or disconnects (since you said we won't "initiate" any connections)... essentially, all those pesky high-traffic concerns.



________________________________________________________________________________
Explanation of Relay Server by Trent
  (... and let's all assume there's one running publicly at RST.com on port 8080)

Is your service hidden... trapped behind a firewall, or somehow
inaccessible to the world at large?  If so, use our server at RST.com
to make your service available to everyone.  It's straightforward:
your server currently listens for any incoming connections, so you'll
modify it to do this instead: 

- Make a single connection to our server at RST.com:8080,

- accept one HOST:PORT line of input for your new public address,

- then sit and respond to that one client, and we'll route everyone to you.

We'll manage the ports and we'll forward each of your users' requests.
All you have to do is advertise to everyone the HOST:PORT that we sent
back when you made the connection; give it to all your users, and
they'll be able to connect to your application from anywhere.


Let's give a quick Java code example to show how.  Assume you've got
some class or method that runs your inner loops for one client, like
so:

  public class RequestWaiter implements Runnable {
    ...
    public void run() {
      ...
        String messageIn = incoming.readLine();
        while (messageIn != null) { // loop until the stream is closed
          outgoing.println(response(messageIn));
          messageIn = incoming.readLine();
        }
      ...
    }
    ...
  }


You've also got an outer loop that accepts client connections;
probably something like this, which calls our sample "RequestWaiter"
for each client connection you get:

        serverSocket = new ServerSocket(port);
        while(true) { // loop forever, spawning a thread for each new client
          clientSocket = serverSocket.accept();
          new Thread(new RequestWaiter(clientSocket)).start();
        }


Here's the change: instead of listening for all your clients, use
something like this to make one connection to our relay, get the new
host and port, and then treat that like your only client:

        clientSocket = new Socket("RST.com", 8080); // this is our service
        incoming = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        publicHostAndPort = incoming.readLine(); // publish this to the world: it goes to you
        new RequestWaiter(clientSocket).run();

You're ready for the world.
