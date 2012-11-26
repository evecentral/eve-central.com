package com.evecentral.api

import akka.actor.Actor
import akka.pattern.ask

import net.liftweb.json.Xml.{toJson, toXml}
import net.liftweb.json._

import spray.http.MediaTypes._
import spray.routing.{RequestContext, Directives}
import spray.http.HttpHeaders.RawHeader
import spray.httpx.encoding.{Deflate, NoEncoding, Gzip}
import spray.httpx.LiftJsonSupport
import com.evecentral.dataaccess._
import com.evecentral.FixedSprayMarshallers
import com.evecentral.routes._

trait APIv3Service extends FixedSprayMarshallers with LiftJsonSupport {
	this: Actor =>
	import Directives._
	val liftJsonFormats = DefaultFormats

	/* Lookup some global actors */
	def pathActor = context.actorFor("RouteFinder")
	def ordersActor = context.actorFor("GetOrders")
	/* A local actor parsing helper actor */
	/* Note we can't register this as we are not the actor - one odd spray decision */
	val unifiedParser = context.actorFor("UnifiedParser")

	import LookupHelper._

	val api3Service = {
		respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
			pathPrefix("api") {
				path("station/shorten" / IntNumber) { stationid =>
					get {
						ctx =>
							val station = StaticProvider.stationsMap(stationid)
							complete {
								import net.liftweb.json.JsonDSL._
								compact(render(("short_name" -> station.shortName) ~ ("long_name" -> station.name)))
							}
							//ctx.complete("")//

					}
				} ~ path("distance/from" / "[^/]+".r / "to" / "[^/]+".r) {
						(fromr, tor) =>
							get {
								respondWithMediaType(`application/json`) {
									ctx =>

										val from  = lookupSystem(fromr)
										val to = lookupSystem(tor)

										import net.liftweb.json.JsonDSL._
										ctx.complete((pathActor ? DistanceBetween(from, to)).apply() match {
											case Some(x) => compact(render(("distance" -> x)))
											case _ => throw new Exception("No value returned")
										}
										)
								}
							}
					} ~ path("route/from" / "[^/]+".r / "to" / "[^/]+".r) {
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
											case None =>
												compact(render(("error" -> "No such path")))
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
										case None =>
											("error" -> "Lonely")

									}
									ctx.complete(json)
							}
					} ~
					path("types" / ".*".r) {
						rest =>
							get {
								respondWithMediaType(`application/json`) {
									import  net.liftweb.json.JsonDSL._
									completeWith {
										compact(render(StaticProvider.typesMap.filter(_._2.name.contains(rest)).filter(!_._2.name.contains("Blueprint")).map(types => ("typename" -> types._2.name) ~ ("typeid" -> types._2.typeid))))
									}
								}
							}
					} ~
					path("regions") {
						get {
							respondWithMediaType(`application/json`) {
								import net.liftweb.json.JsonDSL._
								completeWith {
									compact(render(StaticProvider.regionsMap.map(region => ("regionname" -> region._2.name) ~ ("regionid" -> region._2.regionid))))
								}
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
					path("upload" / Remaining) {
						fluff => // Fluff is anything trailing in the URL, which we'll just ignore for sanity here
							(decodeRequest(NoEncoding) | decodeRequest(Gzip) | decodeRequest(Deflate)) {
								get {
									parameter("data") { data =>
										unifiedParser ! data
										completeWith {"1"}
									}
								} ~ post {
									formFields("data") { data =>
										unifiedParser ! data
										completeWith { "1" }
									} ~ content(as[String]) {
										data =>
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

}
