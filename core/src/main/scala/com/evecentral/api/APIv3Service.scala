package com.evecentral.api

import cc.spray.http.MediaTypes._
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.directives.IntNumber

import com.evecentral.dataaccess._
import cc.spray.encoding.{NoEncoding, Gzip}

import net.liftweb.json._
import com.evecentral.routes.{Jump, RouteBetween, DistanceBetween, RouteFinderActor}
import com.evecentral.FixedSprayMarshallers
import cc.spray.typeconversion.LiftJsonSupport
import scala.Some

trait APIv3Service extends Directives with FixedSprayMarshallers with LiftJsonSupport {

  val liftJsonFormats = DefaultFormats

  def pathActor = { val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0) }
  def ordersActor = { val r = (Actor.registry.actorsFor[GetOrdersActor]); r(0) }

  import LookupHelper._

  val api3Service = {
    pathPrefix("api") {
      path("station/shorten" / IntNumber) {
        stationid =>
          import net.liftweb.json.JsonDSL._
          get {
            respondWithContentType(`application/json`) {
            ctx =>
              val station = StaticProvider.stationsMap(stationid)
              ctx.complete(compact(render(("short_name" -> station.shortName) ~ ("long_name" -> station.name))))
            }
          }
      } ~
      path("distance/from" / "[^/]+".r / "to" / "[^/]+".r) {
        (fromr, tor) =>
          get {
            respondWithMediaType(`application/json`) {
              ctx =>

                val from  = lookupSystem(fromr)
                val to = lookupSystem(tor)

                import net.liftweb.json.JsonDSL._

                ctx.complete((pathActor ? DistanceBetween(from, to)).as[Int] match {
                  case Some(x) => compact(render(("distance" -> x)))
                  case _ => throw new Exception("No value returned")
                }
                )
            }
          }
      } ~
      path("route/from" / "[^/]+".r / "to" / "[^/]+".r) {
        (fromr, tor) =>
          get {
            respondWithMediaType(`application/json`) {
              ctx =>
                import net.liftweb.json.JsonDSL._

                val from  = lookupSystem(fromr)
                val to = lookupSystem(tor)

                ctx.complete((pathActor ? RouteBetween(from, to)).as[Seq[Jump]] match {
                  case Some(x) => compact(render(x.map {jump =>
                    ("fromid" -> jump.from.systemid) ~ ("toid" -> jump.to.systemid) ~ (
                      "from" -> jump.from.name) ~ ("to" -> jump.to.name) ~ ("secchange" -> jump.secChange) }
                  ))
                })
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
            (decodeRequest(NoEncoding) | decodeRequest(Gzip)) {
              formFields("data") { data =>
                completeWith {
                  "1"
                }
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
