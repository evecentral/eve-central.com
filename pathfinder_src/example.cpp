#include "graph.h"


int
main(int, char *[])
{

  Graph *g = new Graph();
  int initial = 30001359;
  int initial2 = 30001408;

  //  for(int i =0;i<128;i++) {
    g->computeFromSource(initial);
    std::cout << g->countHops(initial2) << std::endl;
    //  }

  

  

  return EXIT_SUCCESS;
}
