package com.evecentral.routes

import com.evecentral.Database
import akka.actor.Actor

class RouteFinderActor extends Actor {

  override def preStart() {
    super.preStart()
    allJumps
  }

  def receive = {
    case _ => Unit
  }



  private def allJumps = {
    var m = List[(Int, Int)]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT fromsystem,tosystem FROM jumps") {
          row =>
            val from = row.nextInt match {
              case Some(x) => x
            }
            val to = row.nextInt match {
              case Some(x) => x
            }
            m = m ++ List((from, to))
        }
    }
    m
  }

}