%module pathfinder
%{
#include "graph.h"
%}

class Graph {
	public:
	Graph();
	~Graph();
	void computeFromSource(int s);
	int countHops(int d);
};