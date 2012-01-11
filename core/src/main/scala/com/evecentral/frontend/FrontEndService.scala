package com.evecentral.frontend

import cc.spray.http.MediaTypes._
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.ScalateSupport

trait FrontEndService extends Directives with ScalateSupport {

  val frontEndService = {
      path("") {
        get {
          render("com/evecentral/templates/index.ssp", Map())
        }
      }
  }

}
