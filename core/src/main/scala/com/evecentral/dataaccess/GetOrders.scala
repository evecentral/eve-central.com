package com.evecentral.dataaccess

import akka.dispatch.Future
import akka.routing._
import akka.actor.Actor
import akka.event.EventHandler
import Actor._
import com.evecentral.{ECActorPool, ActorUtil, Database}
import org.joda.time.{DateTime, Period}
import org.postgresql.util.PGInterval
import net.noerd.prequel.{StringFormattable, IntFormattable}

case class MarketOrder(typeid: Long, orderId: Long, price: Double, bid: Boolean, station: Station, system: SolarSystem, region: Region, range: Int,
                       volremain: Long,  volenter: Long, minVolume: Long, expires: Period, reportedAt: DateTime) {
  val weightPrice = price * volenter
}

object MarketOrder {
  implicit def pimpMoToDouble(m: MarketOrder) : Double = { m.price }
}

case class GetOrdersFor(bid: Option[Boolean], types: Seq[Long], regions: Seq[Long], systems: Seq[Long], hours: Long = 24, minq: Long = 1)


class GetOrdersActor extends ECActorPool {
  /**
   * This query does a lot of internal SQL building and not a prepared statement. I'm sorry,
   * but at least everything is typesafe :-)
   */
  private def orderList(filter: GetOrdersFor): Seq[MarketOrder] = {
    val db = Database.coreDb
    val regionLimit = Database.concatQuery("regionid", filter.regions)
    val typeLimit = Database.concatQuery("typeid", filter.types)
    val systems = Database.concatQuery("systemid", filter.systems)
    val hours = filter.hours + " hours"

    val bid = filter.bid match {
      case Some(b) => b match {
        case true => "bid = 1"
        case _ => "bid = 0"
      }
      case None => "1=1"
    }
    
    db.transaction {
      tx =>

        tx.select("SELECT typeid,orderid,price,bid,stationid,systemid,regionid,range,volremain,volenter,minvolume,duration,reportedtime" +
          " FROM current_market WHERE reportedtime >= NOW() - (INTERVAL ?) AND " + bid + " AND (" +
          typeLimit + ") AND (" +
          regionLimit + ") AND ( " +
          systems + ") AND price > 0.15 ", StringFormattable(hours)) {
          row =>
            MarketOrder(row.nextLong.get, row.nextLong.get, row.nextDouble.get, row.nextBoolean get,
              StaticProvider.stationsMap(row.nextLong get),
              StaticProvider.systemsMap(row.nextLong get),
              StaticProvider.regionsMap(row.nextLong get), row.nextInt get,
              row.nextLong get, row.nextLong get, row.nextLong get,
              new Period(row.nextObject.get.asInstanceOf[PGInterval].getSeconds.toLong),
              new DateTime(row.nextDate.get)
            );
        }
    }
  }

  def instance = actorOf(new Actor {
    def receive = {
      case x: GetOrdersFor => self.channel ! orderList(x)
    }
  })
}
