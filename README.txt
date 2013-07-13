
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




Still to do:
- ensure different IPs work
- test scripts
- approach: route incoming client back to original ServerSocket; or
create new ServerSocket for server to reconnect... allow multiple
clients with different connections?
- bash scripts
- may want to make that abstract main work
-- help for relay
- - error level param
- - add host parameter to Relay
- remove System.outs
- wrap exceptions in others
- remove ???
- close threads at the end?
- all connections close when remote client closes
- if it shuts down, undefined (I do null)

start 12... pause at 12:30... for ?
done at 3:17?
no, 4:17
figured out port conflict at 5:04
finished 6:24

Assumptions:

I can treat any incoming server IP as if there is only one server on that IP.  (In other words, any incoming server connection that has the same IP as a previous one can replace that previous one.)

???? I can use a server/socket library (eg. for Java, for node).

I can ignore problems of too much data without a newline.

I assume that these two sentences cover the same requirement:
"it should output on stdout what its new public address is"
"this requires your relay server interface to notify relayed clients of their public address"
(In other words, it's possible that your second sentence means there is some other notification method that is required, but I think not.)

I don't have to provide all the help info I typically would spend time on (eg. if they provide a port of "xyz").

I can ignore problems of too many servers, too many clients,
connections being open for too long, or disconnects (since you said we
won't "initiate" any connections)... essentially, all those pecky
high-traffic concerns.



Later:
- java accepts on a port, then binds to a different port... so we can
use that new socket (otherwise we'd for them to reconnect)
