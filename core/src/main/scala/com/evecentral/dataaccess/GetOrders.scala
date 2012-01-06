package com.evecentral.dataaccess

import akka.dispatch.Future
import akka.routing._
import akka.actor.Actor
import akka.event.EventHandler
import Actor._
import com.evecentral.{ActorUtil, Database}
import net.noerd.prequel.IntFormattable

case class MarketOrder(typeid: Long, orderId: Long, price: Double, bid: Boolean, station: Station, system: SolarSystem, region: Region, range: Int,
                       volremain: Int,  volenter: Int, minVolume: Int)

case class GetOrdersFor(bid: Boolean, types: Seq[Long], regions: Seq[Long], systems: Seq[Long], hours: Long)


class GetOrdersActor extends Actor with ActorUtil with DefaultActorPool
with BoundedCapacityStrategy
with ActiveFuturesPressureCapacitor
with SmallestMailboxSelector
with BasicNoBackoffFilter {

  def extract[T](t: Option[T]): T = {
    t match {
      case Some(x) => x
    }
  }

  private def orderList(filter: GetOrdersFor): Seq[MarketOrder] = {
    val db = Database.coreDb
    val regionLimit = Database.concatQuery("regionid", filter.regions)
    val typeLimit = Database.concatQuery("typeid", filter.types)
    val bid = filter.bid match {
      case true => 1
      case _ => 0
    }




    db.transaction {
      tx =>

        tx.select("SELECT typeid,orderid,price,bid,stationid,systemid,regionid,range,volremain,volenter,minvolume FROM current_market WHERE bid = ? AND (" +
          typeLimit + ") AND (" +
          regionLimit + ") AND price > 0.15 ", IntFormattable(bid)) {
          row =>
            MarketOrder(extract(row.nextLong), extract(row.nextLong), extract(row.nextDouble), extract(row.nextBoolean),
              StaticProvider.stationsMap(extract(row.nextLong)),
              StaticProvider.systemsMap(extract(row.nextLong)),
              StaticProvider.regionsMap(extract(row.nextLong)), extract(row.nextInt),
            extract(row.nextInt), extract(row.nextInt), extract(row.nextInt));
        }
    }
  }

  def receive = _route

  def lowerBound = 2

  def upperBound = 4

  def rampupRate = 0.1

  def partialFill = true

  def selectionCount = 1

  def instance = actorOf(new Actor {
    def receive = {
      case x: GetOrdersFor => self.reply(orderList(x))
    }
  })
}
