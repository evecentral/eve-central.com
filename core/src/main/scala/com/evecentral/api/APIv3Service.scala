package com.evecentral.api

import net.liftweb.json.Xml.{toJson, toXml}

import cc.spray.http.MediaTypes._
import akka.actor.{PoisonPill, Actor, Scheduler}
import Actor.actorOf
import cc.spray.Directives

import com.evecentral.dataaccess._

import net.liftweb.json._
import com.evecentral.FixedSprayMarshallers
import cc.spray.typeconversion.LiftJsonSupport
import cc.spray.directives.{Remaining, IntNumber}
import cc.spray.encoding.{Deflate, NoEncoding, Gzip}
import com.evecentral.datainput.UnifiedUploadParsingActor
import com.evecentral.routes._

trait APIv3Service extends Directives with FixedSprayMarshallers with LiftJsonSupport {

  val liftJsonFormats = DefaultFormats

	/* Lookup some global actors */
  def pathActor = { val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0) }
  def ordersActor = { val r = (Actor.registry.actorsFor[GetOrdersActor]); r(0) }
	/* A local actor parsing helper actor */
	/* Note we can't register this as we are not the actor - one odd spray decision */
	val unifiedParser = actorOf[UnifiedUploadParsingActor]

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
			path("neighbors/of" / "[^/]+".r / "radius" / IntNumber) {
				(origin, radius) =>

					import net.liftweb.json.JsonDSL._
					get {
						ctx =>
							val or = lookupSystem(origin)
							val json = (pathActor ? NeighborsOf(or, radius)).as[Seq[SolarSystem]] match {
								case Some(x) => x.map { ss =>
									("solarsystemid" -> ss.systemid) ~ ("name" -> ss.name) ~ ("security" -> ss.security)
								}
							}
							ctx.complete(json)
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
      path("upload" / Remaining) {
				fluff => // Fluff is anything trailing in the URL, which we'll just ignore for sanity here
					(decodeRequest(NoEncoding) | decodeRequest(Gzip) | decodeRequest(Deflate)) {
						get {
							parameter("data") { data =>
								unifiedParser ! data
								completeWith {"1"}
							}
						} ~
							post {
								formFields("data") { data =>
									unifiedParser ! data
									completeWith { "1" }
								}
							} ~ put {
							ctx =>
								val content = ctx.request.content.get
								val sb = new String(content.buffer, "UTF-8")
								unifiedParser ! sb
								ctx.complete(sb)
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
