package com.evecentral.api

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import com.evecentral.FixedSprayMarshallers
import com.evecentral.dataaccess._
import com.evecentral.routes._
import com.evecentral.util.ActorNames
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.httpx.encoding.{Deflate, NoEncoding, Gzip}
import spray.routing.{RequestContext, HttpService}
import spray.httpx.marshalling.Marshaller
import com.evecentral.datainput.StatisticsCapture
import org.joda.time.DateTime
import scala.util.{Success, Failure}


trait APIv3Service extends HttpService with FixedSprayMarshallers {
  this: Actor =>

  implicit val timeout: Timeout = 10.seconds

  def fcomplete[T](future: Future[T], ctx: RequestContext)(implicit marshaller: Marshaller[T]) {
    future.onComplete {
      case Failure(t) => ctx.failWith(t)
      case Success(s) => ctx.complete(s)
    }
  }

  /* Lookup some global actors */
  def pathActor = context.actorFor("/user/" + ActorNames.routefinder)

  def ordersActor = context.actorFor("/user/" + ActorNames.getorders)

  def histStatsActor = context.actorFor("/user/" + ActorNames.gethiststats)

  /* A local actor parsing helper actor */
  /* Note we can't register this as we are not the actor - one odd spray decision */
  val unifiedParser = context.actorFor("/user/" + ActorNames.unifiedparser)

  import LookupHelper._
  import JacksonMapper._

  val api3Service = {
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
      pathPrefix("api") {
        path("station/shorten" / IntNumber) {
          stationid =>
            get {
              ctx =>
                val station = StaticProvider.stationsMap(stationid)
                complete {
                  serialize(Map(("short_name" -> station.shortName), ("long_name" -> station.name)))
                }

            }
        } ~ pathPrefix("history/for/type" / IntNumber) {
          (typeid) =>
            path("region" / "[^/]+".r / "bid" / IntNumber) {
              (region, bid) =>
                get {
                  respondWithMediaType(`application/json`) {
                    ctx =>
                      val getF = (histStatsActor ? GetHistStats.Request(StaticProvider.typesMap(typeid), bid == 1,
                        region = lookupRegion(region)
                      )).map {
                        case x: Seq[GetHistStats.CapturedOrderStatistics] => serialize(Map("values" -> x))
                        case _ => throw new Exception("no available stats")
                      }
                      fcomplete(getF, ctx)
                  }
                }
            } ~ path("system" / "[^/]+".r / "bid" / IntNumber) {
              (systemid, bid) =>
                get {
                  respondWithMediaType(`application/json`) {
                    ctx =>
                      val system = lookupSystem(systemid)
                      val region = system.region
                      val getF = (histStatsActor ? GetHistStats.Request(StaticProvider.typesMap(typeid),
                        bid == 1,
                        region = region,
                        system = Some(system)
                      )).map {
                        case x: Seq[GetHistStats.CapturedOrderStatistics] => serialize(Map("values" -> x))
                        case _ => throw new Exception("no available stats")
                      }
                      fcomplete(getF, ctx)
                  }
                }

            } ~ path("empire" / "bid" / IntNumber) {
              (bid) =>
                get {
                  respondWithMediaType(`application/json`) {
                    ctx =>
                      val getF = (histStatsActor ? GetHistStats.Request(StaticProvider.typesMap(typeid),
                        bid == 1,
                        region = AllEmpireRegions
                      )).map {
                        case x: Seq[GetHistStats.CapturedOrderStatistics] => serialize(Map("values" -> x))
                        case _ => throw new Exception("no available stats")
                      }
                      fcomplete(getF, ctx)
                  }
                }
            }
        } ~ path("distance/from" / "[^/]+".r / "to" / "[^/]+".r) {
          (fromr, tor) =>
            get {
              respondWithMediaType(`application/json`) {
                ctx =>
                  val from = lookupSystem(fromr)
                  val to = lookupSystem(tor)

                  val distanceF = (pathActor ? DistanceBetween(from, to)).map {
                    case x: Int => (serialize(Map("distance" -> x)))
                    case _ => throw new Exception("No value returned")
                  }
                  fcomplete(distanceF, ctx)
              }
            }
        } ~ path("route/from" / "[^/]+".r / "to" / "[^/]+".r) {
          (fromr, tor) =>
            get {
              respondWithMediaType(`application/json`) {
                ctx =>
                  val from = lookupSystem(fromr)
                  val to = lookupSystem(tor)

                  val routeFuture = (pathActor ? RouteBetween(from, to)).mapTo[List[Jump]].map {
                    v =>
                      serialize(v)
                  }
                  fcomplete(routeFuture, ctx)
              }
            }
        } ~ path("neighbors/of" / "[^/]+".r / "radius" / IntNumber) {
          (origin, radius) =>
            get {
              ctx =>
                val or = lookupSystem(origin)
                val json = (pathActor ? NeighborsOf(or, radius)).mapTo[Seq[SolarSystem]].map {
                  sss =>
                    serialize(sss)
                }
                fcomplete(json, ctx)
            }
        } ~ path("types" / ".*".r) {
          rest =>
            get {
              respondWithMediaType(`application/json`) {
                complete {
                  val types = StaticProvider.typesMap.view.map { _._2 }.filter { ty =>
                   ty.name.contains(rest) &&
                     ty.group > 0
                  }.toSeq
                  serialize(types)
                }
              }
            }
        } ~ path("regions") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                serialize(StaticProvider.regionsMap)
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
        } ~ path("time") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                serialize(new DateTime)
              }
            }
          }
        } ~ path("upload" / Rest) {
          fluff => // Fluff is anything trailing in the URL, which we'll just ignore for sanity here
            (decodeRequest(NoEncoding) | decodeRequest(Gzip) | decodeRequest(Deflate)) {
              get {
                parameter("data") {
                  data =>
                    unifiedParser ! data
                    complete {
                      "1"
                    }
                }
              } ~ post {
                formFields("data") {
                  data =>
                    unifiedParser ! data
                    complete {
                      "1"
                    }
                } ~ entity(as[String]) {
                  data =>
                    unifiedParser ! data
                    complete {
                      "1"
                    }
                }
              } ~ put {
                ctx =>
                  val content = ctx.request.entity
                  val sb = new String(content.buffer, "UTF-8")
                  unifiedParser ! sb
                  ctx.complete(sb)
              }
            }
        } ~ path("syndicate") {
          complete {
            "1"
          }
        }
      }
    }
  }
}


