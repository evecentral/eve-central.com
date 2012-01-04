package com.evecentral.api

import cc.spray.http.MediaTypes._
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.directives.IntNumber


trait APIService extends Directives {

  def getOrders = {
    val r = (Actor.registry.actorsFor[GetOrdersActor]); r(0)
  }

  val helloService = {
    pathPrefix("api3/orders") {
      path("type" / IntNumber) {
        typeid =>
          get {
            respondWithMediaType(`text/plain`) {
              completeWith {
                (getOrders ? GetOrdersFor(true, List[Long](typeid), Nil)).as[Seq[MarketOrder]] match {
                  case Some(x) => x(0).orderId.toString
                  case None => "None"
                }

              }
            }
          }
      }
    } ~
      path("shutdown") {
        (post | parameter('method ! "post")) {
          ctx =>
            Scheduler.scheduleOnce(() => Actor.registry.foreach(_ ! PoisonPill), 1000, TimeUnit.MILLISECONDS)
            ctx.complete("Will shutdown server in 1 second...")
        }
      }
  }

}
