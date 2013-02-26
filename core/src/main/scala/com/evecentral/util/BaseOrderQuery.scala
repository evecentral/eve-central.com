package com.evecentral.util

import akka.actor.Actor
import com.evecentral.dataaccess._
import com.evecentral.{RegisterCacheFor, GetCacheFor, OrderStatistics, OrderCacheActor}
import com.evecentral.routes.RouteFinderActor
import akka.pattern.ask
import com.evecentral.dataaccess.GetOrdersFor
import com.evecentral.RegisterCacheFor
import com.evecentral.GetCacheFor
import com.evecentral.dataaccess.OrderList
import akka.dispatch.Future
import akka.util.duration._
import akka.util.Timeout

trait BaseOrderQuery {
  this: Actor =>

  import context.dispatcher

  def ordersActor = context.actorFor("/user/" + ActorNames.getorders)

  def statCache = context.actorFor("/user/" + ActorNames.statCache)

  def pathActor = context.actorFor("/user/" + ActorNames.routefinder)

  implicit val timeout: Timeout = 60.seconds

  def fetchOrdersFor(buyq: GetOrdersFor): Future[Seq[MarketOrder]] = {
    (ordersActor ? buyq).map(_.asInstanceOf[OrderList].result)
  }

  def fetchCachedStats(query: GetOrdersFor, highToLow: Boolean): Future[Option[OrderStatistics]] = {
    (statCache ? GetCacheFor(query, highToLow)).map(_.asInstanceOf[Option[OrderStatistics]])
  }

  def storeCachedStats(stats: OrderStatistics, query: GetOrdersFor): OrderStatistics = {
    val cached = OrderStatistics.cached(query, stats)
    (statCache ! RegisterCacheFor(cached))
    cached
  }

  def defaultMinQ(minq: Option[Long], typeid: Long) = minq getOrElse QueryDefaults.minQ(typeid)

  def getCachedStatistic(query: GetOrdersFor): Future[OrderStatistics] = {
    fetchCachedStats(query, query.bid.getOrElse(false)).flatMap {
      value =>
        value match {
          case None => fetchOrdersFor(query).map {
            orders =>
              storeCachedStats(OrderStatistics(orders, query.bid.getOrElse(false)), query)
          }
          case Some(v) => Future {
            v
          }
        }
    }
  }


  /**
   * Return a tuple of order statistics for buy/sell/all in the most efficient way possible.
   * @param typeid
   * @param setHours
   * @param regionLimit
   * @param usesystem
   * @param minq
   * @return
   */
  def getCachedStatistics(typeid: Long, setHours: Long, regionLimit: Seq[Long], usesystem: Option[Long], minq: Option[Long]):
  Future[(OrderStatistics, OrderStatistics, OrderStatistics)] = {
    val numminq = defaultMinQ(minq, typeid)
    val usesys = usesystem match {
      case None => Nil
      case Some(x) => List[Long](x)
    }

    val allq = GetOrdersFor(None, List(typeid), regionLimit, usesys, setHours, numminq)
    val buyq = GetOrdersFor(Some(true), List(typeid), regionLimit, usesys, setHours, numminq)
    val selq = GetOrdersFor(Some(false), List(typeid), regionLimit, usesys, setHours, numminq)

    val allCache = getCachedStatistic(allq)
    val buyCache = getCachedStatistic(buyq)
    val sellCache = getCachedStatistic(selq)
    Future.sequence(Seq(buyCache, allCache, sellCache)).map {
      s =>
        (s(0), s(1), s(2))
    }
  }

}
