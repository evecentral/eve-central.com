package com.evecentral

import akka.config.Supervision._
import akka.actor.{Supervisor, Actor}
import Actor._
import cc.spray.can.HttpServer

import cc.spray.{SprayCanRootService, HttpService}

import com.evecentral.frontend.FrontEndService
import com.evecentral.dataaccess._
import com.evecentral.api._


object Boot extends App {


  //LoggerFactory.getLogger(getClass)
  // initialize SLF4J early

  val apiModule = new APIv3Service {}
  val apiv2Module = new APIv2Service {
    Supervisor(
      SupervisorConfig(OneForOneStrategy(List(classOf[Exception]), 3, 100),
        List(
          Supervise(quicklookActor, Permanent)
        )
      ))
  }
  val staticModule = new StaticService {}

  val frontEndService = new FrontEndService {}

  val config = cc.spray.can.ServerConfig(host = "0.0.0.0")

  val httpApiService = actorOf(new HttpService(apiModule.helloService))
  val httpApiv2Service = actorOf(new HttpService(apiv2Module.v2Service))
  val httpStaticService = actorOf(new HttpService(staticModule.staticService))
  val httpFeService = actorOf(new HttpService(frontEndService.frontEndService))
  val rootService = actorOf(new SprayCanRootService(httpApiService, httpApiv2Service, httpStaticService, httpFeService))
  val sprayCanServer = actorOf(new HttpServer(config))

  val systemsMap = StaticProvider.systemsMap
  val stationsMAp = StaticProvider.stationsMap
  val typesMap = StaticProvider.typesMap

  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      List(
        Supervise(httpApiService, Permanent),
        Supervise(httpApiv2Service, Permanent),
        Supervise(httpStaticService, Permanent),
        Supervise(httpFeService, Permanent),
        Supervise(rootService, Permanent),
        Supervise(sprayCanServer, Permanent),
        Supervise(actorOf(new GetOrdersActor()), Permanent)
      )
    )
  )
}
