package com.evecentral.util

import akka.actor.Actor
import com.evecentral.dataaccess._
import com.evecentral.{RegisterCacheFor, GetCacheFor, OrderStatistics, OrderCacheActor}
import com.evecentral.routes.RouteFinderActor
import com.evecentral.dataaccess.GetOrdersFor
import com.evecentral.RegisterCacheFor
import com.evecentral.GetCacheFor
import com.evecentral.dataaccess.OrderList


trait BaseOrderQuery {

	def ordersActor = {
		val r = Actor.registry.actorsFor[GetOrdersActor]
		r(0)
	}

	def statCache = {
		val r = (Actor.registry.actorsFor[OrderCacheActor]);
		r(0)
	}

	def pathActor = {
		val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0)
	}


	def fetchOrdersFor(buyq: GetOrdersFor) : Seq[MarketOrder] = {
		val buyf = (ordersActor ? buyq)
		(buyf.as[OrderList] getOrElse OrderList(null, List[MarketOrder]())).result
	}

	def fetchCachedStats(query: GetOrdersFor, highToLow: Boolean) : Option[OrderStatistics] = {
		val r = (statCache ? GetCacheFor(query, highToLow))
		r.as[Option[OrderStatistics]] getOrElse  None
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
	def getCachedStatistics(typeid: Long, setHours: Long, regionLimit: Seq[Long], usesystem: Option[Long], minq: Option[Long]): (OrderStatistics, OrderStatistics, OrderStatistics) = {
		val numminq = defaultMinQ(minq, typeid)
		val usesys = usesystem match {
			case None => Nil
			case Some(x) => List[Long](x)
		}

		val allq = GetOrdersFor(None, List(typeid), regionLimit, usesys, setHours, numminq)
		val buyq = GetOrdersFor(Some(true), List(typeid), regionLimit, usesys, setHours, numminq)
		val selq = GetOrdersFor(Some(false), List(typeid), regionLimit, usesys, setHours, numminq)

		val alls = fetchCachedStats(allq, false) getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(allq)), allq)
		val sels = fetchCachedStats(selq, false) getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(selq)), selq)
		val buys = fetchCachedStats(buyq, true) getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(buyq), true), buyq)
		(buys, alls, sels)
	}

}
