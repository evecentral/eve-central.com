package com.evecentral

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import akka.util.Timeout
import com.evecentral.api._
import com.evecentral.dataaccess._
import com.evecentral.datainput.{StatisticsCaptureActor, UnifiedUploadParsingActor, UploadStorageActor}
import com.evecentral.routes.RouteFinderActor
import com.evecentral.util.ActorNames
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http.StatusCodes._
import spray.routing._
import spray.routing.Directives.{complete, requestUri}
import spray.util.LoggingContext

import scala.concurrent.duration._

object Boot extends App {

  val config = ConfigFactory.load()
  val system = ActorSystem("evec")

  val systemsMap = StaticProvider.systemsMap
  val stationsMAp = StaticProvider.stationsMap
  val typesMap = StaticProvider.typesMap
  LoggerFactory.getLogger(getClass)
  val log = LoggerFactory.getLogger("boot")
  // initialize SLF4J early


  // "Singleton" actors
  log.info("Booting GetOrdersActor")
  val ordersActor = system.actorOf(Props[GetOrdersActor].withRouter(new RoundRobinRouter(80)), ActorNames.getorders)
  log.info("Booting GetHistStats")
  val gethiststats = system.actorOf(Props[GetHistStats], ActorNames.gethiststats)
  log.info("Booting StatisticsCaptureActor")
  val statCap = system.actorOf(Props[StatisticsCaptureActor], ActorNames.statCapture)
  log.info("Booting UploadStorageActor")
  val unifiedActor = system.actorOf(Props[UploadStorageActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.uploadstorage)
  log.info("Booting RouteActor")
  val routeActor = system.actorOf(Props[RouteFinderActor], ActorNames.routefinder)
  log.info("Booting OrderCacheActor")
  val statCache = system.actorOf(Props[OrderCacheActor], ActorNames.statCache)
  log.info("Booting UnifiedUploadParsingActor")
  val parser = system.actorOf(Props[UnifiedUploadParsingActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.unifiedparser)

  def catchAllHandler() =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          log.error("Exception at URI " + uri, e)
          complete(InternalServerError, "An internal error occurred. Here is a debugging stacktrace. \n" +
            "Please report issues to https://github.com/theatrus/eve-central.com\n\n" + e.getStackTraceString)
        }
    }


  class APIServiceActor extends Actor with APIv2Service with APIv3Service {
    def actorRefFactory = context
    implicit val route = RoutingSettings.default
    implicit val exception = ExceptionHandler.default
    def receive = runRoute(handleExceptions(catchAllHandler()) { v2Service ~ api3Service })

    override implicit val timeout: Timeout = 60.seconds
  }
  log.info("Booting APIServiceActor")
  val apiModule = system.actorOf(Props[APIServiceActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.http_apiv3)

  IO(Http)(system) ! Http.Bind(apiModule, interface = "0.0.0.0", port = 8081)

}
