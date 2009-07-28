import Pyro.core

mapserver = Pyro.core.getProxyForURI("PYROLOC://localhost:7766/mapserver")

print mapserver.route_comp(30001359, [30001408, 30001409])
