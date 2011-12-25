package com.evecentral

import akka.dispatch.Future
import akka.actor.Actor
import akka.event.EventHandler

case class MarketOrder(orderId : Long, price : Double)

case class GetOrdersFor(types : Seq[Long], regions : Seq[Long])


class Markets extends Actor with ActorUtil {


  private def orderList(filter : GetOrdersFor) = {
    val query = Database.coredb
    query.select("SELECT orderid,price FROM current_market LIMIT 10") { row =>
      MarketOrder(row.getLong("orderid"), row.getLong("price"))
    }
  }
  
  def receive = {
    case x : GetOrdersFor =>  self.reply(orderList(x))
    case _ => self.reply(orderList(GetOrdersFor(Nil, Nil)))
  }
}
