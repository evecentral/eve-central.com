/** EVE-Central.com Codebase
*   Copyright (C) 2006-2009 StackFoundry LLC and Yann Ramin
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU Affero General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/



#include <boost/config.hpp>
#include <iostream>
#include <fstream>

#include <string>
#include <boost/tokenizer.hpp>
#include <boost/tuple/tuple.hpp>
#include <map>
#include <ext/hash_map>
#include <queue>

#include <vector>
#include <algorithm>
#include <list>



class Vertex {
public:
  Vertex(int _id) {
    id = _id;
  }
  Vertex() {
    id = -1; 
  }
  int id;
  inline bool operator<(const Vertex &rhs) const {
    return id < rhs.id;
  }
  inline bool operator==(const Vertex &rhs) const {
    return id == rhs.id;
  }
};



class Distance {
public:
  Vertex v;
  int distance;
  Distance(Vertex _v) {
    v = _v;
    distance = 0x7FFFFFFF;
  }
  Distance() { }
  inline bool operator<(const Distance &rhs) const {
    return distance > rhs.distance;
  }
};


class DistancePtr {
 public:
  DistancePtr() { d = NULL; }
  DistancePtr(Distance *d) { this->d = d; }
  inline int getDistance() { return d->distance; }
  inline void setDistance(int dis) { d->distance = dis; }
  inline Vertex& getVertex() { return d->v; }
  inline void setVertex(const Vertex &v) { d->v = v; }

  inline bool operator<(const DistancePtr &rhs) const {
    return d->distance > rhs.d->distance;
  }
 private:
  Distance *d;

};

class Edge {
public:
  Edge(int _v1, int _v2, int _w) {
    v1 = _v1; v2 = _v2; w = _w; }
  Edge() {
    v1 = -1;
    v2 = -1;
    w = -1;
  }
  int v1;
  int v2;
  int w;

};




class Graph {


public:
  Graph();
  ~Graph();
  void computeFromSource(int s);
  int countHops(int d);

private:
  void resetGraph(int );

  std::vector<Vertex> verticies;
  std::map<Vertex, std::vector<Edge> > edgeVertex;
  std::vector<Edge> edges;
  std::vector<Distance*> d_p;
  std::vector<DistancePtr> d;
  std::map<Vertex, std::vector<DistancePtr> > distanceMap;
  
  std::map<Vertex, Vertex> previous;
  int num_nodes;


};
