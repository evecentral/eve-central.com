package com.evecentral

import akka.config.Supervision._
import akka.actor.{Supervisor, Actor}
import Actor._
import cc.spray.can.HttpServer
import org.slf4j.LoggerFactory
import cc.spray.{SprayCanRootService, HttpService}

import com.evecentral.frontend.FrontEndService
import com.evecentral.api._

object Boot extends App {


  LoggerFactory.getLogger(getClass)
  // initialize SLF4J early

  val mainModule = new APIService {}

  val staticModule = new StaticService {}

  val frontEndService = new FrontEndService {}

  val httpService = actorOf(new HttpService(mainModule.helloService))
  val httpStaticService = actorOf(new HttpService(staticModule.staticService))
  val httpFeService = actorOf(new HttpService(frontEndService.frontEndService))
  val rootService = actorOf(new SprayCanRootService(httpService, httpStaticService, httpFeService))
  val sprayCanServer = actorOf(new HttpServer())

  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      List(
        Supervise(httpService, Permanent),
        Supervise(httpStaticService, Permanent),
        Supervise(httpFeService, Permanent),
        Supervise(rootService, Permanent),
        Supervise(sprayCanServer, Permanent),
        Supervise(actorOf(new GetOrdersActor()), Permanent)
      )
    )
  )
}
