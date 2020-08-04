import sys

from flask_script import Manager
import app
from flask_twisted import Twisted
from twisted.python import log

if __name__=="__main__":
        app = app.returnapp()
        twisted = Twisted(app)
        log.startLogging(sys.stdout)

        manager = Manager(app)
        manager.run()