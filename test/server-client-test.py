
# from the end of http://docs.python.org/2/library/socketserver.html

import socket
import sys
import threading
import time
import SocketServer


# This is our line-oriented buffer receive method (one character at a time)
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

        relayed = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), SleepTimeHandlerRelay(2))
        relayed2 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), EchoHandlerRelay())
        relayed3 = ThreadedServerThroughRelay(sys.argv[1], int(sys.argv[2]), EchoHandlerRelay())


        '''
        relayed = ThreadedServerThroughOldRelay(sys.argv[1], int(sys.argv[2]), SleepTimeHandlerRelay(2))
        relayed2 = ThreadedServerThroughOldRelay(sys.argv[1], int(sys.argv[2]), EchoHandlerRelay())
        relayed3 = ThreadedServerThroughOldRelay(sys.argv[1], int(sys.argv[2]), EchoHandlerRelay())
        '''

        relayed.start()
        
        time.sleep(1) # let it start up
        host, port = relayed.publicHostAndPortTuple
        ThreadedNewlineClient(host, port, "Data, pause...\n... more, pause...\ndone.").start()
        


        relayed2.start()
        
        time.sleep(1) # let it start up
        host, port = relayed2.publicHostAndPortTuple
        ThreadedNewlineClient(host, port, "This should \n happen before \n the other.").start()
        



        relayed3.start()
        
        time.sleep(1) # let it start up
        host, port = relayed3.publicHostAndPortTuple
        time.sleep(2) # let's wait until some of the sleep stuff is printed
        ThreadedNewlineClient(host, port, "This should \n be interleaved.").start()
        




        time.sleep(5)
        relayed.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
        relayed2.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it
        relayed3.sock.shutdown(socket.SHUT_RDWR) # .close() doesn't do it





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

