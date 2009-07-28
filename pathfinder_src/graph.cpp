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
#include <queue>

#include <vector>
#include <algorithm>
#include <list>
#include "graph.h"

using namespace boost;



void Graph::resetGraph(int sourceId) {

    d.clear();
    d.reserve(5000);
    previous.clear();

    std::vector<Distance*>::iterator i = d_p.begin();
    for (;i != d_p.end(); ++i) {
        Distance *ptr = *i;
        if (ptr->v.id == sourceId) {
            ptr->distance = 0;
        } else {
            ptr->distance = 0x7FFFFFFF;
        }

        d.push_back(DistancePtr(ptr));
    }

    make_heap(d.begin(), d.end());


}

void Graph::computeFromSource(int sourceId) {
    resetGraph(sourceId);

    while(!d.empty()) {

        DistancePtr dis = *(d.begin());

        //    std::cout << "Visiting " << dis.getVertex().id << std::endl;

        pop_heap(d.begin(), d.end());
        d.pop_back();

        std::vector<Edge>::iterator ei = edgeVertex[dis.getVertex()].begin();
        std::vector<Edge>::iterator eie = edgeVertex[dis.getVertex()].end();


        std::vector<DistancePtr> &distances_v = distanceMap[dis.getVertex()];
        int foo_size = distances_v.size();

        for (; ei != eie; ++ei) {
            Edge &e = *ei;


            for (int foo = 0; foo < foo_size; ++foo) {
                DistancePtr disv = distances_v[foo];
                //std::cout << "Map entry " << disv.getVertex().id  << std::endl;
                if ( disv.getVertex().id == e.v2) {
                    //	    std::cout << "MATCH" << std::endl;
                    if (dis.getDistance() + e.w < disv.getDistance()) {

                        disv.setDistance(dis.getDistance() + e.w);

                        previous[disv.getVertex()] = dis.getVertex();
                        make_heap(d.begin(), d.end());
                    }


                    break;
                }
            }


        }

    }


}

int Graph::countHops(int dest) {
    int count = 0;

    Distance myD = previous[Vertex(dest)];
    if (myD.v.id == -1) { return -2; }
    while(1) {

        myD = previous[myD.v];
        count++;
        if (myD.v.id == -1) { break; }



    }

    return count;
}

Graph::~Graph() {
    std::vector<Distance*>::iterator i = d_p.begin();
    for (;i != d_p.end(); ++i) {
        Distance *ptr = *i;
        delete ptr;
    }



}

Graph::Graph() {
    std::ifstream vertexfile("vertex.csv");
    num_nodes =0;
    int last_sys;

    previous.clear();

    edges.reserve(10000);
    //    d.reserve(10000);


    for(std::string line; std::getline(vertexfile, line);) {
        int vr;
        sscanf(line.c_str(), "%d", &vr);
        num_nodes++;
        verticies.push_back(Vertex(vr));


        previous[Vertex(vr)] = Vertex(-1);


        Distance *findDist = new Distance(Vertex(vr));

        d_p.push_back(findDist);



    }









    std::ifstream edgefile("edges_weight.csv");
    for(std::string line; std::getline(edgefile, line);) {

        char_delimiters_separator < char >sep(false, "", " ");
        tokenizer <> line_toks(line, sep);
        tokenizer <>::iterator i = line_toks.begin();

        std::string _sys1 = *i++;
        std::string _sys2 = *i++;
        std::string _we = *i++;

        // yeah this sucks
        int sys1;
        sscanf(_sys1.c_str(), "%d", &sys1);

        int sys2;
        sscanf(_sys2.c_str(), "%d", &sys2);

        int we;
        sscanf(_we.c_str(), "%d", &we);

        edges.push_back(Edge(sys1, sys2, we));
        edgeVertex[Vertex(sys1)].push_back(Edge(sys1, sys2, we));



    }


    std::vector<Edge>::iterator edgei = edges.begin();
    for (;edgei != edges.end(); ++edgei) {
        Edge e = * edgei;
        std::vector<Distance*>::iterator disi = d_p.begin();
        for (;disi != d_p.end(); ++disi) {
            Distance *pt = *disi;
            if (pt->v.id == e.v2) {
                //std::cout << "Distance [" << e.v1 << "] " << e.v2 << std::endl;
                distanceMap[Vertex(e.v1)].push_back(DistancePtr(pt));
            }

        }
    }



}
