
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

I can use a server/socket library (eg. for Java).

The request & response terminators can be CR or LF or CR-LF.

The protocol is always one request from the client which results in
one response from the server.  So if a server isn't finished with a
request yet, subsequent requests by the same client will queue; also,
multiline requests/responses will depend on an acknowledgement of each
line from the other side.

I don't have to do connection management, eg. signals back-and-forth
for when the relay shuts down or when the servers shut down.  I can do
my own thing if a server shuts down (like return "null" or error or no
responses to clients).

I can ignore problems of too much data on one newline, too many
servers, too many clients, connections being open for too long, or
disconnects... essentially, all those pesky high-traffic concerns.






________________________________________________________________________________
EXPLANATION OF RELAY SERVER

This relay server is your public intermediary to the rest of the
world, giving access to your own server that may be sitting behind a
firewall.

To run it, make sure Java (1.6+) is installed.  Unpack the relay
server:

tar -xvf relay.tgz
cd relay



You can run it in basic mode using "localhost" and port 8080 by default:

relay &



But it's best to customize the address you're using, and probably the
port.  Let's say your server is running at 1234.amazonaws.com on port
8888:

relay -h 1234.amazonaws.com 8888 &



If you want to see all connections made to the server for debugging,
use the -v option (or -vv for even more info):

relay -h 1234.amazonaws.com -v 8888 &



Now, the relay server will accept connections from clients anywhere
and forward them along to your server... once you make a few changes
to your server.





CHANGES TO YOUR SERVER

Now you have to modify your own server: it needs to register itself
and retrieve it's own public address.  Here's how: your server
currently listens for any incoming connections, so you'll modify it to
do this instead:

1) Make a single connection to your relay server,

2) accept one HOST:PORT line of input for your new public address,

3) then wait for more HOST:PORT addresses to tell you where to connect
  to each client as they make connection requests.

Let's go through the steps.  First, after you connect to the server,
it will send back the host and port information, like this:

1234.amazonaws.com:8889

That address is for you to share with the world.  Keep that socket
open and listen on it; if someone comes in and connects to that
address, you will get another HOST:PORT combination that tells you
where to connect to that client.  For example, the next line might
come to you as:

1234.amazonaws.com:8895

That means that a client has made a connection to the relay, so you
should connect to that address and receive the requests and give
responses through it.



Let's give a quick Java code example to demonstrate how you would need
to modify an existing server to work with this relay.  Assume you've
got some class or method that runs your inner loops for one client,
like so:

  public class ResponseHandler implements Runnable {
    ...
    public void run() {
      ...
        incoming = new BufferedReader(new InputStreamReader(clientConn.getInputStream()));
        outgoing = new PrintWriter(clientConn.getOutputStream(), true);

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
probably something like this, which reads from a ServerSocket and then
calls the sample RequestWaiter for each client connection you get:

        serverSocket = new ServerSocket(port);
        while(true) { // loop forever, spawning a thread for each new client
          clientSocket = serverSocket.accept();
          new Thread(new ResponseHandler(clientSocket)).start(); // talk to client
        }


Here's the change: instead of listening for all your clients as a
server, make one connection to our relay, get the public HOST:PORT
which you can advertize, and then listen for other addresses where you
can connect to each client as they contact your public address.

        clientAddressSocket = new Socket("1234.amazonaws.com", 8888); // for the relay server

        // grab my public address from the relay
        incoming = new BufferedReader(new InputStreamReader(clientAddressSocket.getInputStream()));
        String publicHostAndPort = incoming.readLine();
        System.out.println(publicHostAndPort); // this is the HOST:PORT to give clients

        // now listen on that socket for new connections to make for new clients
        String messageIn = incoming.readLine();
        while (messageIn != null) { // loop until closed, spawning a thread for each new client
          // parse out the host and port where to connect for this new client
          String clientHost = messageIn.substring(0, messageIn.indexOf(":"));
          int clientPort = Integer.valueOf(messageIn.substring(messageIn.indexOf(":") + 1));
          clientSocket = new Socket(clientHost, clientPort);
          new Thread(new ResponseHandler(clientSocket)).start(); // talk to client

          // now wait for the next client HOST:PORT
          messageIn = incoming.readLine();
        }


Now give everyone the HOST:PORT that you got back (in "publicHostAndPort") and you're ready for the world.



________________________________________________________________________________
Commentary

You asked for some feedback, but the problem is actually pretty well
specified.  In my case, I focused on the routing and I aimed for a
very simple (simplistic!) server modification.  You could conceivably
warn people, but I think you want to find people who will do it right
the first time (or maybe understand their mistakes with little
explanation :-).

I thought about supplying test cases, but there's the same issue: you
want people who will come up with good test cases or grok their
mistakes quickly.

Of course, I'm sure you have test servers to help with your grading.
One thing that is a pain, I'm sure, is to modify those servers to work
with people's relays; I thought it was clunky to architect and run two
different versions of my servers.  So I created the relay-adapter.py
to sit in between the servers and the client or the relay: when run as
a simple proxy, it passes client requests to a server; then it can be
modified to work with the relay and route things to the servers.
Anyway, I thought it worthwhile to share that part of my testing.


