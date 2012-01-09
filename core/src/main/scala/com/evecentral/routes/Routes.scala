package com.evecentral.routes

import com.evecentral.Database
import akka.actor.Actor
import edu.uci.ics.jung.graph.{DirectedSparseGraph, UndirectedSparseGraph}
import com.evecentral.dataaccess.{StaticProvider, SolarSystem}
import edu.uci.ics.jung.graph.util.EdgeType
import org.slf4j.{Logger,  LoggerFactory}
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import scalaj.collection.Imports._


/**
 * A jump from one solar system to another.
 */
case class Jump(from: SolarSystem, to: SolarSystem, secChange : Boolean = false)

/**
 * Helper for Jung Dijkstra's
 */
private class JumpExtractor(secWeight : Int = 1000) extends org.apache.commons.collections15.Transformer[Jump,  java.lang.Number] {
  def transform(j: Jump) : Number = {
    j.secChange match {
      case true => secWeight
      case false => 1
    }
  }
}

/**
 * Ask for a route between two points. Message reply is a Seq[Jump]
 */
case class RouteBetween(from: SolarSystem, to: SolarSystem)

/**
 * Ask for the integer distance between two points.
 */
case class DistanceBetween(from: SolarSystem, to: SolarSystem)

/**
 * Actor which finds distances and paths between systems
 */
class RouteFinderActor extends Actor {

  private val logger = LoggerFactory.getLogger(this.getClass.getName)

  override def preStart() {
    super.preStart()
    allJumps
    dsp = new DijkstraShortestPath[SolarSystem, Jump](graph, new JumpExtractor(), true)
  }

  private[routes] def routeDistance(from: SolarSystem, to: SolarSystem) : Int = {
    route(from, to).length
  }

  private[routes] def route(from: SolarSystem, to: SolarSystem) : Seq[Jump] = {
    dsp.getPath(from, to) asScala
  }

  def receive = {
    case DistanceBetween(f,t) => self.reply(routeDistance(f,t))
    case RouteBetween(f,t) => self.reply(route(f,t))
  }
  
  private var graph = new DirectedSparseGraph[SolarSystem, Jump]()
  private var dsp : DijkstraShortestPath[SolarSystem, Jump] = null

  private def allJumps = {

    graph = new DirectedSparseGraph[SolarSystem, Jump]()
    logger.info("Starting to build route graph")
    
    StaticProvider.systemsMap.foreach( x => graph.addVertex(x._2) )

    logger.info("Loaded verticies")
    
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT fromsystem,tosystem FROM jumps") {
          row =>
            val from = row.nextInt match {
              case Some(x) => StaticProvider.systemsMap(x)
            }
            val to = row.nextInt match {
              case Some(x) => StaticProvider.systemsMap(x)
            }
            val jump = Jump(from, to, (from.security < 0.5) || (to.security < 0.5))
            val edges = new edu.uci.ics.jung.graph.util.Pair[SolarSystem](from,to)
            graph.addEdge(jump, edges, EdgeType.DIRECTED)

        }
        
    }
    logger.info("Route graph built")
  }

}
