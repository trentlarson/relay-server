
# from the end of http://docs.python.org/2/library/socketserver.html

import socket
import sys
import threading
import time
import SocketServer


# Read from the sockets, looking for "\n" (one character at a time).
def read_line(s):
    ret = ''
    while True:
        c = s.recv(1)
        if c == '\n' or c == '':
            break
        else:
            ret += c
    return ret

import os
execfile('servers.py')



class ThreadedNewlineClient(threading.Thread):
    def __init__(self, ip, port, message):
        super(ThreadedNewlineClient, self).__init__()
        self.ip = ip
        self.port = port
        self.messages = message.split("\n")

    def run(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.ip, self.port))
        try:
            for message in self.messages:
                sock.sendall(message + "\n")
                nextResponse = read_line(sock)
                print "Received: {}".format(nextResponse)
        finally:
            sock.close()


def runTestClients4(hostPorts):
    ThreadedNewlineClient(hostPorts[0][0], hostPorts[0][1], "1\n2\n3\n4\n5").start()
    time.sleep(3)


    ThreadedNewlineClient(hostPorts[1][0], hostPorts[1][1],
                          "Data, pause...\n... more, pause...\n... done.").start()


    ThreadedNewlineClient(hostPorts[2][0], hostPorts[2][1],
                          "This should \n happen before \n the other.").start()
    

    time.sleep(2) # let's wait until some of the sleep stuff is printed
    ThreadedNewlineClient(hostPorts[3][0], hostPorts[3][1],
                          "This should \n be interleaved.").start()
        





def runTestTwo10SecondClients(hostPorts):
    print "This should only take 10 seconds."
    ThreadedNewlineClient(hostPorts[0][0], hostPorts[0][1], "3\n3\n3\n1").start()
    ThreadedNewlineClient(hostPorts[1][0], hostPorts[1][1], "1\n1\n4\n4").start()






def runTestClientsWithState(hostPorts):
    ThreadedNewlineClient(hostPorts[0][0], hostPorts[0][1], "1-1\n1-2\n1-3\n1-4").start()
    ThreadedNewlineClient(hostPorts[1][0], hostPorts[1][1], "... some\n... random\n... inside").start()
    ThreadedNewlineClient(hostPorts[0][0], hostPorts[0][1], "2-1\n2-2\n2-3\n2-4").start()





if __name__ == "__main__":


    HIT_EXISTING_SERVERS = False;
    RELAY_HOST = None
    RELAY_PORT = None
    i = 1;
    while (i < len(sys.argv)):
        if (sys.argv[i] == "-e"):
            HIT_EXISTING_SERVERS = True
            i = i + 1;
        elif (sys.argv[i] == "-r"):
            RELAY_HOST = sys.argv[i+1]
            RELAY_PORT = int(sys.argv[i+2])
            i = i + 3
        elif (sys.argv[i] == "-?"):
            print "Usage: "
            print "   -e             hit some existing servers, configured in the script"
            print "   -r HOST PORT   connect to relay server at HOST:PORT"
            exit(0)
        else:
            print "Got unknown option:", sys.argv[i]
            i = i + 1

    if (HIT_EXISTING_SERVERS):

        # BEWARE: an empty line will basically kill the server.
        # (The fix is to do something different in the socket reads, instead of (data != '') everywhere.)

        # rely on servers that are running
        hostPort1 = ('localhost', 8082)
        hostPort2 = ('localhost', 8083)
        hostPort3 = ('localhost', 8099)
        hostPort4 = ('localhost', 8098)

        #runTestClients4([hostPort1, hostPort2, hostPort3, hostPort4])
        runTestTwo10SecondClients([hostPort1, hostPort1])
        #runTestClientsWithState([hostPort1, hostPort2])


    else:
        if (RELAY_HOST != None):
            # We'll start our own servers
        
            echoServer = EchoHandlerRelay()
            sleepHandler2 = SleepTimeHandlerRelay(2)
            relayed1 = ThreadedServerThroughRelay(RELAY_HOST, RELAY_PORT, sleepHandler2)
            relayed1.start()
            relayed2 = ThreadedServerThroughRelay(RELAY_HOST, RELAY_PORT, sleepHandler2)
            relayed2.start()
            relayed3 = ThreadedServerThroughRelay(RELAY_HOST, RELAY_PORT, echoServer)
            relayed3.start()
            relayed4 = ThreadedServerThroughRelay(RELAY_HOST, RELAY_PORT, echoServer)
            relayed4.start()
            time.sleep(1) # gotta give them time to initialize, eg. with their public address



            runTestClients4(map(lambda relay: relay.publicHostAndPortTuple,
                                [relayed1, relayed2, relayed3, relayed4]))


            time.sleep(5)

            relayed1.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
            relayed2.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
            relayed3.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
            relayed4.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it




        else:
            print "Starting our own servers for this test."

            # Port 0 means to select an arbitrary unused port
            HOST, PORT = "localhost", 0

            server = ThreadedTCPServer((HOST, PORT), EchoHandler)
            #server2 = ThreadedTCPServer((HOST, PORT), ThreadNameAndEchoHandler)
            server2 = ThreadedTCPServer((HOST, PORT), SleepTimeHandler)
            ip, port = server.server_address
            ip2, port2 = server2.server_address

            # Start a thread with the server -- that thread will then start one
            # more thread for each request
            server_thread = threading.Thread(target=server.serve_forever)
            # Exit the server thread when the main thread terminates
            server_thread.daemon = True
            server_thread.start()
            print "Server loop running in thread: {} port: {}".format(server_thread.name, port)

            # Start a thread with the server -- that thread will then start one
            # more thread for each request
            server2_thread = threading.Thread(target=server2.serve_forever)
            # Exit the server thread when the main thread terminates
            server2_thread.daemon = True
            server2_thread.start()
            print "Server 2 loop running in thread: {} port: {}".format(server2_thread.name, port2)
            #print "Server 2 loop running in thread:", server2_thread.name
        
            ThreadedNewlineClient(ip2, port2, "Hello World 0\n12345\n67890\n123").start()
            ThreadedNewlineClient(ip, port, "Hello World 0\n12345\n67890\n123").start()
            #client.daemon = True # Why doesn't this keep the program running until it's finished?
            time.sleep(20);

            #server.shutdown()
            #server2.shutdown()

