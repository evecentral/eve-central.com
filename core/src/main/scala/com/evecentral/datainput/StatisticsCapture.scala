package com.evecentral.datainput

import akka.actor.{Actor, Props}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import com.evecentral.util.BaseOrderQuery
import akka.pattern.ask

import org.joda.time.{DateTime => JTDateTime}

import com.evecentral.dataaccess.{OrderList, StaticProvider, GetOrdersFor}
import com.evecentral.{RegisterCacheFor, OrderStatistics, Database}

private[this] case class CaptureStatistics()

private[this] case class StoreStatistics(query: GetOrdersFor, result: OrderStatistics)

object StatisticsCapture {
  val allEmpireRegions = -1
  val noRegion = 0
}

class StatisticsCaptureActor extends Actor with BaseOrderQuery {

  /**
   * A static list of systems to always capture statistics for
   */
  val systemsAlwaysCaptureFor = Map("The Forge" -> "Jita",
    "Domain" -> "Amarr",
    "Sinq Laison" -> "Dodixie",
    "Heimatar" -> "Rens")
    .map {
    case (r,s) =>
      (StaticProvider.regionsByName(r).regionid ->
        StaticProvider.systemsByName(s).systemid)
  }.toMap

  private val log = LoggerFactory.getLogger(getClass)
  private val toCaptureSet = scala.collection.mutable.Set[GetOrdersFor]()
  implicit val ec = context.dispatcher

  override def preStart() {
    log.info("Pre-starting statistics capture actor")
    val timeTillNextHour = 60 - new JTDateTime().getMinuteOfHour
    context.system.scheduler.schedule(timeTillNextHour.minutes, 60.minutes,
      self, CaptureStatistics())
  }

  def buildQueries(bid: Boolean, typeid: Int, regionid: Long): List[GetOrdersFor] = {
    val base = List(GetOrdersFor(Some(bid), List(typeid), List(regionid), List(), 1),
      GetOrdersFor(Some(bid), List(typeid), List(), List(), 24),
      GetOrdersFor(Some(bid), List(typeid), StaticProvider.empireRegions.map(_.regionid), List(), 24))

    base ++ (systemsAlwaysCaptureFor.filter { case (r: Long, s) => r == regionid }.map {
      case (r: Long,s: Long) => GetOrdersFor(Some(bid), List(typeid), List(r), List(s), 24)
    })
  }

  def storeStatistics(query: GetOrdersFor, result: OrderStatistics) {
    val region = if (query.regions.size > 1) StatisticsCapture.allEmpireRegions
    else if (query.regions.size == 1) query.regions.head
    else StatisticsCapture.noRegion

    val system = query.systems.headOption match {
      case Some(y) => y
      case None => 0
    }

    val item = query.types(0).toInt

    Database.coreDb.transaction {
      tx =>
        import net.noerd.prequel.SQLFormatterImplicits._
        tx.execute("INSERT INTO trends_type_region (typeid, region, average, median, volume, stddev, buyup, systemid, bid, minimum, maximum) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
          item,
          region, result.avg, result.median, result.volume, result.stdDev, result.fivePercent, system,
          query.bid.get match {
            case true => 1
            case false => 0
          }, result.min, result.max)
    }

    val cached = OrderStatistics.cached(query, result)
    statCache ! RegisterCacheFor(cached)
  }


  def receive = {
    case UploadTriggerEvent(typeid, regionid) =>
      /* Build queries for orders */
      //if (StaticProvider.typesMap.contains(typeid))
      //  toCaptureSet ++= (buildQueries(true, typeid, regionid) ++ buildQueries(false, typeid, regionid))
    case StoreStatistics(query, result) =>
      //storeStatistics(query, result)
    case CaptureStatistics() =>
      log.info("Capturing statistics in a large batch")
      val results = toCaptureSet.toSeq.map(capset => (ordersActor ? capset).mapTo[OrderList])

      log.info(results.size + " results to capture")
      toCaptureSet.clear()

      // Attach an oncomplete to all the actors
      results.map {
        entity =>
          entity onSuccess {
            case OrderList(query, result) => self ! StoreStatistics(query, OrderStatistics(result, query.bid.getOrElse(false)))
          }
      }
  }

}
