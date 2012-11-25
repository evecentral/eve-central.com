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


trait BaseOrderQuery {
	this: Actor =>

	def ordersActor = context.actorFor("GetOrders")

	def statCache = context.actorFor("StatisticsCache")

	def pathActor = context.actorFor("RouteFinder")

	def fetchOrdersFor(buyq: GetOrdersFor) : Future[Seq[MarketOrder]] = {
		(ordersActor ? buyq) mapTo manifest[Seq[MarketOrder]]
	}

	def fetchCachedStats(query: GetOrdersFor, highToLow: Boolean) : Future[Option[OrderStatistics]] = {
		(statCache ? GetCacheFor(query, highToLow)) mapTo manifest[Option[OrderStatistics]]
	}

	def storeCachedStats(stats: OrderStatistics, query: GetOrdersFor) : OrderStatistics = {
		val cached = OrderStatistics.cached(query, stats)
		(statCache! RegisterCacheFor(cached))
		cached
	}

	def defaultMinQ(minq: Option[Long], typeid: Long) = minq getOrElse QueryDefaults.minQ(typeid)


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
	(Future[OrderStatistics], Future[OrderStatistics], Future[OrderStatistics]) = {
		val numminq = defaultMinQ(minq, typeid)
		val usesys = usesystem match {
			case None => Nil
			case Some(x) => List[Long](x)
		}

		val allq = GetOrdersFor(None, List(typeid), regionLimit, usesys, setHours, numminq)
		val buyq = GetOrdersFor(Some(true), List(typeid), regionLimit, usesys, setHours, numminq)
		val selq = GetOrdersFor(Some(false), List(typeid), regionLimit, usesys, setHours, numminq)

		val allCache = fetchCachedStats(allq, false)
		val buyCache = fetchCachedStats(buyq, true)
		val sellCache = fetchCachedStats(selq, false)

		Future.sequence(Seq(, , )).map {
			case (allr, buyr, selr) =>
				val alls = allr getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(allq)), allq)
				val sels = buyr getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(selq)), selq)
				val buys = selr getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(buyq), true), buyq)
				(buys, alls, sels)
		}

	}

}
