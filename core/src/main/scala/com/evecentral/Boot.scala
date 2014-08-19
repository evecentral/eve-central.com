package com.evecentral

import akka.actor.{Props, ActorSystem, Actor}
import akka.io.IO

import com.evecentral.dataaccess._
import com.evecentral.api._
import datainput.{UnifiedUploadParsingActor, StatisticsCaptureActor, UploadStorageActor}
import routes.RouteFinderActor
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.{RoutingSettings, ExceptionHandler}
import util.ActorNames
import com.typesafe.config.ConfigFactory
import akka.routing.SmallestMailboxRouter
import scala.concurrent.duration._
import akka.util.Timeout

object Boot extends App {

  val config = ConfigFactory.load()
  val system = ActorSystem("evec")

  val systemsMap = StaticProvider.systemsMap
  val stationsMAp = StaticProvider.stationsMap
  val typesMap = StaticProvider.typesMap
  LoggerFactory.getLogger(getClass)
  private[this] val log = LoggerFactory.getLogger("boot")
  // initialize SLF4J early


  // "Singleton" actors
  log.info("Booting GetOrdersActor")
  val ordersActor = system.actorOf(Props[GetOrdersActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.getorders)
  log.info("Booting GetHistStats")
  val gethiststats = system.actorOf(Props[GetHistStats], ActorNames.gethiststats)
  log.info("Booting StatisticsCaptureActor")
  val statCap = system.actorOf(Props[StatisticsCaptureActor], ActorNames.statCapture)
  log.info("Booting UploadStorageActor")
  val unifiedActor = system.actorOf(Props[UploadStorageActor].withRouter(new SmallestMailboxRouter(3)), ActorNames.uploadstorage)
  log.info("Booting RouteActor")
  val routeActor = system.actorOf(Props[RouteFinderActor], ActorNames.routefinder)
  log.info("Booting OrderCacheActor")
  val statCache = system.actorOf(Props[OrderCacheActor], ActorNames.statCache)
  log.info("Booting UnifiedUploadParsingActor")
  val parser = system.actorOf(Props[UnifiedUploadParsingActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.unifiedparser)

  class APIServiceActor extends Actor with APIv2Service with APIv3Service {
    def actorRefFactory = context
    implicit val route = RoutingSettings.default
    implicit val exception = ExceptionHandler.default
    def receive = runRoute(v2Service ~ api3Service)

    override implicit val timeout: Timeout = 60.seconds
  }
  log.info("Booting APIServiceActor")
  val apiModule = system.actorOf(Props[APIServiceActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.http_apiv3)

  IO(Http)(system) ! Http.Bind(apiModule, interface = "0.0.0.0", port = 8081)

}
