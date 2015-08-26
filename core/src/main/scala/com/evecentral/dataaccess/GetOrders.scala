package com.evecentral.dataaccess

import akka.actor.Actor
import akka.dispatch.{RequiresMessageQueue, BoundedMessageQueueSemantics}
import com.evecentral.Database
import net.noerd.prequel.{LongFormattable, StringFormattable}
import org.joda.time.{DateTime, Period}
import org.slf4j.LoggerFactory

case class MarketOrder(typeid: Long, orderId: Long, price: Double, bid: Boolean, station: Station, system: SolarSystem, region: Region, range: Int,
                       volremain: Long, volenter: Long, minVolume: Long, expires: Period, reportedAt: DateTime) {
  val weightPrice = price * volenter
}

object MarketOrder {
  implicit def pimpMoToDouble(m: MarketOrder): Double = {
    m.price
  }
}

/**
 * Get a list of orders.
 * TODO: the Long parameters really should be a MarketType, Region etc
 */
case class GetOrdersFor(bid: Option[Boolean], types: Seq[Long], regions: Seq[Long], systems: Seq[Long], hours: Long = 24, minq: Long = 1)

case class OrderList(query: GetOrdersFor, result: Seq[MarketOrder])

class GetOrdersActor extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  private val log = LoggerFactory.getLogger(getClass)

  /**
   * This query does a lot of internal SQL building and not a prepared statement. I'm sorry,
   * but at least everything is typesafe :-)
   */
  private def orderList(filter: GetOrdersFor): Seq[MarketOrder] = {
    val db = Database.coreDb
    val regionLimit = Database.concatQuery("regionid", filter.regions)
    val typeLimit = Database.concatQuery("typeid", filter.types)
    val systems = Database.concatQuery("systemid", filter.systems)
    val hours = "%d hours".format(filter.hours)

    val bid = filter.bid match {
      case Some(b) => b match {
        case true => "bid = 1"
        case _ => "bid = 0"
      }
      case None => "1=1"
    }

    val orders = db.transaction {
      tx =>

        tx.select("SELECT typeid,orderid,price,bid,stationid,systemid,regionid,range,volremain,volenter,minvolume,EXTRACT(EPOCH FROM duration),reportedtime" +
          " FROM current_market WHERE reportedtime >= NOW() - (INTERVAL ?) AND volenter >= ? AND " + bid + " AND (" +
          typeLimit + ") AND (" +
          regionLimit + ") AND ( " +
          systems + ")", StringFormattable(hours), LongFormattable(filter.minq)) {
          row =>
            val typeid = row.nextLong.get
            val orderid = row.nextLong.get
            val price = row.nextDouble.get
            val bid = row.nextBoolean.get
            val stationid = row.nextLong.get
            val systemid = row.nextLong.get

            // Get a mock system or station if required (i.e. not in the database?)
            val region = StaticProvider.regionsMap(row.nextLong.get)
            val system = StaticProvider.systemsMap.getOrElse(systemid, SolarSystem(systemid, "Unknown", 0.0, region, 0))
            val station = StaticProvider.stationsMap.getOrElse(stationid, Station(stationid, "Unknown", "Unknown", system))
            val range = row.nextInt.get
            val volremain = row.nextLong.get
            val volenter = row.nextLong.get
            val minvol = row.nextLong.get
            val duration = new Period(row.nextLong.get * 1000)

            MarketOrder(typeid, orderid, price, bid,
              station,
              system,
              region,
              range,
              volremain, volenter, minvol,
              duration,
              new DateTime(row.nextDate.get)
            )
        }
    }
    log.info("ORDERS: Fetched for " + typeLimit.toString)
    orders
  }

  def receive = {
    case x: GetOrdersFor => {
      sender ! OrderList(x, orderList(x))
    }
  }


}
