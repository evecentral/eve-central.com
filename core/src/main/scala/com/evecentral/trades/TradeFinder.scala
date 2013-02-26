package com.evecentral.trades

import akka.actor.Actor
import com.evecentral.dataaccess._
import akka.dispatch.Future
import akka.pattern.ask
import com.evecentral.util.ActorNames
import akka.util.Timeout
import akka.util.duration._

import com.evecentral.dataaccess.Region
import com.evecentral.dataaccess.GetOrdersFor
import com.evecentral.dataaccess.SolarSystem
import com.evecentral.dataaccess.OrderList
import com.evecentral.routes.{Jump, RouteBetween}

object TradeFinder {

  case class RequestSellToBuy(origin: Either[SolarSystem, Region], destination: Either[SolarSystem, Region], taxRate: Double)

  case class MatchedOrder(from: MarketOrder, to: MarketOrder)

}

class TradeFinder extends Actor {

  import TradeFinder._
  import context._

  implicit val timeout: Timeout = 60.seconds

  private[this] val getOrders = actorFor(ActorNames.getorders)
  private[this] val getRoutes = actorFor(ActorNames.routefinder)

  def fetchOrder(bid: Boolean, location: Either[SolarSystem, Region]): Future[OrderList] = {
    val regions = location match {
      case Left(ss) => Nil
      case Right(r) => Seq(r.regionid)
    }
    val systems = location match {
      case Left(ss) => Seq(ss.systemid)
      case Right(r) => Nil
    }
    val request = GetOrdersFor(Some(bid), Seq(), regions = regions, systems = systems)
    (getOrders ? request).mapTo
  }

  def getRawProfitable(o: Map[Long, Seq[MarketOrder]],
                       d: Map[Long, Seq[MarketOrder]], taxRate: Double) = {

    // Match the type IDs to only available ones
    val originOrdersF = o.filterKeys(k => d.contains(k))
    val destOrdersF = d.filterKeys(k => originOrdersF.contains(k))

    originOrdersF.map {
      case (k, v) => (k ->(v, destOrdersF(k)))
    }.mapValues {
      v =>
        v._1.sortBy(_.price).zip(v._2.sortBy(-_.price)).filter {
          case (o, d) => o.price < (d.price - d.price * taxRate)
        }.unzip
    }.filter {
      case (key, (v1, v2)) => v1.size > 0 && v2.size > 0
    }
  }

  def enumerateRoutes(orders: Map[Long, (Seq[MarketOrder], Seq[MarketOrder])]): Map[Long, Future[Seq[List[Jump]]]] = {
    val systems = orders.mapValues {
      case (a, b) => (a.map {
        order => order.system
      }.distinct, b.map {
        order => order.system
      }.distinct)
    }

    val pairs = systems.map {
      case (key, value) => (key -> (for {a <- value._1; b <- value._2} yield (a, b)).distinct)
    }
    pairs.map {
      case (key, routes) =>
        key -> Future.sequence(routes.map {
          case (s, d) => (getRoutes ? RouteBetween(s, d)).mapTo[List[Jump]]
        })
          .map {
          routes => routes.distinct.sortBy {
            _.size
          }
        }
    }
  }

  def receive = {
    case RequestSellToBuy(origin, destination, taxRate) => {
      val rp = for {
        oFuture <- fetchOrder(false, origin).map {
          r => r.result.groupBy(_.typeid)
        }
        dFuture <- fetchOrder(true, destination).map {
          r => r.result.groupBy(_.typeid)
        }
      } yield getRawProfitable(oFuture, dFuture, taxRate)

    }
  }

}
