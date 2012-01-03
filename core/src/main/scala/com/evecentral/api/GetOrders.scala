package com.evecentral.api

import akka.dispatch.Future
import akka.routing._
import akka.actor.Actor
import akka.event.EventHandler
import Actor._
import com.evecentral.{ActorUtil, Database}

case class MarketOrder(typeid : Long,  orderId : Long, price : Double, bid : Boolean, station : Long,  system : Long)

case class GetOrdersFor(bid : Boolean,  types : Seq[Long], regions : Seq[Long])


class GetOrdersActor extends Actor with ActorUtil with DefaultActorPool
with BoundedCapacityStrategy
with ActiveFuturesPressureCapacitor
with SmallestMailboxSelector
with BasicNoBackoffFilter {


  private def orderList(filter : GetOrdersFor) = {
    val query = Database.coreDb
    val regionLimit = filter.regions.foldLeft("1=1") {(i,s) => i + " OR regionid = " + s.toString}
    val typeLimit = filter.types.foldLeft("1=1")((i,s) => i + " OR typeid = " + s.toString )
    val bid = filter.bid match { case true => 1 case _ => 0}

    query.select("SELECT typeid,orderid,stationid,systemid,bid,price FROM current_market WHERE bid = ? AND (" +
      typeLimit + ") AND (" +
      regionLimit + ") AND price > 0.15 LIMIT 10", bid) {
      row =>
      MarketOrder(row.getLong("typeid"),
        row.getLong("orderid"),
        row.getLong("price"),
        row.getBoolean("bid"),
        row.getLong("stationid"),
        row.getLong("systemid"))
    }
  }
  def receive = _route
  def lowerBound = 2
  def upperBound = 4
  def rampupRate = 0.1
  def partialFill = true
  def selectionCount = 1
  def instance = actorOf(new Actor {def receive = {
    case x : GetOrdersFor =>  self.reply(orderList(x))
  }})
}
