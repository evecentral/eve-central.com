package com.evecentral

import akka.config.Supervision._
import akka.actor.{Supervisor, Actor}
import Actor._

import cc.spray.{SprayCanRootService, HttpService}

import com.evecentral.frontend.FrontEndService
import com.evecentral.dataaccess._
import com.evecentral.api._
import datainput.UploadStorageActor
import mail.MailDispatchActor
import routes.RouteFinderActor
import org.slf4j.LoggerFactory
import cc.spray.can.{MessageParserConfig, HttpServer}


object Boot extends App {


  LoggerFactory.getLogger(getClass)
  // initialize SLF4J early

  val akkaConfig = akka.config.Config.config
  val apiModule = new APIv3Service {}
  val apiv2Module = new APIv2Service {}
  val staticModule = new StaticService {}

  val frontEndService = new FrontEndService {}

  val config = cc.spray.can.ServerConfig(host = "0.0.0.0", port = akkaConfig.getInt("server-port", 8080),
    parserConfig = MessageParserConfig(maxUriLength = 16384))

  val httpApiService = actorOf(new HttpService(apiModule.api3Service))
  val httpApiv2Service = actorOf(new HttpService(apiv2Module.v2Service))
  val httpStaticService = actorOf(new HttpService(staticModule.staticService))
  val httpFeService = actorOf(new HttpService(frontEndService.frontEndService))
  val rootService = actorOf(new SprayCanRootService(httpApiService, httpApiv2Service, httpStaticService, httpFeService))
  val sprayCanServer = actorOf(new HttpServer(config))

  val systemsMap = StaticProvider.systemsMap
  val stationsMAp = StaticProvider.stationsMap
  val typesMap = StaticProvider.typesMap

	/* Build a supervisor for all of the "top-level" actor objects */
  val supervisor = Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Throwable], classOf[Exception]), 100, 100),
      List(
        Supervise(httpApiService, Permanent),
        Supervise(httpApiv2Service, Permanent),
        Supervise(httpStaticService, Permanent),
        Supervise(httpFeService, Permanent),
        Supervise(rootService, Permanent),
        Supervise(sprayCanServer, Permanent),
        Supervise(apiv2Module.quicklookActor, Permanent),
        Supervise(apiv2Module.marketstatActor, Permanent),
        Supervise(apiv2Module.olduploadActor, Permanent),
				Supervise(apiModule.unifiedParser, Permanent),
        Supervise(actorOf(new GetOrdersActor()), Permanent),
        Supervise(actorOf(new RouteFinderActor()), Permanent),
        Supervise(actorOf(new OrderCacheActor()), Permanent),
        Supervise(actorOf(new MailDispatchActor()), Permanent),
				Supervise(actorOf(new UploadStorageActor()), Permanent)

      )
    )
  )
  supervisor.start
}
