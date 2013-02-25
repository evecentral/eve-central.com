package com.evecentral.api

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import com.codahale.jerkson.Json.generate
import com.evecentral.FixedSprayMarshallers
import com.evecentral.dataaccess._
import com.evecentral.routes._
import com.evecentral.util.ActorNames
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.httpx.encoding.{Deflate, NoEncoding, Gzip}
import spray.routing.HttpService
import org.joda.time.DateTime


trait APIv3Service extends HttpService with FixedSprayMarshallers {
  this: Actor =>

  implicit val timeout: Timeout = 10.seconds

  /* Lookup some global actors */
  def pathActor = context.actorFor("/user/" + ActorNames.routefinder)
  def ordersActor = context.actorFor("/user/" + ActorNames.getorders)
  def histStatsActor = context.actorFor("/user/" + ActorNames.gethiststats)
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
                generate(Map(("short_name" -> station.shortName), ("long_name" -> station.name)))
              }

          }
        } ~ path("history/for/type" / IntNumber / "region" / "[^/]+".r / "bid" / IntNumber) {
          (typeid, region, bid) =>
            get {
              respondWithMediaType(`application/json`) { ctx =>
                val getF = (histStatsActor ? GetHistStats.Request(StaticProvider.typesMap(typeid), bid == 1,
                  region = lookupRegion(region)
                )).map {
                  case x : Seq[GetHistStats.CapturedOrderStatistics] => generate(x)
                  case _ => throw new Exception("no available stats")
                }
                getF.onComplete {
                  case Left(t) => ctx.failWith(t)
                  case Right(s) => ctx.complete(s)
                }
              }
            }
        } ~ path("history/for/type" / IntNumber / "system" / "[^/]+".r / "bid" / IntNumber) {
          (typeid, system, bid) =>
            get {
              respondWithMediaType(`application/json`) { ctx =>
                val system = lookupSystem(system)
                val region = system.region
                val getF = (histStatsActor ? GetHistStats.Request(StaticProvider.typesMap(typeid),
                  bid == 1,
                  region = region,
                  system = system)
                )).map {
                  case x : Seq[GetHistStats.CapturedOrderStatistics] => generate(x)
                  case _ => throw new Exception("no available stats")
                }
                getF.onComplete {
                  case Left(t) => ctx.failWith(t)
                  case Right(s) => ctx.complete(s)
                }
              }
            }
        }	~ path("distance/from" / "[^/]+".r / "to" / "[^/]+".r) {
          (fromr, tor) =>
            get {
              respondWithMediaType(`application/json`) { ctx =>
                val from  = lookupSystem(fromr)
                val to = lookupSystem(tor)


                val distanceF = (pathActor ? DistanceBetween(from, to)).map {
                  case x : Int => (generate(Map("distance" -> x)))
                  case _ => throw new Exception("No value returned")
                }
                distanceF.onComplete {
                  case Left(t) => ctx.failWith(t)
                  case Right(s) => ctx.complete(s)
                }

              }
            }
        } ~ path("route/from" / "[^/]+".r / "to" / "[^/]+".r) {
          (fromr, tor) =>
            get {
              respondWithMediaType(`application/json`) {
                ctx =>
                  val from  = lookupSystem(fromr)
                  val to = lookupSystem(tor)

                  val routeFuture = (pathActor ? RouteBetween(from, to)).mapTo[List[Jump]].map { v =>
                    generate(v)
                  }

                  routeFuture onComplete {
                    case Right(data) => ctx.complete(data)
                    case Left(t) => ctx.failWith(t)
                  }

              }
            }
        } ~ path("neighbors/of" / "[^/]+".r / "radius" / IntNumber) {
          (origin, radius) =>
            get {
              ctx =>
                val or = lookupSystem(origin)
                val json = (pathActor ? NeighborsOf(or, radius)).mapTo[Seq[SolarSystem]].map { sss =>
                  generate(sss)
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
                complete {
                  generate(StaticProvider.typesMap.filter(_._2.name.contains(rest)).filter(!_._2.name.contains("Blueprint")))
                }
              }
            }
        } ~ path("regions") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                generate(StaticProvider.regionsMap)
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


