	package com.evecentral.api

	import akka.actor.Actor
	import akka.pattern.ask

	import net.liftweb.json._

	import spray.http.MediaTypes._
	import spray.routing.{HttpService, RequestContext, Directives}
	import spray.http.HttpHeaders.RawHeader
	import spray.httpx.encoding.{Deflate, NoEncoding, Gzip}
	import spray.httpx.LiftJsonSupport
	import com.evecentral.dataaccess._
	import com.evecentral.FixedSprayMarshallers
	import com.evecentral.routes._
	import akka.util.Timeout
	import akka.util.duration._
	import com.evecentral.util.ActorNames
	import spray.http.HttpResponse


	trait APIv3Service extends HttpService with FixedSprayMarshallers with LiftJsonSupport {
		this: Actor =>
		val liftJsonFormats = DefaultFormats

		implicit val timeout: Timeout = 10.seconds

		/* Lookup some global actors */
		def pathActor = context.actorFor("/user/" + ActorNames.routefinder)
		def ordersActor = context.actorFor("/user/" + ActorNames.getorders)
		/* A local actor parsing helper actor */
		/* Note we can't register this as we are not the actor - one odd spray decision */
		val unifiedParser = context.actorFor("/user/" + ActorNames.unifiedparser)

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
									compact(render(Map(("short_name" -> station.shortName), ("long_name" -> station.name))))
								}

						}
					} ~ path("distance/from" / "[^/]+".r / "to" / "[^/]+".r) {
							(fromr, tor) =>
								get {
									respondWithMediaType(`application/json`) { ctx =>
										val from  = lookupSystem(fromr)
										val to = lookupSystem(tor)
										import net.liftweb.json.JsonDSL._
										complete {
											(pathActor ? DistanceBetween(from, to)).map {
												case Some(x : Int) => (render(("distance" -> x)))
												case _ => throw new Exception("No value returned")
											}
										}
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

										val routeFuture = (pathActor ? RouteBetween(from, to)).mapTo[List[Jump]].map { v =>
											compact(render(v.map {jump =>
												Map[String, String]("fromid" -> jump.from.systemid.toString, "toid" -> jump.to.systemid.toString,
													"from" -> jump.from.name, "to" -> jump.to.name, "secchange" -> jump.secChange.toString) }
											))}

										routeFuture onComplete {
											case Right(data) => ctx.complete(data)
											case Left(t) => ctx.failWith(t)
										}

								}
							}
					} ~ path("neighbors/of" / "[^/]+".r / "radius" / IntNumber) {
						(origin, radius) =>

							import net.liftweb.json.JsonDSL._
							get {
								ctx =>
									val or = lookupSystem(origin)
									val json = (pathActor ? NeighborsOf(or, radius)).mapTo[Seq[SolarSystem]].map { sss =>
										sss.map { ss => Map("solarsystemid" -> ss.systemid.toString, "name" -> ss.name, "security" -> ss.security.toString) }
									}
									json.onComplete {
										case Right(data) => ctx.complete(json)
										case Left(t) => ctx.failWith(t)
									}
							}
					} ~ path("types" / ".*".r) {
						rest =>
							get {
								respondWithMediaType(`application/json`) {
									import  net.liftweb.json.JsonDSL._
									complete {
										compact(render(StaticProvider.typesMap.filter(_._2.name.contains(rest)).filter(!_._2.name.contains("Blueprint")).map(
											types => Map("typename" -> types._2.name, "typeid" -> types._2.typeid.toString))))
									}
								}
							}
					} ~ path("regions") {
						get {
							respondWithMediaType(`application/json`) {
								complete {
									import net.liftweb.json.JsonDSL._
									compact(render(StaticProvider.regionsMap.map(region => Map("regionname" -> region._2.name, "regionid" -> region._2.regionid.toString))))
								}
							}
						}
					} ~ path("orders/type" / IntNumber) {
						typeid =>
							get {
								respondWithMediaType(`text/plain`) {
									complete {
										"Hello!"
									}
								}
							}
					} ~ path("upload" / Rest) {
						fluff => // Fluff is anything trailing in the URL, which we'll just ignore for sanity here
							(decodeRequest(NoEncoding) | decodeRequest(Gzip) | decodeRequest(Deflate)) {
								get {
									parameter("data") { data =>
										unifiedParser ! data
										complete {"1"}
									}
								} ~ post {
									formFields("data") { data =>
										unifiedParser ! data
										complete { "1" }
									} ~ entity(as[String]) {
										data =>
											unifiedParser ! data
											complete { "1" }
									}
								} ~ put {
									ctx =>
										val content = ctx.request.entity
										val sb = new String(content.buffer, "UTF-8")
										unifiedParser ! sb
										ctx.complete(sb)
								}
							}
				} ~ path ("syndicate") {
						complete {
							"1"
						}
					}
				}
			}
		}
	}


