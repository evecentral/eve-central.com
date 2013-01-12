package com.evecentral

import akka.actor.{Props, ActorSystem, Actor}

import com.evecentral.dataaccess._
import com.evecentral.api._
import datainput.{UnifiedUploadParsingActor, StatisticsCaptureActor, UploadStorageActor}
import routes.RouteFinderActor
import org.slf4j.LoggerFactory
import spray.io.{IOExtension, SingletonHandler}
import util.ActorNames
import spray.can.server.HttpServer
import com.typesafe.config.ConfigFactory
import akka.routing.SmallestMailboxRouter
import akka.util.duration._
import akka.util.Timeout


object Boot extends App {

	val config = ConfigFactory.load()
	val system = ActorSystem("evec")
	val ioBridge = IOExtension(system).ioBridge()

  val systemsMap = StaticProvider.systemsMap
	val stationsMAp = StaticProvider.stationsMap
	val typesMap = StaticProvider.typesMap
  LoggerFactory.getLogger(getClass)
  // initialize SLF4J early

	// "Singleton" actors
	val ordersActor = system.actorOf(Props[GetOrdersActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.getorders)
	val unifiedActor = system.actorOf(Props[UploadStorageActor].withRouter(new SmallestMailboxRouter(3)), ActorNames.uploadstorage)
	val routeActor = system.actorOf(Props[RouteFinderActor], ActorNames.routefinder)
	val statCache = system.actorOf(Props[OrderCacheActor], ActorNames.statCache)
	val parser = system.actorOf(Props[UnifiedUploadParsingActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.unifiedparser)
	val statCap = system.actorOf(Props[StatisticsCaptureActor], ActorNames.statCapture)
	val gethiststats = system.actorOf(Props[GetHistStats].withRouter(new SmallestMailboxRouter(10)), ActorNames.gethiststats)


	class APIServiceActor extends Actor with APIv2Service with APIv3Service {
		def actorRefFactory = context
		def receive = runRoute(v2Service ~ api3Service)
		override implicit val timeout : Timeout = 60.seconds
	}

  val apiModule = system.actorOf(Props[APIServiceActor].withRouter(new SmallestMailboxRouter(10)), ActorNames.http_apiv3)


	val server = system.actorOf(
	Props(new HttpServer(ioBridge, SingletonHandler(apiModule))),
	"http_server"
	)

	server ! HttpServer.Bind("0.0.0.0", 8081)

}
