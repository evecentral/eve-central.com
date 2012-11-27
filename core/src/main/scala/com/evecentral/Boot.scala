package com.evecentral

import akka.actor.{Props, ActorSystem, Actor}

import com.evecentral.dataaccess._
import com.evecentral.api._
import datainput.{StatisticsCaptureActor, UploadStorageActor}
import routes.RouteFinderActor
import org.slf4j.LoggerFactory
import spray.io.{SingletonHandler, IOBridge}
import util.ActorNames
import spray.can.server.HttpServer


object Boot extends App {

	val system = ActorSystem("evec")
	val ioBridge = system.actorOf(Props[IOBridge], "iobridge")

  val systemsMap = StaticProvider.systemsMap
	val stationsMAp = StaticProvider.stationsMap
	val typesMap = StaticProvider.typesMap
  LoggerFactory.getLogger(getClass)
  // initialize SLF4J early



	class APIServiceActor extends Actor with APIv2Service with APIv3Service {

		def actorRefFactory = context

		def receive = runRoute(v2Service ~ api3Service)
	}

  val apiModule = system.actorOf(Props[APIServiceActor], ActorNames.http_apiv3)


	val server = system.actorOf(
	Props(new HttpServer(ioBridge, SingletonHandler(apiModule)))
	)

	server ! HttpServer.Bind("localhost", 8081)

}
