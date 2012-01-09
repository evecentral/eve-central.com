package com.evecentral.api

import cc.spray.json._
import cc.spray.http.MediaTypes._
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.directives.IntNumber

import com.evecentral.dataaccess._
import com.evecentral.routes.{DistanceBetween, RouteFinderActor}
import cc.spray.typeconversion.SprayJsonSupport


trait APIv3Service extends Directives with DefaultJsonProtocol with SprayJsonSupport {

  def path = { val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0) }
  def getOrders = {
    val r = (Actor.registry.actorsFor[GetOrdersActor]); r(0)
  }

  val helloService = {
    pathPrefix("api3") {
      path("distance/from" / IntNumber / "to" / IntNumber) {
        (fromid, toid) =>
          get {
            respondWithMediaType(`application/json`) {
              ctx =>
              val from = StaticProvider.systemsMap(fromid)
              val to = StaticProvider.systemsMap(toid)
              ctx.complete((path ? DistanceBetween(from, to)).as[Int] match {
                  case Some(x) => Map("distance" -> x)
                  case _ => throw new Exception("No value returned")
                }
              )
            }
          }
      } ~
      path("orders/type" / IntNumber) {
        typeid =>
          get {
            respondWithMediaType(`text/plain`) {
              completeWith {
                //(getOrders ? GetOrdersFor(true, List[Long](typeid), Nil, Nil, 24)).as[Seq[MarketOrder]] match {
                 // case Some(x) => x(0).orderId.toString
                  //case None => "None"
                "Hello!"
                //}

              }
            }
          }
      }
    }
  }

}
