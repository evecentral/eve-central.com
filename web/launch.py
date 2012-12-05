#!/usr/bin/python
#    EVE-Central.com Codebase
#    Copyright (C) 2006-2012 StackFoundry LLC and Yann Ramin
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.


import sys
#import cmemcache as memcache
import memcache
#sys.modules['memcache'] = memcache # Hack
sys.path.append('../lib')

import cherrypy

from tradetool import TradeTool
from evec import Home
from json_tools import JsonTools


import os

if __name__=="__main__":
    cherrypy.config.update("site.config")

    if os.path.isfile('site.config.local'):
        cherrypy.config.update("site.config.local")

    cherrypy.config.update({'server.socket_port': int(sys.argv[1])})

    cherrypy.tree.mount(Home(), '/home')
    cherrypy.tree.mount(TradeTool(), '/tradetool')
    cherrypy.tree.mount(JsonTools(), '/json_tools')



    conf = {'/' : { 'tools.staticdir.dir' : ".",
                    'tools.staticdir.on' : True },
            }

    cherrypy.tree.mount(root = Home(), config = conf)

    cherrypy.engine.start()
    cherrypy.server.start()

    #from guppy import hpy; h=hpy()
    #from IPython.Shell import IPShellEmbed

    #ipshell = IPShellEmbed()

    #ipshell() # this call anywhere in your program will start IPython
    #cherrypy.engine.stop()
    #sys.exit(0)
