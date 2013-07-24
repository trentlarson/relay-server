

# This just contains classes.  To run tests, use server-client-test.py




#
# base handler for line-oriented communications
#
class NewlineInputHandler(SocketServer.BaseRequestHandler):
    def response(self, message):
        return "(undefined, yo)"
    def handle(self):
        data = read_line(self.request)
        while (data != ''):
            nextResponse = self.response(data)
            self.request.sendall(nextResponse + "\n")
            data = read_line(self.request)


#
# handler that returns message
#
class EchoHandler(NewlineInputHandler):
    def response(self, message):
        return message

#
# handler that returns thread and message
#
class ThreadNameAndEchoHandler(NewlineInputHandler):
    def response(self, message):
        return "{}: {}".format(threading.current_thread().name, message)

#
# handler that sleeps some seconds, then returns thread and message
#
class SleepTimeHandler(NewlineInputHandler):
    def response(self, message):
        time.sleep(3)
        return message


class ThreadedTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass





################################################################################
#
# Here's where we start the relay-server versions.
#
# (Yes, I want to refactor this so each server is a class with a relay option.)
# 

def hostAndPortTuple(hostAndPort):
    hostPortList = hostAndPort.split(":")
    return (hostPortList[0], int(hostPortList[1]))
    


class ThreadedServerReachingToClient(threading.Thread):
    def __init__(self, ipAndPort, responder):
        super(ThreadedServerReachingToClient, self).__init__()
        self.clientIp, self.clientPort = hostAndPortTuple(ipAndPort)
        self.responder = responder

    def run(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((self.clientIp, self.clientPort))
        try:
            data = read_line(sock)
            while (data != ''):
                nextResponse = self.responder.response(data)
                sock.sendall(nextResponse + "\n")
                data = read_line(sock)
        finally:
            sock.close()
        

class ThreadedServerThroughRelay(threading.Thread):
    def __init__(self, ip, port, responder):
        super(ThreadedServerThroughRelay, self).__init__()
        self.relayIp = ip
        self.relayPort = port
        self.responder = responder
        self.publicHostAndPortTuple = ('localhost', 8888) # just dummy defaults
        self.sock = None # for later, so our main method can shutdown when finished

    def run(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.relayIp, self.relayPort))
        try:
            publicHostAndPort = read_line(self.sock)
            print publicHostAndPort
            self.publicHostAndPortTuple = hostAndPortTuple(publicHostAndPort)
            newClient = read_line(self.sock)
            while (newClient != ''):
                ThreadedServerReachingToClient(newClient, self.responder).start()
                newClient = read_line(self.sock)
        finally:
            self.sock.close()


class ThreadedServerThroughOldRelay(threading.Thread):
    def __init__(self, ip, port, responder):
        super(ThreadedServerThroughOldRelay, self).__init__()
        self.relayIp = ip
        self.relayPort = port
        self.responder = responder
        self.publicHostAndPortTuple = ('localhost', 8888)
        self.sock = None # for later, so our main method can shutdown when finished

    def run(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.relayIp, self.relayPort))
        try:
            publicHostAndPort = read_line(self.sock)
            print publicHostAndPort
            self.publicHostAndPortTuple = hostAndPortTuple(publicHostAndPort)
            message = read_line(self.sock)
            while (message != ''):
                self.sock.sendall(self.responder.response(message) + "\n")
                newClient = read_line(self.sock)
        finally:
            self.sock.close()



def echo(message):
    return message

def threadNameAndEcho(message):
    return "{}: {}".format(threading.current_thread().name, message)

def sleepAndEcho(seconds, message, messageNum):
    time.sleep(seconds)
    return message + " (at {}*{}={} seconds)".format(messageNum, seconds, messageNum*seconds)

#
# handler that returns message
#
class EchoHandlerRelay():
    def response(self, message):
        return echo(message)

#
# handler that returns thread and message
#
class ThreadNameAndEchoHandlerRelay():
    def response(self, message):
        return threadNameAndEcho(message)

#
# handler that sleeps 5 seconds, then returns thread and message
#
class SleepTimeHandlerRelay():
    def __init__(self, seconds):
        self.seconds = seconds
        self.messageNum = 0
    def response(self, message):
        self.messageNum = self.messageNum + 1
        return sleepAndEcho(self.seconds, message, self.messageNum)
