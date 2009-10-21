all:
	swig -c++ -python pathfinder.i 
	g++ -fPIC -ftree-vectorize -Iboost/boost -I/usr/include/python2.5 -O3 pathfinder_wrap.cxx graph.cpp -shared -o _pathfinder.so
