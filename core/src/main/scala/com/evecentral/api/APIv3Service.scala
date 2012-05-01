package com.evecentral.api

import cc.spray.json._
import cc.spray.http.MediaTypes._
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.directives.IntNumber

import com.evecentral.dataaccess._
import com.evecentral.routes.{DistanceBetween, RouteFinderActor}
import cc.spray.typeconversion.SprayJsonSupport


trait APIv3Service extends Directives with DefaultJsonProtocol with SprayJsonSupport {

  def pathActor = { val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0) }
  def ordersActor = { val r = (Actor.registry.actorsFor[GetOrdersActor]); r(0) }

  val api3Service = {
    pathPrefix("api") {
      path("distance/from" / IntNumber / "to" / IntNumber) {
        (fromid, toid) =>
          get {
            respondWithMediaType(`application/json`) {
              ctx =>
              val from = StaticProvider.systemsMap(fromid)
              val to = StaticProvider.systemsMap(toid)
              ctx.complete((pathActor ? DistanceBetween(from, to)).as[Int] match {
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
      } ~ /* Upload support for unified formats */
      path("upload") {
        get {
          parameter("data") { data =>
            completeWith {
              "1"
            }
          }
        }~
          post {
            formFields("data") { data =>
              completeWith {
                "1"
              }
            }
          }
      } ~
      path ("syndicate") {
        completeWith {
          "1"
        }
      }
    }
  }

}
