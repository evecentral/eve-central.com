
import Pyro.core
import pathfinder
import time

class MapServer(Pyro.core.ObjBase):
    def __init__(self):
        Pyro.core.ObjBase.__init__(self)
        self.mapserver = pathfinder.Graph()
        self.routecache = {}
	self.flush_time = time.time()
        

    def route_comp(self, fromt, to_list):

        map_built = False
        return_map = {}
        stime = time.time()
        print "Computing",fromt,"-",to_list
        
        for tot in to_list:
        
            rckey = `max(fromt, tot)` + ":" + `min(fromt, tot)`
            if rckey in self.routecache:
                return_map[tot] = self.routecache[rckey]
            else:
                if map_built is False:
                    self.mapserver.computeFromSource(fromt)
                    map_built = True
                return_map[tot] = self.mapserver.countHops(tot)
                self.routecache[rckey] = return_map[tot]

        print "Done",time.time() - stime
	if time.time() - self.flush_time > 60*60*48:
	   self.routecache = {}
	   print "Flushed all routes"
	   self.flush_time = time.time()
        return return_map



Pyro.core.initServer()
daemon=Pyro.core.Daemon()
uri=daemon.connect(MapServer(),"mapserver")

print "The daemon runs on port:",daemon.port
print "The object's uri is:",uri

daemon.requestLoop()
