package com.evecentral.trades

import akka.actor.Actor
import com.evecentral.dataaccess._
import akka.dispatch.Future
import akka.pattern.ask
import com.evecentral.util.ActorNames
import scala.Left
import com.evecentral.dataaccess.Region
import com.evecentral.dataaccess.GetOrdersFor
import com.evecentral.dataaccess.SolarSystem
import scala.Right
import scala.Some
import com.evecentral.dataaccess.OrderList

object TradeFinder {
	case class RequestSellToBuy(origin: Either[SolarSystem, Region], destination: Either[SolarSystem, Region], taxRate: Double)
	case class MatchedOrder(from: MarketOrder, to: MarketOrder)
}

class TradeFinder extends Actor {

	import TradeFinder._
	import context._

	private[this] val getOrders = actorFor(ActorNames.getorders)

	private[this] def fetchOrder(bid: Boolean, location: Either[SolarSystem, Region]): Future[OrderList] = {
		val regions = location match { case Left(ss) => Nil case Right(r) => Seq(r.regionid) }
		val systems = location match { case Left(ss) => Seq(ss.systemid) case Right(r) => Nil }
		val request = GetOrdersFor(Some(bid), Seq(), regions = regions, systems = systems)
		(getOrders ? request).mapTo
	}

	private[this] def getRawProfitable(o: Future[Map[Long, Seq[MarketOrder]]],
	                                   d: Future[Map[Long, Seq[MarketOrder]]], taxRate: Double) = {
		Future.sequence(Seq(o, d)).map { l =>
			val originOrders = l(0)
			val destOrders = l(1)
			// Match the type IDs to only available ones
			val originOrdersF = originOrders.filterKeys(k => destOrders.contains(k))
			val destOrdersF = destOrders.filterKeys(k => originOrdersF.contains(k))

			originOrdersF.map { case (k,v) => (k -> (v, destOrdersF(k))) } mapValues { v =>
				v._1.sortBy(_.price).zip(v._2.sortBy(_.price)).filter { case (o,d) => o.price < (d.price - d.price * taxRate) }.unzip
			}
		}
	}

	def receive = {
		case RequestSellToBuy(origin, destination, taxRate) => {
			val oFuture = fetchOrder(false, origin).map { r => r.result.groupBy(_.typeid) }
			val dFuture = fetchOrder(true, destination).map {r => r.result.groupBy(_.typeid) }
			sender ! getRawProfitable(oFuture, dFuture, taxRate)

		}
	}

}
