
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


if __name__ == "__main__":

    USING_RELAY = (len(sys.argv) > 1)
    if USING_RELAY:


        # BEWARE: an empty line will basically kill the server.
        # (The fix is to do something different in the socket reads, instead of (data != '') everywhere.)

        ''' # for your own servers
        host1, port1 = ('localhost', 8083)
        host2, port2 = ('localhost', 8084)
        host3, port3 = ('localhost', 8084)
        host4, port4 = ('localhost', 8084)
        '''

        '''
        '''
        # (Note that 'shutdown' should be called on these down below.)
        echoServer = EchoHandlerRelay()
        sleepHandler2 = SleepTimeHandlerRelay(2)
        relayed1 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), sleepHandler2)
        relayed1.start()
        relayed2 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), sleepHandler2)
        relayed2.start()
        relayed3 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), echoServer)
        relayed3.start()
        relayed4 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), echoServer)
        relayed4.start()
        time.sleep(1) # gotta give them time to initialize, eg. with their public address
        host1, port1 = relayed1.publicHostAndPortTuple
        host2, port2 = relayed2.publicHostAndPortTuple
        host3, port3 = relayed3.publicHostAndPortTuple
        host4, port4 = relayed4.publicHostAndPortTuple



        time.sleep(1) # let it start up
        ThreadedNewlineClient(host1, port1, "1\n2\n3\n4\n5").start()
        time.sleep(3)


        time.sleep(1) # let it start up
        ThreadedNewlineClient(host2, port2, "Data, pause...\n... more, pause...\n... done.").start()


        time.sleep(1) # let it start up
        ThreadedNewlineClient(host3, port3, "This should \n happen before \n the other.").start()


        time.sleep(1) # let it start up
        time.sleep(2) # let's wait until some of the sleep stuff is printed
        ThreadedNewlineClient(host4, port4, "This should \n be interleaved.").start()
        







        time.sleep(5)

        relayed1.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
        relayed2.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
        relayed3.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
        relayed4.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it




    else:
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
        time.sleep(30);

        #server.shutdown()
        #server2.shutdown()

